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
package bitronix.tm;

import bitronix.tm.internal.BitronixSystemException;
import bitronix.tm.internal.LogDebugCheck;
import bitronix.tm.internal.ThreadContext;
import bitronix.tm.internal.XAResourceManager;
import bitronix.tm.utils.*;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import jakarta.transaction.*;
import javax.transaction.xa.XAException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Implementation of {@link TransactionManager} and {@link UserTransaction}.
 *
 * @author Ludovic Orban
 */
public class BitronixTransactionManager
		implements TransactionManager, UserTransaction, Referenceable, Service
{

	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(BitronixTransactionManager.class.toString());
	private static final String NO_TRANSACTION_TEXT = "no transaction started on this thread";
	private final SortedMap<BitronixTransaction, ClearContextSynchronization> inFlightTransactions;
	private volatile boolean shuttingDown;

	/**
	 * Create the {@link BitronixTransactionManager}. Open the journal, load resources and perform recovery
	 * synchronously. The recovery service then gets scheduled for background recovery.
	 */
	public BitronixTransactionManager()
	{
		try
		{
			shuttingDown = false;
			logVersion();
			Configuration configuration = TransactionManagerServices.getConfiguration();
			configuration.buildServerIdArray(); // first call will initialize the ServerId

			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("starting BitronixTransactionManager using " + configuration);
			}
			TransactionManagerServices.getJournal()
			                          .open();
			TransactionManagerServices.getResourceLoader()
			                          .init();
			TransactionManagerServices.getRecoverer()
			                          .run();

			int backgroundRecoveryInterval = TransactionManagerServices.getConfiguration()
			                                                           .getBackgroundRecoveryIntervalSeconds();
			if (backgroundRecoveryInterval < 1)
			{
				throw new InitializationException(
						"invalid configuration value for backgroundRecoveryIntervalSeconds, found '" + backgroundRecoveryInterval + "' but it must be greater than 0");
			}

			inFlightTransactions = createInFlightTransactionsMap();

			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("recovery will run in the background every " + backgroundRecoveryInterval + " second(s)");
			}
			Date nextExecutionDate = new Date(MonotonicClock.currentTimeMillis() + (backgroundRecoveryInterval * 1000L));
			TransactionManagerServices.getTaskScheduler()
			                          .scheduleRecovery(TransactionManagerServices.getRecoverer(), nextExecutionDate);
		}
		catch (IOException ex)
		{
			throw new InitializationException("cannot open disk journal", ex);
		}
		catch (Exception ex)
		{
			TransactionManagerServices.getJournal()
			                          .shutdown();
			TransactionManagerServices.getResourceLoader()
			                          .shutdown();
			throw new InitializationException("initialization failed, cannot safely start the transaction manager", ex);
		}
	}

	/**
	 * Output BTM version information as INFO log.
	 */
	private void logVersion()
	{
		log.info("Bitronix Transaction Manager version " + Version.getVersion());
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("JVM version " + System.getProperty("java.version"));
		}
	}

	/**
	 * Method createInFlightTransactionsMap ...
	 *
	 * @return SortedMap<BitronixTransaction
                       *       	   	   ,
                       *       	   	   ClearContextSynchronization>
	 *
	 * @throws InstantiationException
	 * 		when
	 * @throws IllegalAccessException
	 * 		when
	 * @throws InvocationTargetException
	 * 		when
	 * @throws NoSuchMethodException
	 * 		when
	 */
	private SortedMap<BitronixTransaction, ClearContextSynchronization> createInFlightTransactionsMap()
			throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException
	{
		boolean debug = LogDebugCheck.isDebugEnabled();
		if (debug)
		{
			log.finer("Creating sorted memory storage for inflight transactions.");
		}

		Comparator<BitronixTransaction> timestampSortComparator = (t1, t2) ->
		{
			Long timestamp1 = t1.getResourceManager()
			                    .getGtrid()
			                    .extractTimestamp();
			Long timestamp2 = t2.getResourceManager()
			                    .getGtrid()
			                    .extractTimestamp();

			int compareTo = timestamp1.compareTo(timestamp2);
			if (compareTo == 0 && !t1.getResourceManager()
			                         .getGtrid()
			                         .equals(t2.getResourceManager()
			                                   .getGtrid()))
			{
				// if timestamps are equal, use the Uid as the tie-breaker.  the !equals() check above avoids an expensive string compare() here.
				return t1.getGtrid()
				         .compareTo(t2.getGtrid());
			}
			return compareTo;
		};

		if (debug)
		{
			log.finer("Attempting to use a concurrent sorted map of type 'ConcurrentSkipListMap' (from jre6 or custom supplied backport)");
		}
		try
		{
			@SuppressWarnings("unchecked")
			SortedMap<BitronixTransaction, ClearContextSynchronization> mapInstance = (SortedMap<BitronixTransaction, ClearContextSynchronization>)
					                                                                          ClassLoaderUtils.loadClass("java.util.concurrent.ConcurrentSkipListMap")
					                                                                                          .
							                                                                                          getConstructor(Comparator.class)
					                                                                                          .newInstance(timestampSortComparator);
			return mapInstance;
		}
		catch (ClassNotFoundException e)
		{
			if (debug)
			{
				log.log(Level.FINER, "Concurrent sorted map 'ConcurrentSkipListMap' is not available. Falling back to a synchronized TreeMap.", e);
			}
			return Collections.synchronizedSortedMap(
					new TreeMap<>(timestampSortComparator));
		}
	}

	/**
	 * Start a new transaction and bind the context to the calling thread.
	 *
	 * @throws NotSupportedException
	 * 		if a transaction is already bound to the calling thread.
	 * @throws SystemException
	 * 		if the transaction manager is shutting down.
	 */
	@Override
	public void begin() throws NotSupportedException, SystemException
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("beginning a new transaction");
		}
		if (isShuttingDown())
		{
			throw new BitronixSystemException("cannot start a new transaction, transaction manager is shutting down");
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			dumpTransactionContexts();
		}

		BitronixTransaction currentTx = getCurrentTransaction();
		if (currentTx == null)
		{
			currentTx = createTransaction();
		}

		ThreadContext threadContext = ThreadContext.getContext();
		ClearContextSynchronization clearContextSynchronization = new ClearContextSynchronization(currentTx, threadContext);
		try
		{
			currentTx.getSynchronizationScheduler()
			         .add(clearContextSynchronization, Scheduler.ALWAYS_LAST_POSITION - 1);
			currentTx.setActive(threadContext.getTimeout());
			inFlightTransactions.put(currentTx, clearContextSynchronization);
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("begun new transaction at " + new Date(currentTx.getResourceManager()
				                                                          .getGtrid()
				                                                          .extractTimestamp()));
			}
		}
		catch (RuntimeException | SystemException ex)
		{
			clearContextSynchronization.afterCompletion(Status.STATUS_NO_TRANSACTION);
			throw ex;
		}
	}

	/**
	 * Method commit ...
	 *
	 * @throws RollbackException
	 * 		when
	 * @throws HeuristicMixedException
	 * 		when
	 * @throws HeuristicRollbackException
	 * 		when
	 * @throws SystemException
	 * 		when
	 */
	@Override
	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException
	{
		BitronixTransaction currentTx = getCurrentTransaction();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("committing transaction " + currentTx);
		}
		if (currentTx == null)
		{
			throw new IllegalStateException(NO_TRANSACTION_TEXT);
		}

		currentTx.commit();
	}

	/**
	 * Method getStatus returns the status of this BitronixTransactionManager object.
	 *
	 * @return the status (type int) of this BitronixTransactionManager object.
	 *
	 * @throws SystemException
	 * 		when
	 */
	@Override
	public int getStatus() throws SystemException
	{
		BitronixTransaction currentTx = getCurrentTransaction();
		if (currentTx == null)
		{
			return Status.STATUS_NO_TRANSACTION;
		}

		return currentTx.getStatus();
	}

	/**
	 * Method getTransaction returns the transaction of this BitronixTransactionManager object.
	 *
	 * @return the transaction (type Transaction) of this BitronixTransactionManager object.
	 *
	 * @throws SystemException
	 * 		when
	 */
	@Override
	public Transaction getTransaction() throws SystemException
	{
		return getCurrentTransaction();
	}

	/**
	 * Method resume ...
	 *
	 * @param transaction
	 * 		of type Transaction
	 *
	 * @throws InvalidTransactionException
	 * 		when
	 * @throws SystemException
	 * 		when
	 */
	@Override
	public void resume(Transaction transaction) throws InvalidTransactionException, SystemException
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("resuming " + transaction);
		}
		if (transaction == null)
		{
			throw new InvalidTransactionException("resumed transaction cannot be null");
		}
		if (!(transaction instanceof BitronixTransaction))
		{
			throw new InvalidTransactionException("resumed transaction must be an instance of BitronixTransaction");
		}

		BitronixTransaction tx = (BitronixTransaction) transaction;
		if (getCurrentTransaction() != null)
		{
			throw new IllegalStateException("a transaction is already running on this thread");
		}

		try
		{
			XAResourceManager resourceManager = tx.getResourceManager();
			resourceManager.resume();
			ThreadContext threadContext = ThreadContext.getContext();
			threadContext.setTransaction(tx);
			inFlightTransactions.get(tx)
			                    .setThreadContext(threadContext);
		}
		catch (XAException ex)
		{
			String extraErrorDetails = TransactionManagerServices.getExceptionAnalyzer()
			                                                     .extractExtraXAExceptionDetails(ex);
			throw new BitronixSystemException("cannot resume " + tx + ", error=" + Decoder.decodeXAExceptionErrorCode(ex) +
			                                  (extraErrorDetails == null ? "" : ", extra error=" + extraErrorDetails), ex);
		}
	}

	/**
	 * Method rollback ...
	 *
	 * @throws SystemException
	 * 		when
	 */
	@Override
	public void rollback() throws SystemException
	{
		BitronixTransaction currentTx = getCurrentTransaction();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("rolling back transaction " + currentTx);
		}
		if (currentTx == null)
		{
			throw new IllegalStateException(NO_TRANSACTION_TEXT);
		}

		currentTx.rollback();
	}

	/**
	 * Method setRollbackOnly ...
	 *
	 * @throws SystemException
	 * 		when
	 */
	@Override
	public void setRollbackOnly() throws SystemException
	{
		BitronixTransaction currentTx = getCurrentTransaction();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("marking transaction as rollback only: " + currentTx);
		}
		if (currentTx == null)
		{
			throw new IllegalStateException(NO_TRANSACTION_TEXT);
		}

		currentTx.setRollbackOnly();
	}

	/**
	 * Method setTransactionTimeout sets the transactionTimeout of this BitronixTransactionManager object.
	 *
	 * @param seconds
	 * 		the transactionTimeout of this BitronixTransactionManager object.
	 *
	 * @throws SystemException
	 * 		when
	 */
	@Override
	public void setTransactionTimeout(int seconds) throws SystemException
	{
		if (seconds < 0)
		{
			throw new BitronixSystemException("cannot set a timeout to less than 0 second (was: " + seconds + "s)");
		}
		ThreadContext.getContext()
		             .setTimeout(seconds);
	}

	/**
	 * Method suspend ...
	 *
	 * @return Transaction
	 *
	 * @throws SystemException
	 * 		when
	 */
	@Override
	public Transaction suspend() throws SystemException
	{
		BitronixTransaction currentTx = getCurrentTransaction();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("suspending transaction " + currentTx);
		}
		if (currentTx == null)
		{
			return null;
		}

		try
		{
			currentTx.getResourceManager()
			         .suspend();
			clearCurrentContextForSuspension();
			inFlightTransactions.get(currentTx)
			                    .setThreadContext(null);
			return currentTx;
		}
		catch (XAException ex)
		{
			String extraErrorDetails = TransactionManagerServices.getExceptionAnalyzer()
			                                                     .extractExtraXAExceptionDetails(ex);
			throw new BitronixSystemException("cannot suspend " + currentTx + ", error=" + Decoder.decodeXAExceptionErrorCode(ex) +
			                                  (extraErrorDetails == null ? "" : ", extra error=" + extraErrorDetails), ex);
		}
	}

	/**
	 * Unlink the transaction from the current thread's context.
	 */
	private void clearCurrentContextForSuspension()
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("clearing current thread context: " + ThreadContext.getContext());
		}
		ThreadContext.getContext()
		             .clearTransaction();
	}

	/**
	 * Check if the transaction manager is in the process of shutting down.
	 *
	 * @return true if the transaction manager is in the process of shutting down.
	 */
	private boolean isShuttingDown()
	{
		return shuttingDown;
	}

	/**
	 * Dump an overview of all running transactions as debug logs.
	 */
	public void dumpTransactionContexts()
	{
		if (!LogDebugCheck.isDebugEnabled())
		{
			return;
		}

		// We're using an iterator, so we must synchronize on the collection
		synchronized (inFlightTransactions)
		{
			log.finer("dumping " + inFlightTransactions.size() + " transaction context(s)");
			for (BitronixTransaction tx : inFlightTransactions.keySet())
			{
				log.finer(tx.toString());
			}
		}
	}

	/**
	 * Get the transaction currently registered on the current thread context.
	 *
	 * @return the current transaction or null if no transaction has been started on the current thread.
	 */
	public BitronixTransaction getCurrentTransaction()
	{
		return ThreadContext.getContext()
		                    .getTransaction();
	}

	/**
	 * Create a new transaction on the current thread's context.
	 *
	 * @return the created transaction.
	 */
	private BitronixTransaction createTransaction()
	{
		BitronixTransaction transaction = new BitronixTransaction();
		ThreadContext.getContext()
		             .setTransaction(transaction);
		return transaction;
	}

	/**
	 * BitronixTransactionManager can only have a single instance per JVM so this method always returns a reference
	 * with no special information to find back the sole instance. BitronixTransactionManagerObjectFactory will be used
	 * by the JNDI server to get the BitronixTransactionManager instance of the JVM.
	 *
	 * @return an empty reference to get the BitronixTransactionManager.
	 */
	@Override
	public Reference getReference() throws NamingException
	{
		return new Reference(
				BitronixTransactionManager.class.getName(),
				new StringRefAddr("TransactionManager", "BitronixTransactionManager"),
				BitronixTransactionManagerObjectFactory.class.getName(),
				null
		);
	}

	/**
	 * Return a count of the current in-flight transactions.  Currently this method is only called by unit tests.
	 *
	 * @return a count of in-flight transactions
	 */
	public int getInFlightTransactionCount()
	{
		return inFlightTransactions.size();
	}

	/**
	 * Return the timestamp of the oldest in-flight transaction.
	 *
	 * @return the timestamp or Long.MIN_VALUE if there is no in-flight transaction.
	 */
	public long getOldestInFlightTransactionTimestamp()
	{
		try
		{
			// The inFlightTransactions map is sorted by timestamp, so the first transaction is always the oldest
			BitronixTransaction oldestTransaction = inFlightTransactions.firstKey();
			long oldestTimestamp = oldestTransaction.getResourceManager()
			                                        .getGtrid()
			                                        .extractTimestamp();

			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("oldest in-flight transaction's timestamp: " + oldestTimestamp);
			}
			return oldestTimestamp;

		}
		catch (NoSuchElementException e)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.log(Level.FINER, "oldest in-flight transaction's timestamp: " + Long.MIN_VALUE, e);
			}
			return Long.MIN_VALUE;
		}
	}

	/*
	 * Internal impl
	 */

	/**
	 * Shut down the transaction manager and release all resources held by it.
	 * <p>This call will also close the resources pools registered by the {@link bitronix.tm.resource.ResourceLoader}
	 * like JMS and JDBC pools. The manually created ones are left untouched.</p>
	 * <p>The Transaction Manager will wait during a configurable graceful period before forcibly killing active
	 * transactions.</p>
	 * After this method is called, attempts to create new transactions (via calls to
	 * {@link jakarta.transaction.TransactionManager#begin()}) will be rejected with a {@link SystemException}.
	 *
	 * @see Configuration#getGracefulShutdownInterval()
	 */
	@Override
	public synchronized void shutdown()
	{
		if (isShuttingDown())
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("Transaction Manager has already shut down");
			}
			return;
		}

		log.info("shutting down Bitronix Transaction Manager");
		internalShutdown();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("shutting down resource loader");
		}
		TransactionManagerServices.getResourceLoader()
		                          .shutdown();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("shutting down executor");
		}
		TransactionManagerServices.getExecutor()
		                          .shutdown();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("shutting down task scheduler");
		}
		TransactionManagerServices.getTaskScheduler()
		                          .shutdown();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("shutting down journal");
		}
		TransactionManagerServices.getJournal()
		                          .shutdown();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("shutting down recoverer");
		}
		TransactionManagerServices.getRecoverer()
		                          .shutdown();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("shutting down configuration");
		}
		TransactionManagerServices.getConfiguration()
		                          .shutdown();

		// clear references
		TransactionManagerServices.clear();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("shutdown ran successfully");
		}
	}

	/**
	 * Method internalShutdown ...
	 */
	private void internalShutdown()
	{
		shuttingDown = true;
		dumpTransactionContexts();

		int seconds = TransactionManagerServices.getConfiguration()
		                                        .getGracefulShutdownInterval();
		int txCount = 0;
		try
		{
			txCount = inFlightTransactions.size();
			while (seconds > 0 && txCount > 0)
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("still " + txCount + " in-flight transactions, waiting... (" + seconds + " second(s) left)");
				}
				try
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException ex)
				{
					// ignore
				}
				seconds--;
				txCount = inFlightTransactions.size();
			}
		}
		catch (Exception ex)
		{
			log.log(Level.SEVERE, "cannot get a list of in-flight transactions", ex);
		}

		if (txCount > 0)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("still " + txCount + " in-flight transactions, shutting down anyway");
				dumpTransactionContexts();
			}
		}
		else
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("all transactions finished, resuming shutdown");
			}
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
		return "a BitronixTransactionManager with " + inFlightTransactions.size() + " in-flight transaction(s)";
	}

	private final class ClearContextSynchronization
			implements Synchronization
	{
		private final BitronixTransaction currentTx;
		private final AtomicReference<ThreadContext> threadContext;

		/**
		 * Constructor ClearContextSynchronization creates a new ClearContextSynchronization instance.
		 *
		 * @param currentTx
		 * 		of type BitronixTransaction
		 * @param threadContext
		 * 		of type ThreadContext
		 */
		@SuppressWarnings("WeakerAccess")
		public ClearContextSynchronization(BitronixTransaction currentTx, ThreadContext threadContext)
		{
			this.currentTx = currentTx;
			this.threadContext = new AtomicReference<>(threadContext);
		}

		/**
		 * Method beforeCompletion ...
		 */
		@Override
		public void beforeCompletion()
		{
			//Nothing to do
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
			ThreadContext context = threadContext.get();
			if (context != null)
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("clearing transaction from thread context: " + context);
				}
				context.clearTransaction();
			}
			else
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("thread context was null when clear context synchronization executed");
				}
			}
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("removing transaction from in-flight transactions: " + currentTx);
			}
			inFlightTransactions.remove(currentTx);
		}

		/**
		 * Method setThreadContext sets the threadContext of this ClearContextSynchronization object.
		 *
		 * @param threadContext
		 * 		the threadContext of this ClearContextSynchronization object.
		 */
		@SuppressWarnings("WeakerAccess")
		public void setThreadContext(ThreadContext threadContext)
		{
			this.threadContext.set(threadContext);
		}

		/**
		 * Method toString ...
		 *
		 * @return String
		 */
		@Override
		public String toString()
		{
			return "a ClearContextSynchronization for " + currentTx;
		}
	}
}
