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

import javax.transaction.HeuristicCommitException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import java.util.*;
import java.util.logging.Level;

/**
 * Phase 1 &amp; 2 Rollback logic engine.
 *
 * @author Ludovic Orban
 */
public final class Rollbacker
		extends AbstractPhaseEngine
{

	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(Rollbacker.class.toString());

	private final List<XAResourceHolderState> interestedResources = Collections.synchronizedList(new ArrayList<>());
	// this list has to be thread-safe as the RollbackJobs can be executed in parallel (when async 2PC is configured)
	private final List<XAResourceHolderState> rolledbackResources = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Constructor Rollbacker creates a new Rollbacker instance.
	 *
	 * @param executor
	 * 		of type Executor
	 */
	public Rollbacker(Executor executor)
	{
		super(executor);
	}

	/**
	 * Rollback the current XA transaction. Transaction will not timeout while changing status but rather by some
	 * extra logic that will manually throw the exception after doing as much cleanup as possible.
	 *
	 * @param transaction
	 * 		the transaction to rollback.
	 * @param interestedResources
	 * 		resources that should be rolled back.
	 *
	 * @throws HeuristicCommitException
	 * 		when all resources committed instead.
	 * @throws HeuristicMixedException
	 * 		when some resources committed and some rolled back.
	 * @throws bitronix.tm.internal.BitronixSystemException
	 * 		when an internal error occured.
	 */
	public void rollback(BitronixTransaction transaction, List<XAResourceHolderState> interestedResources) throws HeuristicMixedException, HeuristicCommitException, SystemException
	{
		XAResourceManager resourceManager = transaction.getResourceManager();
		transaction.setStatus(Status.STATUS_ROLLING_BACK);
		this.interestedResources.clear();
		this.interestedResources.addAll(interestedResources);

		try
		{
			executePhase(resourceManager, true);
		}
		catch (PhaseException ex)
		{
			logFailedResources(ex);
			transaction.setStatus(Status.STATUS_UNKNOWN);
			throwException("transaction failed during rollback of " + transaction, ex, interestedResources.size());
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("rollback executed on resources " + Decoder.collectResourcesNames(rolledbackResources));
		}

		// Some resources might have failed the 2nd phase of 2PC.
		// Only resources which successfully rolled back should be registered in the journal, the other
		// ones should be picked up by the recoverer.
		// Not interested resources have to be included as well since they returned XA_RDONLY and they
		// don't participate in phase 2: the TX succeded for them.
		Set<String> rolledbackAndNotInterestedUniqueNames = new HashSet<>();
		rolledbackAndNotInterestedUniqueNames.addAll(collectResourcesUniqueNames(rolledbackResources));
		List<XAResourceHolderState> notInterestedResources = collectNotInterestedResources(resourceManager.getAllResources(), interestedResources);
		rolledbackAndNotInterestedUniqueNames.addAll(collectResourcesUniqueNames(notInterestedResources));

		if (LogDebugCheck.isDebugEnabled())
		{
			List<XAResourceHolderState> rolledbackAndNotInterestedResources = new ArrayList<>();
			rolledbackAndNotInterestedResources.addAll(rolledbackResources);
			rolledbackAndNotInterestedResources.addAll(notInterestedResources);

			log.finer("rollback succeeded on resources " + Decoder.collectResourcesNames(rolledbackAndNotInterestedResources));
		}

		transaction.setStatus(Status.STATUS_ROLLEDBACK, rolledbackAndNotInterestedUniqueNames);
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
	 * @throws HeuristicCommitException
	 * 		when
	 */
	private void throwException(String message, PhaseException phaseException, int totalResourceCount) throws HeuristicMixedException, HeuristicCommitException
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
			throw new BitronixHeuristicCommitException(message + ":" +
			                                           " all resource(s) " + Decoder.collectResourcesNames(heuristicResources) +
			                                           " improperly unilaterally committed", phaseException);
		}
		else
		{
			throw new BitronixHeuristicMixedException(message + ":" +
			                                          (!errorResources.isEmpty()
			                                           ? " resource(s) " + Decoder.collectResourcesNames(errorResources) + " threw unexpected exception"
			                                           : "") +
			                                          (!errorResources.isEmpty() && !heuristicResources.isEmpty() ? " and" : "") +
			                                          (!heuristicResources.isEmpty() ? " resource(s) " + Decoder.collectResourcesNames(heuristicResources) +
			                                                                           " improperly unilaterally committed" + (hazard ? " (or hazard happened)" : "") : ""),
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
	 * @param resourceHolder
	 * 		the resource that is going to receive a command.
	 *
	 * @return the {@link bitronix.tm.twopc.executor.Job} that is going to execute the command.
	 */
	@Override
	protected Job createJob(XAResourceHolderState resourceHolder)
	{
		return new RollbackJob(resourceHolder);
	}

	private final class RollbackJob
			extends Job
	{

		/**
		 * Constructor RollbackJob creates a new RollbackJob instance.
		 *
		 * @param resourceHolder
		 * 		of type XAResourceHolderState
		 */
		public RollbackJob(XAResourceHolderState resourceHolder)
		{
			super(resourceHolder);
		}

		/**
		 * Method execute ...
		 */
		@Override
		public void execute()
		{
			try
			{
				rollbackResource(getResource());
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
		 * Method rollbackResource ...
		 *
		 * @param resourceHolder
		 * 		of type XAResourceHolderState
		 *
		 * @throws XAException
		 * 		when
		 */
		private void rollbackResource(XAResourceHolderState resourceHolder) throws XAException
		{
			try
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("trying to rollback resource " + resourceHolder);
				}
				resourceHolder.getXAResource()
				              .rollback(resourceHolder.getXid());
				rolledbackResources.add(resourceHolder);
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("rolled back resource " + resourceHolder);
				}
			}
			catch (XAException ex)
			{
				handleXAException(resourceHolder, ex);
			}
		}

		/**
		 * Method handleXAException ...
		 *
		 * @param failedResourceHolder
		 * 		of type XAResourceHolderState
		 * @param xaException
		 * 		of type XAException
		 *
		 * @throws XAException
		 * 		when
		 */
		private void handleXAException(XAResourceHolderState failedResourceHolder, XAException xaException) throws XAException
		{
			switch (xaException.errorCode)
			{
				case XAException.XA_HEURRB:
					forgetHeuristicRollback(failedResourceHolder);
					return;

				case XAException.XA_HEURCOM:
				case XAException.XA_HEURHAZ:
				case XAException.XA_HEURMIX:
					log.severe("heuristic rollback is incompatible with the global state of this transaction - guilty: " + failedResourceHolder);
					throw xaException;

				default:
					String extraErrorDetails = TransactionManagerServices.getExceptionAnalyzer()
					                                                     .extractExtraXAExceptionDetails(xaException);
					log.log(Level.WARNING, "resource '" + failedResourceHolder.getUniqueName() + "' reported " + Decoder.decodeXAExceptionErrorCode(xaException) +
					                       " when asked to rollback transaction branch. Transaction is prepared and will rollback via recovery service when resource availability allows."
					                       + (extraErrorDetails == null ? "" : " Extra error=" + extraErrorDetails), xaException);
			}
		}

		/**
		 * Method forgetHeuristicRollback ...
		 *
		 * @param resourceHolder
		 * 		of type XAResourceHolderState
		 */
		private void forgetHeuristicRollback(XAResourceHolderState resourceHolder)
		{
			try
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("handling heuristic rollback on resource " + resourceHolder.getXAResource());
				}
				resourceHolder.getXAResource()
				              .forget(resourceHolder.getXid());
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("forgotten heuristically rolled back resource " + resourceHolder.getXAResource());
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
			return "a RollbackJob with " + getResource();
		}
	}

}
