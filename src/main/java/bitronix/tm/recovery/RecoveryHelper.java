/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitronix.tm.recovery;

import bitronix.tm.BitronixXid;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.LogDebugCheck;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.utils.Decoder;
import bitronix.tm.utils.Uid;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * Reovery helper methods.
 *
 * @author Ludovic Orban
 */
public class RecoveryHelper
{
	private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(RecoveryHelper.class);

	private static final String XIDS_ON = " xid(s) on ";
	private static final String UNABLE_TO_COMMIT_INDOUBT = "unable to commit in-doubt branch on resource ";
	private static final String EXTRA_ERROR = ", extra error=";
	private static final String ERROR = " - error=";
	private static final String UNABLE_TO_ROLLBACK_INDOUBT = "unable to rollback aborted in-doubt branch on resource ";

	private RecoveryHelper()
	{
		//Nothing needed
	}

	/**
	 * Run the recovery process on the target resource.
	 *
	 * @param xaResourceHolderState
	 * 		the {@link XAResourceHolderState} to recover.
	 *
	 * @return a Set of BitronixXids.
	 *
	 * @throws javax.transaction.xa.XAException
	 * 		if {@link XAResource#recover(int)} calls fail.
	 */
	public static Set<BitronixXid> recover(XAResourceHolderState xaResourceHolderState) throws XAException
	{
		Set<BitronixXid> xids = new HashSet<>();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.trace("recovering with STARTRSCAN");
		}
		int xidCount;
		try
		{
			xidCount = recover(xaResourceHolderState, xids, XAResource.TMSTARTRSCAN);
		}
		catch (XAException ex)
		{
			if (xaResourceHolderState.getIgnoreRecoveryFailures())
			{
				if (LogDebugCheck.isDebugEnabled())
				{
                    log.trace("ignoring recovery failure on resource {}", xaResourceHolderState, ex);
				}
				return Collections.emptySet();
			}
			throw ex;
		}
		if (LogDebugCheck.isDebugEnabled())
		{
            log.trace("STARTRSCAN recovered {}" + XIDS_ON + "{}", xidCount, xaResourceHolderState);
		}

