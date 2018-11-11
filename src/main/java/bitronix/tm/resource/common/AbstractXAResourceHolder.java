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
package bitronix.tm.resource.common;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.BitronixXid;
import bitronix.tm.internal.LogDebugCheck;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.utils.Uid;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of all services required by a {@link XAResourceHolder}. This class keeps a list of all
 * {@link XAResourceHolderState}s of the {@link XAResourceHolder} plus the currently active one. There is
 * one per transaction in which this {@link XAResourceHolder} is enlisted plus all the suspended transactions in which
 * it is enlisted as well.
 *
 * @author Ludovic Orban
 */
public abstract class AbstractXAResourceHolder<T extends XAResourceHolder<T>>
		extends AbstractXAStatefulHolder<T>
		implements XAResourceHolder<T>
{

	private final static java.util.logging.Logger log = java.util.logging.Logger.getLogger(AbstractXAResourceHolder.class.toString());

	private final Map<Uid, Map<Uid, XAResourceHolderState>> xaResourceHolderStates = new HashMap<>();
	private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

	/**
	 * Method getXAResourceHolderStatesForGtrid ...
	 *
	 * @param gtrid
	 * 		of type Uid
	 *
	 * @return Map<Uid
                       *       	   	   ,
                       *       	   	   XAResourceHolderState>
	 */
	// This method is only used by tests.  It is (and always was) potentially thread-unsafe depending on what callers do with the returned map.
	protected Map<Uid, XAResourceHolderState> getXAResourceHolderStatesForGtrid(Uid gtrid)
	{
		rwLock.readLock()
		      .lock();
		try
		{
			return xaResourceHolderStates.get(gtrid);
		}
		finally
		{
			rwLock.readLock()
			      .unlock();
		}
	}

	/**
	 * This method implements a standard Visitor Pattern.  For the specified GTRID, the
	 * provided {@link bitronix.tm.resource.common.XAResourceHolderStateVisitor}'s visit() method is called for each matching
	 * {@link bitronix.tm.internal.XAResourceHolderState} in the order they were added.  This method was introduced
	 * as a replacement for the old getXAResourceHolderStatesForGtrid(Uid) method.  The old
	 * getXAResourceHolderStatesForGtrid method exported an internal collection which was unsynchronized
	 * yet was iterated over by the callers.  Using the Visitor Pattern allows us to perform the same
	 * iteration within the context of a lock, and avoids exposing internal state and implementation
	 * details to callers.
	 *
	 * @param gtrid
	 * 		the GTRID of the transaction state to visit {@link bitronix.tm.internal.XAResourceHolderState}s for
	 * @param visitor
	 * 		a {@link bitronix.tm.resource.common.XAResourceHolderStateVisitor} instance
	 */
	@Override
	public void acceptVisitorForXAResourceHolderStates(Uid gtrid, XAResourceHolderStateVisitor visitor)
	{
		rwLock.readLock()
		      .lock();
		try
		{
			Map<Uid, XAResourceHolderState> statesForGtrid = xaResourceHolderStates.get(gtrid);
			if (statesForGtrid != null)
			{
				for (XAResourceHolderState xaResourceHolderState : statesForGtrid.values())
				{
					if (!visitor.visit(xaResourceHolderState))
					{
						break;
					}
				}
			}
		}
		finally
		{
			rwLock.readLock()
			      .unlock();
		}
	}

	/**
	 * Checks whether there are {@link bitronix.tm.internal.XAResourceHolderState}s for the specified GTRID.
	 *
	 * @param gtrid
	 * 		the GTRID of the transaction state to check existence for
	 *
	 * @return true if there are {@link bitronix.tm.internal.XAResourceHolderState}s, false otherwise
	 */
	@Override
	public boolean isExistXAResourceHolderStatesForGtrid(Uid gtrid)
	{
		rwLock.readLock()
		      .lock();
		try
		{
			return xaResourceHolderStates.containsKey(gtrid);
		}
		finally
		{
			rwLock.readLock()
			      .unlock();
		}
	}

	/**
	 * Get a count of {@link bitronix.tm.internal.XAResourceHolderState}s for the specified GTRID.
	 *
	 * @param gtrid
	 * 		the GTRID to get a {@link bitronix.tm.internal.XAResourceHolderState} count for
	 *
	 * @return the count of {@link bitronix.tm.internal.XAResourceHolderState}s, or 0 if there are no states for the
	 * 		specified GTRID
	 */
	@Override
	public int getXAResourceHolderStateCountForGtrid(Uid gtrid)
	{
		rwLock.readLock()
		      .lock();
		try
		{
			Map<Uid, XAResourceHolderState> statesForGtrid = xaResourceHolderStates.get(gtrid);
			if (statesForGtrid != null)
			{
				return statesForGtrid.size();
			}
			return 0;
		}
		finally
		{
			rwLock.readLock()
			      .unlock();
		}
	}

	/**
	 * Add a {@link bitronix.tm.internal.XAResourceHolderState} of this wrapped resource.
	 *
	 * @param xid
	 * 		the Xid of the transaction state to add.
	 * @param xaResourceHolderState
	 * 		the {@link bitronix.tm.internal.XAResourceHolderState} to set.
	 */
	@Override
	public void putXAResourceHolderState(BitronixXid xid, XAResourceHolderState xaResourceHolderState)
	{
		Uid gtrid = xid.getGlobalTransactionIdUid();
		Uid bqual = xid.getBranchQualifierUid();

		rwLock.writeLock()
		      .lock();
		try
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("putting XAResourceHolderState [" + xaResourceHolderState + "] on " + this);
			}
			if (!xaResourceHolderStates.containsKey(gtrid))
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("GTRID [" + gtrid + "] previously unknown to " + this + ", adding it to the resource's transactions list");
				}

				// use a LinkedHashMap as iteration order must be guaranteed
				Map<Uid, XAResourceHolderState> statesForGtrid = new LinkedHashMap<>(4);
				statesForGtrid.put(bqual, xaResourceHolderState);
				xaResourceHolderStates.put(gtrid, statesForGtrid);
			}
			else
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("GTRID [" + gtrid + "] previously known to " + this + ", adding it to the resource's transactions list");
				}

				Map<Uid, XAResourceHolderState> statesForGtrid = xaResourceHolderStates.get(gtrid);
				statesForGtrid.put(bqual, xaResourceHolderState);
			}
		}
		finally
		{
			rwLock.writeLock()
			      .unlock();
		}
	}

	/**
	 * Remove all states related to a specific Xid from this wrapped resource.
	 *
	 * @param xid
	 * 		the Xid of the transaction state to remove.
	 */
	@Override
	public void removeXAResourceHolderState(BitronixXid xid)
	{
		Uid gtrid = xid.getGlobalTransactionIdUid();
		Uid bqual = xid.getBranchQualifierUid();

		rwLock.writeLock()
		      .lock();
		try
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("removing XAResourceHolderState of xid " + xid + " from " + this);
			}

			Map<Uid, XAResourceHolderState> statesForGtrid = xaResourceHolderStates.get(gtrid);
			if (statesForGtrid == null)
			{
				log.warning("tried to remove unknown GTRID [" + gtrid + "] from " + this + " - Bug?");
				return;
			}

			XAResourceHolderState removed = statesForGtrid.remove(bqual);
			if (removed == null)
			{
				log.warning("tried to remove unknown BQUAL [" + bqual + "] from " + this + " - Bug?");
				return;
			}

			if (statesForGtrid.isEmpty())
			{
				xaResourceHolderStates.remove(gtrid);
			}
		}
		finally
		{
			rwLock.writeLock()
			      .unlock();
		}
	}

	/**
	 * Check if this {@link bitronix.tm.resource.common.XAResourceHolder} contains a state for a specific {@link bitronix.tm.resource.common.XAResourceHolder}.
	 * In other words: has the {@link bitronix.tm.resource.common.XAResourceHolder}'s {@link javax.transaction.xa.XAResource} been enlisted in some transaction ?
	 *
	 * @param xaResourceHolder
	 * 		the {@link bitronix.tm.resource.common.XAResourceHolder} to look for.
	 *
	 * @return true if the {@link bitronix.tm.resource.common.XAResourceHolder} is enlisted in some transaction, false otherwise.
	 */
	@Override
	public boolean hasStateForXAResource(XAResourceHolder<? extends XAResourceHolder> xaResourceHolder)
	{
		rwLock.readLock()
		      .lock();
		try
		{
			for (Map<Uid, XAResourceHolderState> statesForGtrid : xaResourceHolderStates.values())
			{
				for (XAResourceHolderState otherXaResourceHolderState : statesForGtrid.values())
				{
					if (otherXaResourceHolderState.getXAResource() == xaResourceHolder.getXAResource())
					{
						if (LogDebugCheck.isDebugEnabled())
						{
							log.finer("resource " + xaResourceHolder + " is enlisted in another transaction with " + otherXaResourceHolderState.getXid()
							                                                                                                                   .toString());
						}
						return true;
					}
				}
			}

			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("resource not enlisted in any transaction: " + xaResourceHolder);
			}
			return false;
		}
		finally
		{
			rwLock.readLock()
			      .unlock();
		}
	}

	/**
	 * If this method returns false, then local transaction calls like Connection.commit() can be made.
	 *
	 * @return true if start() has been successfully called but not end() yet <i>and</i> the transaction is not suspended.
	 */
	public boolean isParticipatingInActiveGlobalTransaction()
	{
		rwLock.readLock()
		      .lock();
		try
		{
			BitronixTransaction currentTransaction = TransactionContextHelper.currentTransaction();
			Uid gtrid = currentTransaction == null
			            ? null
			            : currentTransaction.getResourceManager()
			                                .getGtrid();
			if (gtrid == null)
			{
				return false;
			}

			Map<Uid, XAResourceHolderState> statesForGtrid = xaResourceHolderStates.get(gtrid);
			if (statesForGtrid == null)
			{
				return false;
			}

			for (XAResourceHolderState xaResourceHolderState : statesForGtrid.values())
			{
				if (xaResourceHolderState != null &&
				    xaResourceHolderState.isStarted() &&
				    !xaResourceHolderState.isSuspended() &&
				    !xaResourceHolderState.isEnded())
				{
					return true;
				}
			}
			return false;
		}
		finally
		{
			rwLock.readLock()
			      .unlock();
		}
	}

	/**
	 * Simple helper method which returns a set of GTRIDs of transactions in which
	 * this resource is enlisted. Useful for monitoring.
	 *
	 * @return a set of String-encoded GTRIDs of transactions in which this resource is enlisted.
	 */
	public Set<String> getXAResourceHolderStateGtrids()
	{
		rwLock.readLock()
		      .lock();
		try
		{
			HashSet<String> gtridsAsStrings = new HashSet<>();

			for (Uid uid : xaResourceHolderStates.keySet())
			{
				gtridsAsStrings.add(uid.toString());
			}

			return gtridsAsStrings;
		}
		finally
		{
			rwLock.readLock()
			      .unlock();
		}
	}
}
