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

import bitronix.tm.internal.BitronixRuntimeException;
import bitronix.tm.internal.LogDebugCheck;
import bitronix.tm.utils.Scheduler;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionSynchronizationRegistry;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of JTA 1.1 {@link TransactionSynchronizationRegistry}.
 *
 * @author Ludovic Orban
 */
public class BitronixTransactionSynchronizationRegistry
		implements TransactionSynchronizationRegistry, Referenceable
{

	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(BitronixTransactionSynchronizationRegistry.class.toString());
	private static final ThreadLocal<Map<Object, Object>> resourcesTl = ThreadLocal.withInitial(HashMap::new);

	private static final String CANT_GET_TRANSACTION = "cannot get current transaction status";
	private static final String NO_TRANSACTION_ON_THREAD = "no transaction started on current thread";

	private final BitronixTransactionManager transactionManager;


	/**
	 * Constructor BitronixTransactionSynchronizationRegistry creates a new BitronixTransactionSynchronizationRegistry instance.
	 */
	public BitronixTransactionSynchronizationRegistry()
	{
		transactionManager = TransactionManagerServices.getTransactionManager();
	}

	/**
	 * Method getTransactionKey returns the transactionKey of this BitronixTransactionSynchronizationRegistry object.
	 *
	 * @return the transactionKey (type Object) of this BitronixTransactionSynchronizationRegistry object.
	 */
	@Override
	public Object getTransactionKey()
	{
		try
		{
			if (currentTransaction() == null || currentTransaction().getStatus() == Status.STATUS_NO_TRANSACTION)
			{
				return null;
			}

			return currentTransaction().getGtrid();
		}
		catch (SystemException ex)
		{
			throw new BitronixRuntimeException(CANT_GET_TRANSACTION, ex);
		}
	}

	/**
	 * Method putResource ...
	 *
	 * @param key
	 * 		of type Object
	 * @param value
	 * 		of type Object
	 */
	@Override
	public void putResource(Object key, Object value)
	{
		try
		{
			if (key == null)
			{
				throw new NullPointerException("key cannot be null");
			}
			if (currentTransaction() == null || currentTransaction().getStatus() == Status.STATUS_NO_TRANSACTION)
			{
				throw new IllegalStateException(NO_TRANSACTION_ON_THREAD);
			}

			Object oldValue = getResources().put(key, value);

			if (oldValue == null && getResources().size() == 1)
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("first resource put in synchronization registry, registering a ClearRegistryResourcesSynchronization");
				}
				Synchronization synchronization = new ClearRegistryResourcesSynchronization();
				currentTransaction().getSynchronizationScheduler()
				                    .add(synchronization, Scheduler.ALWAYS_LAST_POSITION);
			}
		}
		catch (SystemException ex)
		{
			throw new BitronixRuntimeException(CANT_GET_TRANSACTION, ex);
		}
	}

	/**
	 * Method getResource ...
	 *
	 * @param key
	 * 		of type Object
	 *
	 * @return Object
	 */
	@Override
	public Object getResource(Object key)
	{
		try
		{
			if (key == null)
			{
				throw new NullPointerException("key cannot be null");
			}
			if (currentTransaction() == null || currentTransaction().getStatus() == Status.STATUS_NO_TRANSACTION)
			{
				throw new IllegalStateException(NO_TRANSACTION_ON_THREAD);
			}

			return getResources().get(key);
		}
		catch (SystemException ex)
		{
			throw new BitronixRuntimeException(CANT_GET_TRANSACTION, ex);
		}
	}

	/**
	 * Method registerInterposedSynchronization ...
	 *
	 * @param synchronization
	 * 		of type Synchronization
	 */
	@Override
	public void registerInterposedSynchronization(Synchronization synchronization)
	{
		try
		{
			if (currentTransaction() == null || currentTransaction().getStatus() == Status.STATUS_NO_TRANSACTION)
			{
				throw new IllegalStateException(NO_TRANSACTION_ON_THREAD);
			}
			if (currentTransaction().getStatus() == Status.STATUS_PREPARING ||
			    currentTransaction().getStatus() == Status.STATUS_PREPARED ||
			    currentTransaction().getStatus() == Status.STATUS_COMMITTING ||
			    currentTransaction().getStatus() == Status.STATUS_COMMITTED ||
			    currentTransaction().getStatus() == Status.STATUS_ROLLING_BACK ||
			    currentTransaction().getStatus() == Status.STATUS_ROLLEDBACK
			)
			{
				throw new IllegalStateException("transaction is done, cannot register an interposed synchronization");
			}

			currentTransaction().getSynchronizationScheduler()
			                    .add(synchronization, Scheduler.DEFAULT_POSITION - 1);
		}
		catch (SystemException ex)
		{
			throw new BitronixRuntimeException(CANT_GET_TRANSACTION, ex);
		}
	}

	/**
	 * Method getTransactionStatus returns the transactionStatus of this BitronixTransactionSynchronizationRegistry object.
	 *
	 * @return the transactionStatus (type int) of this BitronixTransactionSynchronizationRegistry object.
	 */
	@Override
	public int getTransactionStatus()
	{
		try
		{
			if (currentTransaction() == null)
			{
				return Status.STATUS_NO_TRANSACTION;
			}

			return currentTransaction().getStatus();
		}
		catch (SystemException ex)
		{
			throw new BitronixRuntimeException(CANT_GET_TRANSACTION, ex);
		}
	}

	/**
	 * Method setRollbackOnly ...
	 */
	@Override
	public void setRollbackOnly()
	{
		try
		{
			if (currentTransaction() == null || currentTransaction().getStatus() == Status.STATUS_NO_TRANSACTION)
			{
				throw new IllegalStateException(NO_TRANSACTION_ON_THREAD);
			}

			currentTransaction().setStatus(Status.STATUS_MARKED_ROLLBACK);
		}
		catch (SystemException ex)
		{
			throw new BitronixRuntimeException("cannot get or set current transaction status", ex);
		}
	}

	/**
	 * Method getRollbackOnly returns the rollbackOnly of this BitronixTransactionSynchronizationRegistry object.
	 *
	 * @return the rollbackOnly (type boolean) of this BitronixTransactionSynchronizationRegistry object.
	 */
	@Override
	public boolean getRollbackOnly()
	{
		try
		{
			if (currentTransaction() == null || currentTransaction().getStatus() == Status.STATUS_NO_TRANSACTION)
			{
				throw new IllegalStateException(NO_TRANSACTION_ON_THREAD);
			}

			return currentTransaction().getStatus() == Status.STATUS_MARKED_ROLLBACK;
		}
		catch (SystemException e)
		{
			throw new BitronixRuntimeException(CANT_GET_TRANSACTION, e);
		}
	}

	/**
	 * Method getResources returns the resources of this BitronixTransactionSynchronizationRegistry object.
	 *
	 * @return the resources (type Map<Object, Object>) of this BitronixTransactionSynchronizationRegistry object.
	 */
	private Map<Object, Object> getResources()
	{
		return resourcesTl.get();
	}

	/**
	 * Method currentTransaction ...
	 *
	 * @return BitronixTransaction
	 */
	private BitronixTransaction currentTransaction()
	{
		return transactionManager.getCurrentTransaction();
	}

	/**
	 * Method getReference returns the reference of this BitronixTransactionSynchronizationRegistry object.
	 *
	 * @return the reference (type Reference) of this BitronixTransactionSynchronizationRegistry object.
	 *
	 * @throws NamingException
	 * 		when
	 */
	@Override
	public Reference getReference() throws NamingException
	{
		return new Reference(
				BitronixTransactionManager.class.getName(),
				new StringRefAddr("TransactionSynchronizationRegistry", "BitronixTransactionSynchronizationRegistry"),
				BitronixTransactionSynchronizationRegistryObjectFactory.class.getName(),
				null
		);
	}

	private final class ClearRegistryResourcesSynchronization
			implements Synchronization
	{
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
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("clearing resources");
			}
			getResources().clear();
		}
	}

}
