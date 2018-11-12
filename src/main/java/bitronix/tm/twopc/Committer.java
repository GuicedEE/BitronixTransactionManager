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
package bitronix.tm.twopc;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.*;
import bitronix.tm.twopc.executor.Executor;
import bitronix.tm.twopc.executor.Job;
import bitronix.tm.utils.Decoder;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import java.util.*;
import java.util.logging.Level;

/**
 * Phase 2 Commit logic engine.
 *
 * @author Ludovic Orban
 */
public final class Committer
		extends AbstractPhaseEngine
{

	private final static java.util.logging.Logger log = java.util.logging.Logger.getLogger(Committer.class.toString());
	private final List<XAResourceHolderState> interestedResources = Collections.synchronizedList(new ArrayList<>());
	// this list has to be thread-safe as the CommitJobs can be executed in parallel (when async 2PC is configured)
	private final List<XAResourceHolderState> committedResources = Collections.synchronizedList(new ArrayList<>());
	private volatile boolean onePhase;


	/**
	 * Constructor Committer creates a new Committer instance.
	 *
	 * @param executor
	 * 		of type Executor
	 */
	public Committer(Executor executor)
	{
		super(executor);
	}

	/**
	 * Execute phase 2 commit.
	 *
	 * @param transaction
	 * 		the transaction wanting to commit phase 2
	 * @param interestedResources
	 * 		a map of phase 1 prepared resources wanting to participate in phase 2 using Xids as keys
	 *
	 * @throws HeuristicRollbackException
	 * 		when all resources committed instead.
	 * @throws HeuristicMixedException
	 * 		when some resources committed and some rolled back.
	 * @throws bitronix.tm.internal.BitronixSystemException
	 * 		when an internal error occured.
	 * @throws bitronix.tm.internal.BitronixRollbackException
	 * 		during 1PC when resource fails to commit
	 */
	public void commit(BitronixTransaction transaction, List<XAResourceHolderState> interestedResources)
			throws HeuristicMixedException, HeuristicRollbackException, SystemException, BitronixRollbackException
	{
		XAResourceManager resourceManager = transaction.getResourceManager();
		if (resourceManager.size() == 0)
		{
			transaction.setStatus(Status.STATUS_COMMITTING); //TODO: there is a disk force here that could be avoided
			transaction.setStatus(Status.STATUS_COMMITTED);
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("phase 2 commit succeeded with no interested resource");
			}
			return;
		}

		transaction.setStatus(Status.STATUS_COMMITTING);

		this.interestedResources.clear();
		this.interestedResources.addAll(interestedResources);
		this.onePhase = resourceManager.size() == 1;

		try
		{
			executePhase(resourceManager, true);
		}
		catch (PhaseException ex)
		{
			logFailedResources(ex);
			if (onePhase)
			{
				transaction.setStatus(Status.STATUS_ROLLEDBACK);
				throw new BitronixRollbackException("transaction failed during 1PC commit of " + transaction, ex);
			}
			else
			{
				transaction.setStatus(Status.STATUS_UNKNOWN);
				throwException("transaction failed during commit of " + transaction, ex, interestedResources.size());
			}
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("phase 2 commit executed on resources " + Decoder.collectResourcesNames(committedResources));
		}

		// Some resources might have failed the 2nd phase of 2PC.
		// Only resources which successfully committed should be registered in the journal, the other
		// ones should be picked up by the recoverer.
		// Not interested resources have to be included as well since they returned XA_RDONLY and they
		// don't participate in phase 2: the TX succeded for them.
		Set<String> committedAndNotInterestedUniqueNames = new HashSet<>();
		committedAndNotInterestedUniqueNames.addAll(collectResourcesUniqueNames(committedResources));
		List<XAResourceHolderState> notInterestedResources = collectNotInterestedResources(resourceManager.getAllResources(), interestedResources);
		committedAndNotInterestedUniqueNames.addAll(collectResourcesUniqueNames(notInterestedResources));

		if (LogDebugCheck.isDebugEnabled())
		{
			List<XAResourceHolderState> committedAndNotInterestedResources = new ArrayList<>();
			committedAndNotInterestedResources.addAll(committedResources);
			committedAndNotInterestedResources.addAll(notInterestedResources);

			log.finer("phase 2 commit succeeded on resources " + Decoder.collectResourcesNames(committedAndNotInterestedResources));
		}

		transaction.setStatus(Status.STATUS_COMMITTED, committedAndNotInterestedUniqueNames);
	}

	/**
	 * Method throwException ...
	 *
	 * @param message
	 * 		of type String
	 * @param phaseException
	 * 		of type PhaseException
	 * @param totalResourceCount
	 * 		of type int
	 *
	 * @throws HeuristicMixedException
	 * 		when
	 * @throws HeuristicRollbackException
	 * 		when
	 */
	private void throwException(String message, PhaseException phaseException, int totalResourceCount) throws HeuristicMixedException, HeuristicRollbackException
	{
		List<Exception> exceptions = phaseException.getExceptions();
		List<XAResourceHolderState> resources = phaseException.getResourceStates();

		boolean hazard = false;
		List<XAResourceHolderState> heuristicResources = new ArrayList<>();
		List<XAResourceHolderState> errorResources = new ArrayList<>();

		for (int i = 0; i < exceptions.size(); i++)
		{
			Exception ex = exceptions.get(i);
			XAResourceHolderState resourceHolder = resources.get(i);
			if (ex instanceof XAException)
			{
				XAException xaEx = (XAException) ex;
				switch (xaEx.errorCode)
				{
					case XAException.XA_HEURHAZ:
						hazard = true;
					case XAException.XA_HEURCOM:
					case XAException.XA_HEURRB:
					case XAException.XA_HEURMIX:
						heuristicResources.add(resourceHolder);
						break;
					default:
					{
						errorResources.add(resourceHolder);
						break;
					}
				}
			}
			else
			{
				errorResources.add(resourceHolder);
			}
		}

		if (!hazard && heuristicResources.size() == totalResourceCount)
		{
			throw new BitronixHeuristicRollbackException(message + ":" +
			                                             " all resource(s) " + Decoder.collectResourcesNames(heuristicResources) +
			                                             " improperly unilaterally rolled back", phaseException);
		}
		else
		{
			throw new BitronixHeuristicMixedException(message + ":" +
			                                          (!errorResources.isEmpty()
			                                           ? " resource(s) " + Decoder.collectResourcesNames(errorResources) + " threw unexpected exception"
			                                           : "") +
			                                          (!errorResources.isEmpty() && !heuristicResources.isEmpty() ? " and" : "") +
			                                          (!heuristicResources.isEmpty() ? " resource(s) " + Decoder.collectResourcesNames(heuristicResources) +
			                                                                           " improperly unilaterally rolled back" + (hazard ? " (or hazard happened)" : "") : ""),
			                                          phaseException);
		}
	}

	/**
	 * Determine if a resource is participating in the phase or not. A participating resource gets
	 * a job created to execute the phase's command on it.
	 *
	 * @param xaResourceHolderState
	 * 		the resource to check for its participation.
	 *
	 * @return true if the resource must participate in the phase.
	 */
	@Override
	protected boolean isParticipating(XAResourceHolderState xaResourceHolderState)
	{
		for (XAResourceHolderState resourceHolderState : interestedResources)
		{
			if (xaResourceHolderState == resourceHolderState)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Create a {@link bitronix.tm.twopc.executor.Job} that is going to execute the phase command on the given resource.
	 *
	 * @param xaResourceHolderState
	 * 		the resource that is going to receive a command.
	 *
	 * @return the {@link bitronix.tm.twopc.executor.Job} that is going to execute the command.
	 */
	@Override
	protected Job createJob(XAResourceHolderState resourceHolder)
	{
		return new CommitJob(resourceHolder);
	}

	private final class CommitJob
			extends Job
	{

		/**
		 * Constructor CommitJob creates a new CommitJob instance.
		 *
		 * @param resourceHolder
		 * 		of type XAResourceHolderState
		 */
		public CommitJob(XAResourceHolderState resourceHolder)
		{
			super(resourceHolder);
		}

		/**
		 * Method getXAException returns the XAException of this CommitJob object.
		 *
		 * @return the XAException (type XAException) of this CommitJob object.
		 */
		@Override
		public XAException getXAException()
		{
			return xaException;
		}

		/**
		 * Method getRuntimeException returns the runtimeException of this CommitJob object.
		 *
		 * @return the runtimeException (type RuntimeException) of this CommitJob object.
		 */
		@Override
		public RuntimeException getRuntimeException()
		{
			return runtimeException;
		}

		/**
		 * Method execute ...
		 */
		@Override
		public void execute()
		{
			try
			{
				commitResource(getResource(), onePhase);
			}
			catch (RuntimeException ex)
			{
				runtimeException = ex;
			}
			catch (XAException ex)
			{
				xaException = ex;
			}
		}

		/**
		 * Method commitResource ...
		 *
		 * @param resourceHolder
		 * 		of type XAResourceHolderState
		 * @param onePhase
		 * 		of type boolean
		 *
		 * @throws XAException
		 * 		when
		 */
		private void commitResource(XAResourceHolderState resourceHolder, boolean onePhase) throws XAException
		{
			try
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("committing resource " + resourceHolder + (onePhase ? " (with one-phase optimization)" : ""));
				}
				resourceHolder.getXAResource()
				              .commit(resourceHolder.getXid(), onePhase);
				committedResources.add(resourceHolder);
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("committed resource " + resourceHolder);
				}
			}
			catch (XAException ex)
			{
				handleXAException(resourceHolder, ex, onePhase);
			}
		}

		/**
		 * Method handleXAException ...
		 *
		 * @param failedResourceHolder
		 * 		of type XAResourceHolderState
		 * @param xaException
		 * 		of type XAException
		 * @param onePhase
		 * 		of type boolean
		 *
		 * @throws XAException
		 * 		when
		 */
		private void handleXAException(XAResourceHolderState failedResourceHolder, XAException xaException, boolean onePhase) throws XAException
		{
			switch (xaException.errorCode)
			{
				case XAException.XA_HEURCOM:
					forgetHeuristicCommit(failedResourceHolder);
					return;

				case XAException.XAER_NOTA:
					throw new BitronixXAException("unknown heuristic termination, global state of this transaction is unknown - guilty: " + failedResourceHolder,
					                              XAException.XA_HEURHAZ, xaException);

				case XAException.XA_HEURHAZ:
				case XAException.XA_HEURMIX:
				case XAException.XA_HEURRB:
				case XAException.XA_RBCOMMFAIL:
				case XAException.XA_RBDEADLOCK:
				case XAException.XA_RBINTEGRITY:
				case XAException.XA_RBOTHER:
				case XAException.XA_RBPROTO:
				case XAException.XA_RBROLLBACK:
				case XAException.XA_RBTIMEOUT:
				case XAException.XA_RBTRANSIENT:
					log.severe("heuristic rollback is incompatible with the global state of this transaction - guilty: " + failedResourceHolder);
					throw xaException;

				default:
					if (onePhase)
					{
						if (LogDebugCheck.isDebugEnabled())
						{
							log.finer("XAException thrown in commit phase of 1PC optimization, rethrowing it");
						}
						throw xaException;
					}
					String extraErrorDetails = TransactionManagerServices.getExceptionAnalyzer()
					                                                     .extractExtraXAExceptionDetails(xaException);
					log.log(Level.WARNING, "resource '" + failedResourceHolder.getUniqueName() + "' reported " + Decoder.decodeXAExceptionErrorCode(xaException) +
					                       (extraErrorDetails == null ? "" : ", extra error=" + extraErrorDetails) + " when asked to commit transaction branch." +
					                       " Transaction is prepared and will commit via recovery service when resource availability allows.", xaException);
			}
		}

		/**
		 * Method forgetHeuristicCommit ...
		 *
		 * @param resourceHolder
		 * 		of type XAResourceHolderState
		 */
		private void forgetHeuristicCommit(XAResourceHolderState resourceHolder)
		{
			try
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("handling heuristic commit on resource " + resourceHolder.getXAResource());
				}
				resourceHolder.getXAResource()
				              .forget(resourceHolder.getXid());
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("forgotten heuristically committed resource " + resourceHolder.getXAResource());
				}
			}
			catch (XAException ex)
			{
				String extraErrorDetails = TransactionManagerServices.getExceptionAnalyzer()
				                                                     .extractExtraXAExceptionDetails(ex);
				log.log(Level.SEVERE, "cannot forget " + resourceHolder.getXid() + " assigned to " + resourceHolder.getXAResource() +
				                      ", error=" + Decoder.decodeXAExceptionErrorCode(ex) + (extraErrorDetails == null ? "" : ", extra error=" + extraErrorDetails), ex);
			}
		}

		/**
		 * Method toString ...
		 *
		 * @return String
		 */
		@Override
		public String toString()
		{
			return "a CommitJob " + (onePhase ? "(one phase) " : "") + "with " + getResource();
		}
	}

}
