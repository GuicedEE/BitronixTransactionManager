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
import bitronix.tm.internal.BitronixRuntimeException;
import bitronix.tm.internal.LogDebugCheck;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.IncrementalRecoverer;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.common.XAStatefulHolder.State;
import bitronix.tm.utils.MonotonicClock;
import bitronix.tm.utils.Uid;

import javax.transaction.Synchronization;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

/**
 * Generic XA pool. {@link XAStatefulHolder} instances are created by the {@link XAPool} out of a
 * {@link XAResourceProducer}. Those objects are then pooled and can be retrieved and/or recycled by the pool
 * depending on the running XA transaction's and the {@link XAStatefulHolder}'s states.
 *
 * @author Ludovic Orban
 * @author Brett Wooldridge
 */
public class XAPool<R extends XAResourceHolder<R>, T extends XAStatefulHolder<T>>
		implements StateChangeListener<T>
{

	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(XAPool.class.toString());

	/**
	 * The stateTransitionLock makes sure that transitions of XAStatefulHolders from one state to another
	 * (movement from one pool to another) are atomic.  A ReentrantReadWriteLock allows any number of
	 * readers to access and iterate the accessiblePool and inaccessiblePool without blocking.  Readers
	 * are blocked only for the instant when a connection is moving between pools.  These locks are
	 * sufficient to protect the collections, which are left intentionally non-concurrent so that failures
	 * in locking logic will be quickly uncovered.
	 */
	private final ReentrantReadWriteLock stateTransitionLock = new ReentrantReadWriteLock();

	private final BlockingDeque<T> availablePool = new LinkedBlockingDeque<>();
	private final Queue<T> accessiblePool = new LinkedList<>();
	private final Queue<T> inaccessiblePool = new LinkedList<>();

	private final AtomicInteger poolSize = new AtomicInteger();

	/**
	 * This map is used to implement the connection sharing feature of Bitronix.
	 */
	private final Map<Uid, StatefulHolderThreadLocal<T>> statefulHolderTransactionMap = new ConcurrentHashMap<>();

	private final ResourceBean bean;
	private final XAResourceProducer<R, T> xaResourceProducer;
	private final Object xaFactory;
	private final AtomicBoolean failed = new AtomicBoolean();
	private final Object poolGrowthShrinkLock = new Object();

	/**
	 * Constructor XAPool creates a new XAPool instance.
	 *
	 * @param xaResourceProducer
	 * 		of type XAResourceProducer<R, T>
	 * @param bean
	 * 		of type ResourceBean
	 * @param xaFactory
	 * 		of type Object
	 *
	 * @throws Exception
	 * 		when
	 */
	public XAPool(XAResourceProducer<R, T> xaResourceProducer, ResourceBean bean, Object xaFactory) throws Exception
	{
		this.xaResourceProducer = xaResourceProducer;
		this.bean = bean;
		if (bean.getMaxPoolSize() < 1 || bean.getMinPoolSize() > bean.getMaxPoolSize())
		{
			throw new IllegalArgumentException("cannot create a pool with min " + bean.getMinPoolSize() + " connection(s) and max " + bean.getMaxPoolSize() + " connection(s)");
		}
		if (bean.getAcquireIncrement() < 1)
		{
			throw new IllegalArgumentException("cannot create a pool with a connection acquisition increment less than 1, configured value is " + bean.getAcquireIncrement());
		}

		if (xaFactory == null)
		{
			this.xaFactory = XAFactoryHelper.createXAFactory(bean);
		}
		else
		{
			this.xaFactory = xaFactory;
		}
		init();

		if (bean.getIgnoreRecoveryFailures())
		{
			log.warning("resource '" + bean.getUniqueName() + "' is configured to ignore recovery failures, make sure this setting is not enabled on a production system!");
		}
	}

	/**
	 * Method init ...
	 *
	 * @throws Exception
	 * 		when
	 */
	private void init() throws Exception
	{
		growUntilMinPoolSize();

		if (bean.getMaxIdleTime() > 0 || bean.getMaxLifeTime() > 0)
		{
			TransactionManagerServices.getTaskScheduler()
			                          .schedulePoolShrinking(this);
		}
	}

	/**
	 * Method growUntilMinPoolSize ...
	 *
	 * @throws Exception
	 * 		when
	 */
	private void growUntilMinPoolSize() throws Exception
	{
		synchronized (poolGrowthShrinkLock)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("growing " + this + " to minimum pool size " + bean.getMinPoolSize());
			}
			for (int i = totalPoolSize(); i < bean.getMinPoolSize(); i++)
			{
				createPooledObject(xaFactory);
			}
		}
	}

	/**
	 * Get the total size of this pool.
	 *
	 * @return the total size of this pool
	 */
	public int totalPoolSize()
	{
		return poolSize.get();
	}

	/**
	 * Method createPooledObject ...
	 *
	 * @param xaFactory
	 * 		of type Object
	 *
	 * @throws Exception
	 * 		when
	 */
	private void createPooledObject(Object xaFactory) throws Exception
	{
		T xaStatefulHolder = xaResourceProducer.createPooledConnection(xaFactory, bean);
		xaStatefulHolder.addStateChangeEventListener(this);
		availablePool.addLast(xaStatefulHolder);
		poolSize.incrementAndGet();
	}

	/* -----------------------------------------------------------------------------------
	 * Pool Transition.  stateChanging() and stateChanged() obtain the stateTransitionLock
	 * write lock to prevent other threads from iterating the pools while a coonection is
	 * in transition from one pool to another.  However, this window of time is extremely
	 * small, and in general allows high-concurrency.
	 * ----------------------------------------------------------------------------------*/

	/**
	 * Close down and cleanup this XAPool instance.
	 */
	public void close()
	{
		synchronized (poolGrowthShrinkLock)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("closing all connections of " + this);
			}

			for (T xaStatefulHolder : getXAResourceHolders())
			{
				try
				{
					xaStatefulHolder.close();
				}
				catch (Exception ex)
				{
					if (LogDebugCheck.isDebugEnabled())
					{
						log.log(Level.FINER, "ignoring exception while closing connection " + xaStatefulHolder, ex);
					}
				}
			}

			if (TransactionManagerServices.isTaskSchedulerRunning())
			{
				TransactionManagerServices.getTaskScheduler()
				                          .cancelPoolShrinking(this);
			}

			stateTransitionLock.writeLock()
			                   .lock();
			try
			{
				availablePool.clear();
				accessiblePool.clear();
				inaccessiblePool.clear();
				failed.set(false);
			}
			finally
			{
				stateTransitionLock.writeLock()
				                   .unlock();
			}
		}
	}

	/**
	 * Get a connection handle from this pool.
	 *
	 * @param recycle
	 * 		true if we should try to get a connection in the NON_ACCESSIBLE pool in the same transaction
	 *
	 * @return a connection handle
	 *
	 * @throws Exception
	 * 		throw in the pool is unrecoverable or a timeout occurs getting a connection
	 */
	public Object getConnectionHandle(boolean recycle) throws Exception
	{
		synchronized (poolGrowthShrinkLock)
		{
			if (isFailed())
			{
				reinitializePool();
			}
		}

		long remainingTimeMs = TimeUnit.SECONDS.toMillis(bean.getAcquisitionTimeout());
		while (true)
		{
			long before = MonotonicClock.currentTimeMillis();
			T xaStatefulHolder = null;
			if (recycle)
			{
				if (bean.getShareTransactionConnections())
				{
					xaStatefulHolder = getSharedXAStatefulHolder();
				}
				else
				{
					xaStatefulHolder = getNotAccessible();
				}
			}

			if (xaStatefulHolder == null)
			{
				xaStatefulHolder = getInPool(remainingTimeMs);
			}

			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("found " + xaStatefulHolder.getState() + " connection " + xaStatefulHolder + " from " + this);
			}

			try
			{
				// getConnectionHandle() here could throw an exception, if it doesn't the connection is
				// still alive and we can share it (if sharing is enabled)
				Object connectionHandle = xaStatefulHolder.getConnectionHandle();
				if (bean.getShareTransactionConnections())
				{
					putSharedXAStatefulHolder(xaStatefulHolder);
				}

				return connectionHandle;
			}
			catch (Exception ex)
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.log(Level.FINER, "connection is invalid, trying to close it", ex);
				}
				try
				{
					xaStatefulHolder.close();
				}
				catch (Exception ex2)
				{
					if (LogDebugCheck.isDebugEnabled())
					{
						log.log(Level.FINER, "exception while trying to close invalid connection, ignoring it", ex2);
					}
				}
				finally
				{
					if (LogDebugCheck.isDebugEnabled())
					{
						log.finer("removed invalid connection " + xaStatefulHolder + " from " + this);
					}
					if (xaStatefulHolder.getState() != State.CLOSED)
					{
						stateChanged(xaStatefulHolder, xaStatefulHolder.getState(), State.CLOSED);
					}

					if (LogDebugCheck.isDebugEnabled())
					{
						log.finer("waiting " + bean.getAcquisitionInterval() + "s before trying to acquire a connection again from " + this);
					}
					long waitTime = TimeUnit.SECONDS.toMillis(bean.getAcquisitionInterval());
					if (waitTime > 0)
					{
						waitFor(waitTime);
					}
				}

				// check for timeout
				long now = MonotonicClock.currentTimeMillis();
				remainingTimeMs -= (now - before);
				if (remainingTimeMs <= 0)
				{
					throw new BitronixRuntimeException("cannot get valid connection from " + this + " after trying for " + bean.getAcquisitionTimeout() + "s", ex);
				}
			}
		} // while true
	}

	private synchronized void waitFor(long waitTime)
	{
		try
		{
			wait(waitTime);
		}
		catch (InterruptedException ex2)
		{
			log.log(Level.FINEST, "Waiting exception", ex2);
		}
	}

	/* ------------------------------------------------------------------------
	 * Methods to obtain a connection from one of the internal pools.
	 * ------------------------------------------------------------------------*/

	/**
	 * Get a connection handle from this pool.
	 *
	 * @return a connection handle
	 *
	 * @throws Exception
	 * 		throw in the pool is unrecoverable or a timeout occurs getting a connection
	 */
	public Object getConnectionHandle() throws Exception
	{
		return getConnectionHandle(true);
	}

	/**
	 * Method stateChanged ...
	 *
	 * @param source
	 * 		of type T
	 * @param oldState
	 * 		of type State
	 * @param newState
	 * 		of type State
	 */
	@Override
	public void stateChanged(T source, State oldState, State newState)
	{
		stateTransitionLock.writeLock()
		                   .lock();
		try
		{
			switch (newState)
			{
				case IN_POOL:
					if (LogDebugCheck.isDebugEnabled())
					{
						log.finer("added " + source + " to the available pool");
					}
					availablePool.addFirst(source);
					break;
				case ACCESSIBLE:
					if (LogDebugCheck.isDebugEnabled())
					{
						log.finer("added " + source + " to the accessible pool");
					}
					accessiblePool.add(source);
					break;
				case NOT_ACCESSIBLE:
					if (LogDebugCheck.isDebugEnabled())
					{
						log.finer("added " + source + " to the inaccessible pool");
					}
					inaccessiblePool.add(source);
					break;
				case CLOSED:
					source.removeStateChangeEventListener(this);
					poolSize.decrementAndGet();
					break;
			}
		}
		finally
		{
			stateTransitionLock.writeLock()
			                   .unlock();
		}
	}

	/**
	 * Method stateChanging ...
	 *
	 * @param source
	 * 		of type T
	 * @param currentState
	 * 		of type State
	 * @param futureState
	 * 		of type State
	 */
	@Override
	public void stateChanging(T source, State currentState, State futureState)
	{
		stateTransitionLock.writeLock()
		                   .lock();
		try
		{
			switch (currentState)
			{
				case IN_POOL:
					// no-op.  calling availablePool.remove(source) here is reduncant because it was
					// already removed when availablePool.poll() was called.
					break;
				case ACCESSIBLE:
					if (LogDebugCheck.isDebugEnabled())
					{
						log.finer("removed " + source + " from the accessible pool");
					}
					accessiblePool.remove(source);
					break;
				case NOT_ACCESSIBLE:
					if (LogDebugCheck.isDebugEnabled())
					{
						log.finer("removed " + source + " from the inaccessible pool");
					}
					inaccessiblePool.remove(source);
					break;
				case CLOSED:
					break;
			}
		}
		finally
		{
			stateTransitionLock.writeLock()
			                   .unlock();
		}
	}

	/**
	 * Get an IN_POOL connection.  This method blocks for up to remainingTimeMs milliseconds
	 * for someone to return or create a connection in the available pool.  If remainingTimeMs
	 * expires, an exception is thrown.  It does not use stateTransitionLock.readLock() because
	 * the availablePool [a LinkedBlockingQueue] is already thread safe.
	 *
	 * @param remainingTimeMs
	 * 		the maximum time to wait for a connection
	 *
	 * @return a connection from the available (IN_POOL) pool
	 *
	 * @throws Exception
	 * 		thrown in no connection is available before the remainingTimeMs time expires
	 */
	private T getInPool(long remainingTimeMs) throws Exception
	{
		if (inPoolSize() == 0)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("no more free connections in " + this + ", trying to grow it");
			}
			grow();
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("getting IN_POOL connection from " + this + ", waiting if necessary");
		}

		try
		{
			T xaStatefulHolder = availablePool.pollFirst(remainingTimeMs, TimeUnit.MILLISECONDS);
			if (xaStatefulHolder == null)
			{
				if (TransactionManagerServices.isTransactionManagerRunning())
				{
					TransactionManagerServices.getTransactionManager()
					                          .dumpTransactionContexts();
				}

				throw new BitronixRuntimeException("XA pool of resource " + bean.getUniqueName() + " still empty after " + bean.getAcquisitionTimeout() + "s wait time");
			}

			if (expireStatefulHolder(xaStatefulHolder, false))
			{
				return getInPool(remainingTimeMs);
			}

			return xaStatefulHolder;
		}
		catch (InterruptedException e)
		{
			throw new BitronixRuntimeException("Interrupted while waiting for IN_POOL connection.", e);
		}
	}

	/* ------------------------------------------------------------------------
	 * Pool growth and pooled object creation
	 * ------------------------------------------------------------------------*/

	/**
	 * Get a XAStatefulHolder (connection) from the NOT_ACCESSIBLE pool.  This method obtains
	 * the stateTransitionLock.readLock() which prevents any modification during iteration, but
	 * allows multiple threads to iterate simultaneously.
	 *
	 * @return a connection, or null if there are no connections in the inaccessible pool for the current transaction
	 */
	private T getNotAccessible()
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("trying to recycle a NOT_ACCESSIBLE connection of " + this);
		}
		BitronixTransaction transaction = TransactionContextHelper.currentTransaction();
		if (transaction == null)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("no current transaction, no connection can be in state NOT_ACCESSIBLE when there is no global transaction context");
			}
			return null;
		}
		Uid currentTxGtrid = transaction.getResourceManager()
		                                .getGtrid();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("current transaction GTRID is [" + currentTxGtrid + "]");
		}

		stateTransitionLock.readLock()
		                   .lock();
		try
		{
			for (T xaStatefulHolder : inaccessiblePool)
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("found a connection in NOT_ACCESSIBLE state: " + xaStatefulHolder);
				}
				if (containsXAResourceHolderMatchingGtrid(xaStatefulHolder, currentTxGtrid))
				{
					return xaStatefulHolder;
				}
			}

			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("no NOT_ACCESSIBLE connection enlisted in this transaction");
			}
			return null;
		}
		finally
		{
			stateTransitionLock.readLock()
			                   .unlock();
		}
	}

	/**
	 * Try to get a shared XAStatefulHolder.  This method will either return a shared
	 * XAStatefulHolder or <code>null</code>.  If there is no current transaction,
	 * XAStatefulHolder's are not shared.  If there is a transaction <i>and</i> there is
	 * a XAStatefulHolder associated with this thread already, we return that XAStatefulHolder
	 * (provided it is ACCESSIBLE or NOT_ACCESSIBLE).
	 *
	 * @return a shared XAStatefulHolder or <code>null</code>
	 */
	private T getSharedXAStatefulHolder()
	{
		BitronixTransaction transaction = TransactionContextHelper.currentTransaction();
		if (transaction == null)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("no current transaction, shared connection map will not be used");
			}
			return null;
		}
		Uid currentTxGtrid = transaction.getResourceManager()
		                                .getGtrid();

		StatefulHolderThreadLocal<T> threadLocal = statefulHolderTransactionMap.get(currentTxGtrid);
		if (threadLocal != null)
		{
			T xaStatefulHolder = threadLocal.get();
			// Additional sanity checks...
			if (xaStatefulHolder != null &&
			    xaStatefulHolder.getState() != State.IN_POOL &&
			    xaStatefulHolder.getState() != State.CLOSED)
			{

				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("sharing connection " + xaStatefulHolder + " in transaction " + currentTxGtrid);
				}
				return xaStatefulHolder;
			}
		}

		return null;
	}

	/**
	 * Method containsXAResourceHolderMatchingGtrid ...
	 *
	 * @param xaStatefulHolder
	 * 		of type T
	 * @param currentTxGtrid
	 * 		of type Uid
	 *
	 * @return boolean
	 */
	private boolean containsXAResourceHolderMatchingGtrid(T xaStatefulHolder, Uid currentTxGtrid)
	{
		List<? extends XAResourceHolder<? extends XAResourceHolder>> xaResourceHolders = xaStatefulHolder.getXAResourceHolders();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer(xaResourceHolders.size() + " xa resource(s) created by connection in NOT_ACCESSIBLE state: " + xaStatefulHolder);
		}

		class LocalVisitor
				implements XAResourceHolderStateVisitor
		{
			private boolean found;

			/**
			 * Called when visiting all {@link bitronix.tm.internal.XAResourceHolderState}s.
			 * @param xaResourceHolderState the currently visited {@link bitronix.tm.internal.XAResourceHolderState}
			 * @return return <code>true</code> to continue visitation, <code>false</code> to stop visitation
			 */
			@Override
			public boolean visit(XAResourceHolderState xaResourceHolderState)
			{
				// compare GTRIDs
				BitronixXid bitronixXid = xaResourceHolderState.getXid();
				Uid resourceGtrid = bitronixXid.getGlobalTransactionIdUid();
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("NOT_ACCESSIBLE xa resource GTRID: " + resourceGtrid);
				}
				if (currentTxGtrid.equals(resourceGtrid))
				{
					if (LogDebugCheck.isDebugEnabled())
					{
						log.finer("NOT_ACCESSIBLE xa resource's GTRID matched this transaction's GTRID, recycling it");
					}
					found = true;
				}
				return !found; // continue visitation if not found, stop visitation if found
			}
		}

		for (XAResourceHolder<? extends XAResourceHolder> xaResourceHolder : xaResourceHolders)
		{
			LocalVisitor xaResourceHolderStateVisitor = new LocalVisitor();
			xaResourceHolder.acceptVisitorForXAResourceHolderStates(currentTxGtrid, xaResourceHolderStateVisitor);
			if (xaResourceHolderStateVisitor.found)
			{
				return true;
			}
		}
		return false;
	}

	/* ------------------------------------------------------------------------
	 * Pool shrinking and pooled object expiration.
	 * ------------------------------------------------------------------------*/

	/**
	 * Grow the pool by "acquire increment" amount up to the max pool size.
	 *
	 * @throws Exception
	 * 		thrown if creating a pooled objects fails
	 */
	private void grow() throws Exception
	{
		synchronized (poolGrowthShrinkLock)
		{
			long totalPoolSize = totalPoolSize();
			if (totalPoolSize < bean.getMaxPoolSize())
			{
				long increment = bean.getAcquireIncrement();
				if (totalPoolSize + increment > bean.getMaxPoolSize())
				{
					increment = bean.getMaxPoolSize() - totalPoolSize;
				}

				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("incrementing " + bean.getUniqueName() + " pool size by " + increment + " unit(s) to reach " + (totalPoolSize() + increment) + " connection(s)");
				}
				for (int i = 0; i < increment; i++)
				{
					createPooledObject(xaFactory);
				}
			}
			else
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("pool " + bean.getUniqueName() + " already at max size of " + totalPoolSize() + " connection(s), not growing it");
				}
			}

			if (totalPoolSize() < bean.getMinPoolSize())
			{
				growUntilMinPoolSize();
			}
		}
	}

	/**
	 * Method shrink ...
	 *
	 * @throws Exception
	 * 		when
	 */
	public void shrink() throws Exception
	{
		synchronized (poolGrowthShrinkLock)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("shrinking " + this);
			}
			expireOrCloseStatefulHolders(false);
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("shrunk " + this);
			}
		}
	}

	/**
	 * Method expireOrCloseStatefulHolders ...
	 *
	 * @param forceClose
	 * 		of type boolean
	 *
	 * @throws Exception
	 * 		when
	 */
	private void expireOrCloseStatefulHolders(boolean forceClose) throws Exception
	{
		int closed = 0;
		int availableSize = availablePool.size();
		for (int i = 0; i < availableSize; i++)
		{
			T xaStatefulHolder = availablePool.pollFirst();
			if (xaStatefulHolder == null)
			{
				break;
			}

			if (expireStatefulHolder(xaStatefulHolder, forceClose))
			{
				closed++;
			}
			else
			{
				availablePool.addLast(xaStatefulHolder);
			}
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("closed " + closed + (forceClose ? " " : " idle ") + "connection(s)");
		}

		growUntilMinPoolSize();
	}

	/**
	 * Method expireStatefulHolder ...
	 *
	 * @param xaStatefulHolder
	 * 		of type T
	 * @param forceClose
	 * 		of type boolean
	 *
	 * @return boolean
	 */
	private boolean expireStatefulHolder(T xaStatefulHolder, boolean forceClose)
	{
		long expirationTime = Long.MAX_VALUE;
		if (bean.getMaxIdleTime() > 0)
		{
			expirationTime = xaStatefulHolder.getLastReleaseDate()
			                                 .getTime() + TimeUnit.SECONDS.toMillis(bean.getMaxIdleTime());
		}

		if (bean.getMaxLifeTime() > 0)
		{
			long endOfLife = xaStatefulHolder.getCreationDate()
			                                 .getTime() + TimeUnit.SECONDS.toMillis(bean.getMaxLifeTime());
			expirationTime = Math.min(expirationTime, endOfLife);
		}

		long now = MonotonicClock.currentTimeMillis();
		if (!forceClose && LogDebugCheck.isDebugEnabled())
		{
			log.finer("checking if connection can be closed: " + xaStatefulHolder + " - closing time: " + expirationTime + ", now time: " + now);
		}
		if (expirationTime <= now || forceClose)
		{
			try
			{
				xaStatefulHolder.close();
			}
			catch (Exception ex)
			{
				log.log(Level.WARNING, "error closing " + xaStatefulHolder, ex);
			}
			return true;
		}
		return false;
	}

	/**
	 * Method getNextShrinkDate returns the nextShrinkDate of this XAPool object.
	 *
	 * @return the nextShrinkDate (type Date) of this XAPool object.
	 */
	public Date getNextShrinkDate()
	{
		return new Date(MonotonicClock.currentTimeMillis() + TimeUnit.SECONDS.toMillis(bean.getMaxIdleTime()));
	}

	/**
	 * Method reset ...
	 *
	 * @throws Exception
	 * 		when
	 */
	public void reset() throws Exception
	{
		synchronized (poolGrowthShrinkLock)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("resetting " + this);
			}
			expireOrCloseStatefulHolders(true);
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("reset " + this);
			}
		}
	}

	/* ------------------------------------------------------------------------
	 * Miscellaneous public methods
	 * ------------------------------------------------------------------------*/

	/**
	 * Method reinitializePool ...
	 */
	private void reinitializePool()
	{
		try
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("resource '" + bean.getUniqueName() + "' is marked as failed, resetting and recovering it before trying connection acquisition");
			}
			close();
			init();
			IncrementalRecoverer.recover(xaResourceProducer);
		}
		catch (RecoveryException ex)
		{
			throw new BitronixRuntimeException("incremental recovery failed when trying to acquire a connection from failed resource '" + bean.getUniqueName() + "'", ex);
		}
		catch (Exception ex)
		{
			throw new BitronixRuntimeException("pool reset failed when trying to acquire a connection from failed resource '" + bean.getUniqueName() + "'", ex);
		}
	}

	/**
	 * Get the XAFactory (XADataSource) that produces objects for this pool.
	 *
	 * @return the factory (XADataSource) object
	 */
	public Object getXAFactory()
	{
		return xaFactory;
	}

	/**
	 * Method getXAResourceHolders returns the XAResourceHolders of this XAPool object.
	 *
	 * @return the XAResourceHolders (type List<T>) of this XAPool object.
	 */
	public List<T> getXAResourceHolders()
	{
		stateTransitionLock.readLock()
		                   .lock();
		try
		{
			List<T> holders = new ArrayList<>();
			holders.addAll(availablePool);
			holders.addAll(accessiblePool);
			holders.addAll(inaccessiblePool);
			return holders;
		}
		finally
		{
			stateTransitionLock.readLock()
			                   .unlock();
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
		return "an XAPool of resource " + bean.getUniqueName() + " with " + totalPoolSize() + " connection(s) (" + inPoolSize() + " still available)" +
		       (isFailed() ? " -failed-" : "");
	}

	/**
	 * Get the number of objects in the available pool.
	 *
	 * @return the number of available objects
	 */
	public int inPoolSize()
	{
		return availablePool.size();
	}

	/**
	 * Is the XAPool in a failed state?
	 *
	 * @return true if this XAPool has failed, false otherwise
	 */
	public boolean isFailed()
	{
		return failed.get();
	}

	/**
	 * Sets this XAPool as failed or unfailed, requiring recovery.
	 *
	 * @param failed
	 * 		true if this XAPool has failed and requires recovery, false if it is ok
	 */
	public void setFailed(boolean failed)
	{
		this.failed.set(failed);
	}

	/* ------------------------------------------------------------------------
	 * Shared Connection Handling
	 * ------------------------------------------------------------------------*/

	/**
	 * Try to share a XAStatefulHolder with other callers on this thread.  If
	 * there is no current transaction, the XAStatefulHolder is not put into the
	 * ThreadLocal.  If there is a transaction, and it is the first time we're
	 * attempting to share a XAStatefulHolder on this thread, then we register
	 * a Synchronization so we can pull the ThreadLocals out of the shared map
	 * when the transaction completes (either commit() or rollback()).  Without
	 * the Synchronization we would "leak".
	 *
	 * @param xaStatefulHolder
	 * 		a XAStatefulHolder to share with other callers
	 * 		on this thread.
	 */
	private void putSharedXAStatefulHolder(T xaStatefulHolder)
	{
		BitronixTransaction transaction = TransactionContextHelper.currentTransaction();
		if (transaction == null)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("no current transaction, not adding " + xaStatefulHolder + " to shared connection map");
			}
			return;
		}
		Uid currentTxGtrid = transaction.getResourceManager()
		                                .getGtrid();

		StatefulHolderThreadLocal<T> threadLocal = statefulHolderTransactionMap.get(currentTxGtrid);
		if (threadLocal == null)
		{
			// This is the first time this TxGtrid/ThreadLocal is going into the map,
			// register interest in synchronization so we can remove it at commit/rollback
			try
			{
				transaction.registerSynchronization(new SharedStatefulHolderCleanupSynchronization(currentTxGtrid));
			}
			catch (Exception e)
			{
				log.log(Level.FINEST, "Ignoreing Exception", e);
				// OK, forget it.  The transaction is either rollback only or already finished.
				return;
			}

			threadLocal = new StatefulHolderThreadLocal<>();
			statefulHolderTransactionMap.put(currentTxGtrid, threadLocal);
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("added shared connection mapping for " + currentTxGtrid + " holder " + xaStatefulHolder);
			}
		}

		// Set the XAStatefulHolder on the ThreadLocal.  Even if we've already set it before,
		// it's safe -- checking would be more expensive than just setting it again.
		threadLocal.set(xaStatefulHolder);
	}

	private static final class StatefulHolderThreadLocal<T extends XAStatefulHolder>
			extends ThreadLocal<T>
	{
		//Nothing required
	}

	private final class SharedStatefulHolderCleanupSynchronization
			implements Synchronization
	{
		private final Uid gtrid;

		/**
		 * Constructor SharedStatefulHolderCleanupSynchronization creates a new SharedStatefulHolderCleanupSynchronization instance.
		 *
		 * @param gtrid
		 * 		of type Uid
		 */
		private SharedStatefulHolderCleanupSynchronization(Uid gtrid)
		{
			this.gtrid = gtrid;
		}

		/**
		 * Method beforeCompletion ...
		 */
		@Override
		public void beforeCompletion()
		{
			//Nothing required
		}

		/**
		 * Method afterCompletion ...
		 *
		 * @param status
		 * 		of type int
		 */
		@Override
		public void afterCompletion(int status)
		{
			statefulHolderTransactionMap.remove(gtrid);
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("deleted shared connection mappings for " + gtrid);
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
			return "a SharedStatefulHolderCleanupSynchronization with GTRID [" + gtrid + "]";
		}
	}
}
