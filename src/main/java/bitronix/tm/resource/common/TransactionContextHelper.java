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
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.BitronixSystemException;
import bitronix.tm.internal.LogDebugCheck;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.resource.common.XAStatefulHolder.State;
import bitronix.tm.utils.Scheduler;
import bitronix.tm.utils.Uid;

import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;
import java.util.List;

/**
 * Helper class that contains static logic common across all resource types.
 *
 * @author Ludovic Orban
 */
public final class TransactionContextHelper
{

	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(TransactionContextHelper.class.toString());
	private static final String FROM_SPACE = " from ";

	/**
	 * Constructor TransactionContextHelper creates a new TransactionContextHelper instance.
	 */
	// do not instantiate
	private TransactionContextHelper()
	{
	}

	/**
	 * Enlist the {@link XAResourceHolder} in the current transaction or do nothing if there is no global transaction
	 * context for this thread.
	 *
	 * @param xaResourceHolder
	 * 		the {@link XAResourceHolder} to enlist.
	 *
	 * @throws SystemException
	 * 		if an internal error happens.
	 * @throws RollbackException
	 * 		if the current transaction has been marked as rollback only.
	 */
	public static void enlistInCurrentTransaction(XAResourceHolder<? extends XAResourceHolder> xaResourceHolder) throws SystemException, RollbackException
	{
		BitronixTransaction currentTransaction = currentTransaction();
		ResourceBean bean = xaResourceHolder.getResourceBean();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("enlisting " + xaResourceHolder + " into " + currentTransaction);
		}