		try
		{
			while (xidCount > 0)
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.trace("recovering with NOFLAGS");
				}
				xidCount = recover(xaResourceHolderState, xids, XAResource.TMNOFLAGS);
				if (LogDebugCheck.isDebugEnabled())
				{
                    log.trace("NOFLAGS recovered {}" + XIDS_ON + "{}", xidCount, xaResourceHolderState);
				}
			}
		}
		catch (XAException ex)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.trace( "NOFLAGS recovery call failed", ex);
			}
		}

		try
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.trace("recovering with ENDRSCAN");
			}
			xidCount = recover(xaResourceHolderState, xids, XAResource.TMENDRSCAN);
			if (LogDebugCheck.isDebugEnabled())
			{
                log.trace("ENDRSCAN recovered {}" + XIDS_ON + "{}", xidCount, xaResourceHolderState);
			}
		}
		catch (XAException ex)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.trace( "ENDRSCAN recovery call failed", ex);
			}
		}

		return xids;
	}

	/**
	 * Call {@link XAResource#recover(int)} on the resource and fill the <code>alreadyRecoveredXids</code> Set
	 * with recovered {@link BitronixXid}s.
	 * Step 1.
	 *
	 * @param resourceHolderState
	 * 		the {@link XAResourceHolderState} to recover.
	 * @param alreadyRecoveredXids
	 * 		a set of {@link Xid}s already recovered from this resource in this recovery session.
	 * @param flags
	 * 		any combination of {@link XAResource#TMSTARTRSCAN}, {@link XAResource#TMNOFLAGS} or {@link XAResource#TMENDRSCAN}.
	 *
	 * @return the amount of recovered {@link Xid}.
	 *
	 * @throws javax.transaction.xa.XAException
	 * 		if {@link XAResource#recover(int)} call fails.
	 */
	private static int recover(XAResourceHolderState resourceHolderState, Set<BitronixXid> alreadyRecoveredXids, int flags) throws XAException
	{
		Xid[] xids = resourceHolderState.getXAResource()
		                                .recover(flags);
		if (xids == null)
		{
			return 0;
		}

		boolean currentNodeOnly = TransactionManagerServices.getConfiguration()
		                                                    .isCurrentNodeOnlyRecovery();

		Set<BitronixXid> freshlyRecoveredXids = new HashSet<>();
		for (Xid xid : xids)
		{
			if (xid.getFormatId() != BitronixXid.FORMAT_ID)
			{
				if (LogDebugCheck.isDebugEnabled())
				{
                    log.trace("skipping non-bitronix XID {}(format ID: {} GTRID: {}BQUAL: {})", xid, xid.getFormatId(), new Uid(xid.getGlobalTransactionId()), new Uid(xid.getBranchQualifier()));
				}
				continue;
			}

			BitronixXid bitronixXid = new BitronixXid(xid);

			if (currentNodeOnly)
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.trace("recovering XIDs generated by this node only - recovered XIDs' GTRID must contain this JVM uniqueId");
				}
				byte[] extractedServerId = bitronixXid.getGlobalTransactionIdUid()
				                                      .extractServerId();
				byte[] jvmUniqueId = TransactionManagerServices.getConfiguration()
				                                               .buildServerIdArray();

				if (extractedServerId.length == 0)
				{
                    log.error("skipping XID {} as its GTRID's serverId is null. It looks like the disk journal is corrupted!", bitronixXid);
					continue;
				}

				if (!Arrays.equals(jvmUniqueId, extractedServerId))
				{
					String extractedServerIdString = new String(extractedServerId);
					String jvmUniqueIdString = new String(jvmUniqueId);

					if (LogDebugCheck.isDebugEnabled())
					{
                        log.trace("skipping XID {} as its GTRID's serverId <{}> does not match this JVM unique ID <{}>", bitronixXid, extractedServerIdString, jvmUniqueIdString);
					}
					continue;
				}
			}
			else
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.trace("recovering all XIDs regardless of this JVM uniqueId");
				}
			}

			if (alreadyRecoveredXids.contains(bitronixXid))
			{
				if (LogDebugCheck.isDebugEnabled())
				{
                    log.trace("already recovered XID {}, skipping it", bitronixXid);
				}
				continue;
			}

			if (freshlyRecoveredXids.contains(bitronixXid))
			{
                log.warn("resource {} recovered two identical XIDs within the same recover call: {}", resourceHolderState.getUniqueName(), bitronixXid);
				continue;
			}

			if (LogDebugCheck.isDebugEnabled())
			{
                log.trace("recovered {}", bitronixXid);
			}
			freshlyRecoveredXids.add(bitronixXid);
		} // for i < xids.length

		alreadyRecoveredXids.addAll(freshlyRecoveredXids);
		return freshlyRecoveredXids.size();
	}


	/**
	 * Commit the specified branch of a dangling transaction.
	 *
	 * @param xaResourceHolderState
	 * 		the {@link XAResourceHolderState} to commit the branch on.
	 * @param xid
	 * 		the {@link Xid} to commit.
	 *
	 * @return true when commit was successful.
	 */
	public static boolean commit(XAResourceHolderState xaResourceHolderState, Xid xid)
	{
		String uniqueName = xaResourceHolderState.getUniqueName();
		boolean success = true;
		boolean forget = false;

		try
		{
			xaResourceHolderState.getXAResource()
			                     .commit(xid, false);
		}
		catch (XAException ex)
		{
			String extraErrorDetails = TransactionManagerServices.getExceptionAnalyzer()
			                                                     .extractExtraXAExceptionDetails(ex);
			if (ex.errorCode == XAException.XAER_NOTA)
			{
                log.error(UNABLE_TO_COMMIT_INDOUBT + "{} - error=XAER_NOTA{}. Forgotten heuristic?", uniqueName, extraErrorDetails == null ? "" : EXTRA_ERROR + extraErrorDetails, ex);
			}
			else if (ex.errorCode == XAException.XA_HEURCOM)
			{
                log.info(UNABLE_TO_COMMIT_INDOUBT + "{}" + ERROR + "{}{}. Heuristic decision compatible with the global state of this transaction.", uniqueName, Decoder.decodeXAExceptionErrorCode(ex), extraErrorDetails == null ? "" : EXTRA_ERROR + extraErrorDetails);
				forget = true;
			}
			else if (ex.errorCode == XAException.XA_HEURHAZ || ex.errorCode == XAException.XA_HEURMIX || ex.errorCode == XAException.XA_HEURRB)
			{
                log.error(UNABLE_TO_COMMIT_INDOUBT + "{}" + ERROR + "{}{}. Heuristic decision incompatible with the global state of this transaction!", uniqueName, Decoder.decodeXAExceptionErrorCode(ex), extraErrorDetails == null ? "" : EXTRA_ERROR + extraErrorDetails);
				forget = true;
				success = false;
			}
			else
			{
                log.error(UNABLE_TO_COMMIT_INDOUBT + "{}" + ERROR + "{}{}.", uniqueName, Decoder.decodeXAExceptionErrorCode(ex), extraErrorDetails == null ? "" : EXTRA_ERROR + extraErrorDetails, ex);
				success = false;
			}
		}
		if (forget)
		{
			processForget(xaResourceHolderState, xid, uniqueName);
		}
		return success;
	}

	/**
	 * Goes through the forget phase
	 *
	 * @param xaResourceHolderState
	 * 		resource
	 * @param xid
	 * 		yes
	 * @param uniqueName
	 * 		Single unique name
	 */
	private static void processForget(XAResourceHolderState xaResourceHolderState, Xid xid, String uniqueName)
	{
		try
		{
			if (LogDebugCheck.isDebugEnabled())
			{
                log.trace("forgetting XID {} on resource {}", xid, uniqueName);
			}
			xaResourceHolderState.getXAResource()
			                     .forget(xid);
		}
		catch (XAException ex)
		{
			String extraErrorDetails = TransactionManagerServices.getExceptionAnalyzer()
			                                                     .extractExtraXAExceptionDetails(ex);
            log.error("unable to forget XID {} on resource {}, error={}{}", xid, uniqueName, Decoder.decodeXAExceptionErrorCode(ex), extraErrorDetails == null ? "" : EXTRA_ERROR + extraErrorDetails, ex);
		}
	}

	/**
	 * Rollback the specified branch of a dangling transaction.
	 *
	 * @param xaResourceHolderState
	 * 		the {@link XAResourceHolderState} to rollback the branch on.
	 * @param xid
	 * 		the {@link Xid} to rollback.
	 *
	 * @return true when rollback was successful.
	 */
	public static boolean rollback(XAResourceHolderState xaResourceHolderState, Xid xid)
	{
		String uniqueName = xaResourceHolderState.getUniqueName();
		boolean success = true;
		boolean forget = false;
		try
		{
			xaResourceHolderState.getXAResource()
			                     .rollback(xid);
		}
		catch (XAException ex)
		{
			String extraErrorDetails = TransactionManagerServices.getExceptionAnalyzer()
			                                                     .extractExtraXAExceptionDetails(ex);
			if (ex.errorCode == XAException.XAER_NOTA)
			{
                log.error(UNABLE_TO_ROLLBACK_INDOUBT + "{} - error=XAER_NOTA{}. Forgotten heuristic?", uniqueName, extraErrorDetails == null ? "" : EXTRA_ERROR + extraErrorDetails, ex);
			}
			else if (ex.errorCode == XAException.XA_HEURRB)
			{
                log.info(UNABLE_TO_ROLLBACK_INDOUBT + "{}" + ERROR + "{}{}. Heuristic decision compatible with the global state of this transaction.", uniqueName, Decoder.decodeXAExceptionErrorCode(ex), extraErrorDetails == null ? "" : EXTRA_ERROR + extraErrorDetails);
				forget = true;
			}
			else if (ex.errorCode == XAException.XA_HEURHAZ || ex.errorCode == XAException.XA_HEURMIX || ex.errorCode == XAException.XA_HEURCOM)
			{
                log.error(UNABLE_TO_ROLLBACK_INDOUBT + "{}" + ERROR + "{}{}. Heuristic decision incompatible with the global state of this transaction!", uniqueName, Decoder.decodeXAExceptionErrorCode(ex), extraErrorDetails == null ? "" : EXTRA_ERROR + extraErrorDetails);
				forget = true;
				success = false;
			}
			else
			{
                log.error(UNABLE_TO_ROLLBACK_INDOUBT + "{}" + ERROR + "{}{}.", uniqueName, Decoder.decodeXAExceptionErrorCode(ex), extraErrorDetails == null ? "" : EXTRA_ERROR + extraErrorDetails, ex);
				success = false;
			}
		}
		if (forget)
		{
			processForget(xaResourceHolderState, xid, uniqueName);
		}
		return success;
	}

}