		if (currentTransaction != null)
		{
			if (currentTransaction.timedOut())
			{
				throw new BitronixSystemException("transaction timed out");
			}

			// in case multiple unjoined branches of the current transaction have run on the resource,
			// only the last one counts as all the first ones are ended already
			XAResourceHolderState alreadyEnlistedXAResourceHolderState = TransactionContextHelper.getLatestAlreadyEnlistedXAResourceHolderState(xaResourceHolder,
			                                                                                                                                    currentTransaction);
			if (alreadyEnlistedXAResourceHolderState == null || alreadyEnlistedXAResourceHolderState.isEnded())
			{
				currentTransaction.enlistResource(xaResourceHolder.getXAResource());
			}
			else if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("avoiding re-enlistment of already enlisted but not ended resource " + alreadyEnlistedXAResourceHolderState);
			}
		}
		else
		{
			if (bean.getAllowLocalTransactions())
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("in local transaction context, skipping enlistment");
				}
			}
			else
			{
				throw new BitronixSystemException("resource '" + bean.getUniqueName() + "' cannot be used outside XA " +
				                                  "transaction scope. Set allowLocalTransactions to true if you want to allow this and you know " +
				                                  "your resource supports this.");
			}
		}
	}

	/**
	 * Get the transaction running on the current thead context.
	 *
	 * @return null if there is no transaction on the current context or if the transaction manager is not running.
	 */
	public static BitronixTransaction currentTransaction()
	{
		if (!TransactionManagerServices.isTransactionManagerRunning())
		{
			return null;
		}
		return TransactionManagerServices.getTransactionManager()
		                                 .getCurrentTransaction();
	}

	/**
	 * Method getLatestAlreadyEnlistedXAResourceHolderState ...
	 *
	 * @param xaResourceHolder
	 * 		of type XAResourceHolder
	 * @param currentTransaction
	 * 		of type BitronixTransaction
	 *
	 * @return XAResourceHolderState
	 */
	private static XAResourceHolderState getLatestAlreadyEnlistedXAResourceHolderState(XAResourceHolder xaResourceHolder, BitronixTransaction currentTransaction)
	{
		if (currentTransaction == null)
		{
			return null;
		}

		class LocalVisitor
				implements XAResourceHolderStateVisitor
		{
			private XAResourceHolderState latestEnlistedHolder;

			/**
			 * Called when visiting all {@link bitronix.tm.internal.XAResourceHolderState}s.
			 * @param xaResourceHolderState the currently visited {@link bitronix.tm.internal.XAResourceHolderState}
			 * @return return <code>true</code> to continue visitation, <code>false</code> to stop visitation
			 */
			@Override
			public boolean visit(XAResourceHolderState xaResourceHolderState)
			{
				if (xaResourceHolderState != null && xaResourceHolderState.getXid() != null)
				{
					BitronixXid bitronixXid = xaResourceHolderState.getXid();
					Uid resourceGtrid = bitronixXid.getGlobalTransactionIdUid();
					Uid currentTransactionGtrid = currentTransaction.getResourceManager()
					                                                .getGtrid();

					if (currentTransactionGtrid.equals(resourceGtrid))
					{
						latestEnlistedHolder = xaResourceHolderState;
					}
				}
				return true;  // continue visitation
			}
		}
		LocalVisitor xaResourceHolderStateVisitor = new LocalVisitor();
		xaResourceHolder.acceptVisitorForXAResourceHolderStates(currentTransaction.getResourceManager()
		                                                                          .getGtrid(), xaResourceHolderStateVisitor);

		return xaResourceHolderStateVisitor.latestEnlistedHolder;
	}

	/**
	 * Delist the {@link XAResourceHolder} from the current transaction or do nothing if there is no global transaction
	 * context for this thread.
	 *
	 * @param xaResourceHolder
	 * 		the {@link XAResourceHolder} to delist.
	 *
	 * @throws SystemException
	 * 		if an internal error happens.
	 */
	public static void delistFromCurrentTransaction(XAResourceHolder<? extends XAResourceHolder> xaResourceHolder) throws SystemException
	{
		BitronixTransaction currentTransaction = currentTransaction();
		ResourceBean bean = xaResourceHolder.getResourceBean();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("delisting " + xaResourceHolder + FROM_SPACE + currentTransaction);
		}

		// End resource as eagerly as possible. This allows to release connections to the pool much earlier
		// with resources fully supporting transaction interleaving.
		if (isInEnlistingGlobalTransactionContext(xaResourceHolder, currentTransaction) && !bean.getDeferConnectionRelease())
		{

			class LocalVisitor
					implements XAResourceHolderStateVisitor
			{
				private SystemException systemException = null;

				/**
				 * Called when visiting all {@link bitronix.tm.internal.XAResourceHolderState}s.
				 * @param xaResourceHolderState the currently visited {@link bitronix.tm.internal.XAResourceHolderState}
				 * @return return <code>true</code> to continue visitation, <code>false</code> to stop visitation
				 */
				@Override
				public boolean visit(XAResourceHolderState xaResourceHolderState)
				{
					if (!xaResourceHolderState.isEnded())
					{
						if (LogDebugCheck.isDebugEnabled())
						{
							log.finer("delisting resource " + xaResourceHolderState + FROM_SPACE + currentTransaction);
						}

						// Watch out: the delistResource() call might throw a BitronixRollbackSystemException to indicate a unilateral rollback.
						try
						{
							currentTransaction.delistResource(xaResourceHolderState.getXAResource(), XAResource.TMSUCCESS);
						}
						catch (SystemException e)
						{
							systemException = e;
							return false; // stop visitation
						}
					}
					else if (LogDebugCheck.isDebugEnabled())
					{
						log.finer("avoiding delistment of not enlisted resource " + xaResourceHolderState);
					}
					return true; // continue visitation
				}
			}

			LocalVisitor xaResourceHolderStateVisitor = new LocalVisitor();
			xaResourceHolder.acceptVisitorForXAResourceHolderStates(currentTransaction.getResourceManager()
			                                                                          .getGtrid(), xaResourceHolderStateVisitor);

			if (xaResourceHolderStateVisitor.systemException != null)
			{
				throw xaResourceHolderStateVisitor.systemException;
			}
		} // isInEnlistingGlobalTransactionContext
	}

	/**
	 * Method isInEnlistingGlobalTransactionContext ...
	 *
	 * @param xaResourceHolder
	 * 		of type XAResourceHolder<? extends XAResourceHolder>
	 * @param currentTransaction
	 * 		of type BitronixTransaction
	 *
	 * @return boolean
	 */
	private static boolean isInEnlistingGlobalTransactionContext(XAResourceHolder<? extends XAResourceHolder> xaResourceHolder, BitronixTransaction currentTransaction)
	{
		boolean globalTransactionMode = false;
		if (currentTransaction != null && xaResourceHolder.isExistXAResourceHolderStatesForGtrid(currentTransaction.getResourceManager()
		                                                                                                           .getGtrid()))
		{
			globalTransactionMode = true;
		}
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("resource is " + (globalTransactionMode ? "" : "not ") + "in enlisting global transaction context: " + xaResourceHolder);
		}
		return globalTransactionMode;
	}


	/* private methods must not call TransactionManagerServices.getTransactionManager().getCurrentTransaction() */

	/**
	 * Switch the {@link XAStatefulHolder}'s state appropriately after the acquired resource handle has been closed.
	 * The pooled resource will either be marked as closed or not accessible, depending on the value of the bean's
	 * <code>deferConnectionRelease</code> property and will be marked for release after 2PC execution in the latter case.
	 *
	 * @param xaStatefulHolder
	 * 		the {@link XAStatefulHolder} to requeue.
	 * @param bean
	 * 		the {@link ResourceBean} of the {@link XAResourceHolder}.
	 *
	 * @throws BitronixSystemException
	 * 		if an internal error happens.
	 */
	public static void requeue(XAStatefulHolder<? extends XAStatefulHolder> xaStatefulHolder, ResourceBean bean) throws BitronixSystemException
	{
		BitronixTransaction currentTransaction = currentTransaction();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("requeuing " + xaStatefulHolder + FROM_SPACE + currentTransaction);
		}

		if (!TransactionContextHelper.isInEnlistingGlobalTransactionContext(xaStatefulHolder, currentTransaction))
		{
			if (!TransactionContextHelper.isEnlistedInSomeTransaction(xaStatefulHolder))
			{
				// local mode, always requeue connection immediately
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("resource not in enlisting global transaction context, immediately releasing to pool " + xaStatefulHolder);
				}
				xaStatefulHolder.setState(State.IN_POOL);
			}
			else
			{
				throw new BitronixSystemException("cannot close a resource when its XAResource is taking part in an unfinished global transaction");
			}
		}
		else if (bean.getDeferConnectionRelease())
		{
			// global mode, defer connection requeuing
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("deferring release to pool of " + xaStatefulHolder);
			}

			if (!TransactionContextHelper.isAlreadyRegisteredForDeferredRelease(xaStatefulHolder, currentTransaction))
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("registering DeferredReleaseSynchronization for " + xaStatefulHolder);
				}
				DeferredReleaseSynchronization synchronization = new DeferredReleaseSynchronization(xaStatefulHolder);
				currentTransaction.getSynchronizationScheduler()
				                  .add(synchronization, Scheduler.ALWAYS_LAST_POSITION);
			}
			else if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("already registered DeferredReleaseSynchronization for " + xaStatefulHolder);
			}

			xaStatefulHolder.setState(State.NOT_ACCESSIBLE);
		}
		else
		{
			// global mode, immediate connection requeuing
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("immediately releasing to pool " + xaStatefulHolder);
			}
			xaStatefulHolder.setState(State.IN_POOL);
		}
	}

	/**
	 * Method isInEnlistingGlobalTransactionContext ...
	 *
	 * @param xaStatefulHolder
	 * 		of type XAStatefulHolder<? extends XAStatefulHolder>
	 * @param currentTransaction
	 * 		of type BitronixTransaction
	 *
	 * @return boolean
	 */
	private static boolean isInEnlistingGlobalTransactionContext(XAStatefulHolder<? extends XAStatefulHolder> xaStatefulHolder, BitronixTransaction currentTransaction)
	{
		List<? extends XAResourceHolder<? extends XAResourceHolder>> xaResourceHolders = xaStatefulHolder.getXAResourceHolders();
		if (xaResourceHolders == null || xaResourceHolders.isEmpty())
		{
			return false;
		}

		for (XAResourceHolder<? extends XAResourceHolder> xaResourceHolder : xaResourceHolders)
		{
			if (isInEnlistingGlobalTransactionContext(xaResourceHolder, currentTransaction))
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Method isEnlistedInSomeTransaction ...
	 *
	 * @param xaStatefulHolder
	 * 		of type XAStatefulHolder<? extends XAStatefulHolder>
	 *
	 * @return boolean
	 *
	 * @throws BitronixSystemException
	 * 		when
	 */
	private static boolean isEnlistedInSomeTransaction(XAStatefulHolder<? extends XAStatefulHolder> xaStatefulHolder) throws BitronixSystemException
	{
		List<? extends XAResourceHolder<? extends XAResourceHolder>> xaResourceHolders = xaStatefulHolder.getXAResourceHolders();
		if (xaResourceHolders == null || xaResourceHolders.isEmpty())
		{
			return false;
		}

		for (XAResourceHolder<? extends XAResourceHolder> xaResourceHolder : xaResourceHolders)
		{
			if (isEnlistedInSomeTransaction(xaResourceHolder))
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Method isAlreadyRegisteredForDeferredRelease ...
	 *
	 * @param xaStatefulHolder
	 * 		of type XAStatefulHolder<? extends XAStatefulHolder>
	 * @param currentTransaction
	 * 		of type BitronixTransaction
	 *
	 * @return boolean
	 */
	private static boolean isAlreadyRegisteredForDeferredRelease(XAStatefulHolder<? extends XAStatefulHolder> xaStatefulHolder, BitronixTransaction currentTransaction)
	{
		boolean alreadyDeferred = findDeferredRelease(xaStatefulHolder, currentTransaction) != null;
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer(xaStatefulHolder + " is " + (alreadyDeferred ? "" : "not ") + "already registered for deferred release in " + currentTransaction);
		}
		return alreadyDeferred;
	}

	/**
	 * Method isEnlistedInSomeTransaction ...
	 *
	 * @param xaResourceHolder
	 * 		of type XAResourceHolder<? extends XAResourceHolder>
	 *
	 * @return boolean
	 *
	 * @throws BitronixSystemException
	 * 		when
	 */
	private static boolean isEnlistedInSomeTransaction(XAResourceHolder<? extends XAResourceHolder> xaResourceHolder)
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("looking in in-flight transactions for XAResourceHolderState of " + xaResourceHolder);
		}

		if (!TransactionManagerServices.isTransactionManagerRunning())
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("transaction manager not running, there is no in-flight transaction");
			}
			return false;
		}

		return xaResourceHolder.hasStateForXAResource(xaResourceHolder);
	}

	/**
	 * Method findDeferredRelease ...
	 *
	 * @param xaStatefulHolder
	 * 		of type XAStatefulHolder<? extends XAStatefulHolder>
	 * @param currentTransaction
	 * 		of type BitronixTransaction
	 *
	 * @return DeferredReleaseSynchronization
	 */
	private static DeferredReleaseSynchronization findDeferredRelease(XAStatefulHolder<? extends XAStatefulHolder> xaStatefulHolder, BitronixTransaction currentTransaction)
	{
		Scheduler<Synchronization> synchronizationScheduler = currentTransaction.getSynchronizationScheduler();

		for (Synchronization synchronization : synchronizationScheduler)
		{
			if (synchronization instanceof DeferredReleaseSynchronization)
			{
				DeferredReleaseSynchronization deferredReleaseSynchronization = (DeferredReleaseSynchronization) synchronization;
				if (deferredReleaseSynchronization.getXAStatefulHolder() == xaStatefulHolder)
				{
					return deferredReleaseSynchronization;
				}
			} // if synchronization instanceof DeferredReleaseSynchronization
		} // for

		return null;
	}

	/**
	 * Ensure the {@link XAStatefulHolder}'s release won't be deferred anymore (when appropriate) as it has been recycled.
	 *
	 * @param xaStatefulHolder
	 * 		the recycled {@link XAStatefulHolder}.
	 */
	public static void recycle(XAStatefulHolder<? extends XAStatefulHolder> xaStatefulHolder)
	{
		BitronixTransaction currentTransaction = currentTransaction();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("marking " + xaStatefulHolder + " as recycled in " + currentTransaction);
		}
		if (currentTransaction != null)
		{
			Scheduler<Synchronization> synchronizationScheduler = currentTransaction.getSynchronizationScheduler();

			DeferredReleaseSynchronization deferredReleaseSynchronization = findDeferredRelease(xaStatefulHolder, currentTransaction);
			if (deferredReleaseSynchronization != null)
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer(xaStatefulHolder + " has been recycled, unregistering deferred release from " + currentTransaction);
				}
				synchronizationScheduler.remove(deferredReleaseSynchronization);
			}
		}
	}
}
