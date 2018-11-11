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
package bitronix.tm.resource;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.LogDebugCheck;
import bitronix.tm.recovery.IncrementalRecoverer;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.resource.common.XAResourceProducer;

import javax.transaction.xa.XAResource;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;

import static java.nio.charset.StandardCharsets.*;

/**
 * Collection of initialized {@link XAResourceProducer}s. All resources must be registered in the {@link ResourceRegistrar}
 * before they can be used by the transaction manager.
 * <p>
 * Note: The implementation is based on a thread safe, read-optimized list (copy-on-write) assuming that the
 * number of registered resources is around 1 to 16 entries and does not change often. If this assumption is
 * not-true it may be required to re-implement this with a ConcurrentMap instead.
 *
 * @author Ludovic Orban
 * @author Juergen Kellerer
 */
public final class ResourceRegistrar
{

	/**
	 * Specifies the charset that unique names of resources must be encodable with to be storeable in a TX journal.
	 */
	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(ResourceRegistrar.class.toString());
	private static final Set<ProducerHolder> resources = new CopyOnWriteArraySet<>();

	/**
	 * Constructor ResourceRegistrar creates a new ResourceRegistrar instance.
	 */
	private ResourceRegistrar()
	{
		//No config required
	}

	/**
	 * Get a registered {@link XAResourceProducer}.
	 *
	 * @param uniqueName
	 * 		the name of the recoverable resource producer.
	 *
	 * @return the {@link XAResourceProducer} or null if there was none registered under that name.
	 */
	public static XAResourceProducer get(String uniqueName)
	{
		if (uniqueName != null)
		{
			for (ProducerHolder holder : resources)
			{
				if (!holder.isInitialized())
				{
					continue;
				}
				if (uniqueName.equals(holder.getUniqueName()))
				{
					return holder.producer;
				}
			}
		}
		return null;
	}

	/**
	 * Get all {@link XAResourceProducer}s unique names.
	 *
	 * @return a Set containing all {@link bitronix.tm.resource.common.XAResourceProducer}s unique names.
	 */
	public static Set<String> getResourcesUniqueNames()
	{
		Set<String> names = new HashSet<>(resources.size());
		for (ProducerHolder holder : resources)
		{
			if (!holder.isInitialized())
			{
				continue;
			}
			names.add(holder.getUniqueName());
		}

		return Collections.unmodifiableSet(names);
	}

	/**
	 * Register a {@link XAResourceProducer}. If registration happens after the transaction manager started, incremental
	 * recovery is run on that resource.
	 *
	 * @param producer
	 * 		the {@link XAResourceProducer}.
	 *
	 * @throws RecoveryException
	 * 		When an error happens during recovery.
	 */
	public static void register(XAResourceProducer producer) throws RecoveryException
	{
		try
		{
			boolean alreadyRunning = TransactionManagerServices.isTransactionManagerRunning();
			ProducerHolder holder = alreadyRunning ? new InitializableProducerHolder(producer) : new ProducerHolder(producer);
			if (resources.add(holder))
			{
				if (holder instanceof InitializableProducerHolder)
				{
					processInitializableProducerHolder(producer, (InitializableProducerHolder) holder);
				}
			}
			else
			{
				throw new IllegalStateException("A resource with uniqueName '" + holder.getUniqueName() + "' has already been registered. " +
				                                "Cannot register XAResourceProducer '" + producer + "'.");
			}
		}
		catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException("Cannot register the XAResourceProducer '" + producer + "' caused by invalid input.", e);
		}
	}

	/**
	 * Processes a type cast instantiable  producer holder
	 *
	 * @param producer
	 * @param holder
	 *
	 * @return
	 *
	 * @throws RecoveryException
	 */
	private static boolean processInitializableProducerHolder(XAResourceProducer producer, InitializableProducerHolder holder) throws RecoveryException
	{
		boolean recovered = false;
		try
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("Transaction manager is running, recovering resource '" + holder.getUniqueName() + "'.");
			}
			IncrementalRecoverer.recover(producer);
			holder.initialize();
			recovered = true;
		}
		finally
		{
			if (!recovered)
			{
				resources.remove(holder);
			}
		}
		return recovered;
	}

	/**
	 * Unregister a previously registered {@link XAResourceProducer}.
	 *
	 * @param producer
	 * 		the {@link XAResourceProducer}.
	 */
	public static void unregister(XAResourceProducer producer)
	{
		ProducerHolder holder = new ProducerHolder(producer);
		if (!resources.remove(holder) && LogDebugCheck.isDebugEnabled())
		{
			log.log(Level.FINER, "resource with uniqueName '" + holder.getUniqueName() + "' has not been registered");
		}
	}

	/**
	 * Find in the registered {@link XAResourceProducer}s the {@link XAResourceHolder} from which the specified {@link XAResource} comes from.
	 *
	 * @param xaResource
	 * 		the {@link XAResource} to look for
	 *
	 * @return the associated {@link XAResourceHolder} or null if it cannot be found.
	 */
	public static XAResourceHolder findXAResourceHolder(XAResource xaResource)
	{
		boolean debug = LogDebugCheck.isDebugEnabled();

		for (ProducerHolder holder : resources)
		{
			if (!holder.isInitialized())
			{
				continue;
			}

			XAResourceProducer producer = holder.producer;
			XAResourceHolder resourceHolder = producer.findXAResourceHolder(xaResource);
			if (resourceHolder != null)
			{
				if (debug)
				{
					log.finer("XAResource " + xaResource + " belongs to " + resourceHolder + " that itself belongs to " + producer);
				}
				return resourceHolder;
			}
			if (debug)
			{
				log.finer("XAResource " + xaResource + " does not belong to any resource of " + producer);
			}
		}

		return null;
	}

	/**
	 * Implements a holder that maintains XAResourceProducers in a set only differentiating them by their unique names.
	 */
	private static class ProducerHolder
	{

		private final XAResourceProducer producer;

		/**
		 * Constructor ProducerHolder creates a new ProducerHolder instance.
		 *
		 * @param producer
		 * 		of type XAResourceProducer
		 */
		private ProducerHolder(XAResourceProducer producer)
		{
			if (producer == null)
			{
				throw new IllegalArgumentException("XAResourceProducer may not be 'null'. Verify your call to ResourceRegistrar.[un]register(...).");
			}

			String uniqueName = producer.getUniqueName();
			if (uniqueName == null || uniqueName.length() == 0)
			{
				throw new IllegalArgumentException("The given XAResourceProducer '" + producer + "' does not specify a uniqueName.");
			}

			String transcodedUniqueName = new String(uniqueName.getBytes(US_ASCII), US_ASCII);
			if (!transcodedUniqueName.equals(uniqueName))
			{
				throw new IllegalArgumentException("The given XAResourceProducer's uniqueName '" + uniqueName + "' is not compatible with the charset " +
				                                   "'US-ASCII' (transcoding results in '" + transcodedUniqueName + "'). " + System.getProperty("line.separator") +
				                                   "BTM requires unique names to be compatible with US-ASCII when used with a transaction journal.");
			}

			this.producer = producer;
		}

		/**
		 * Method hashCode ...
		 *
		 * @return int
		 */
		@Override
		public int hashCode()
		{
			return getUniqueName().hashCode();
		}

		/**
		 * Method equals ...
		 *
		 * @param o
		 * 		of type Object
		 *
		 * @return boolean
		 */
		@Override
		public boolean equals(Object o)
		{
			if (this == o)
			{
				return true;
			}
			if (!(o instanceof ProducerHolder))
			{
				return false;
			}
			ProducerHolder that = (ProducerHolder) o;
			return getUniqueName().equals(that.getUniqueName());
		}

		/**
		 * Method toString ...
		 *
		 * @return String
		 */
		@Override
		public String toString()
		{
			return "ProducerHolder{" +
			       "producer=" + producer +
			       ", initialized=" + isInitialized() +
			       '}';
		}

		/**
		 * Method isInitialized returns the initialized of this ProducerHolder object.
		 *
		 * @return the initialized (type boolean) of this ProducerHolder object.
		 */
		boolean isInitialized()
		{
			return true;
		}

		/**
		 * Method getUniqueName returns the uniqueName of this ProducerHolder object.
		 *
		 * @return the uniqueName (type String) of this ProducerHolder object.
		 */
		String getUniqueName()
		{
			return producer.getUniqueName();
		}
	}

	/**
	 * Extends the default holder with thread safe initialization to put uninitialized holders in the set.
	 */
	private static class InitializableProducerHolder
			extends ProducerHolder
	{

		private volatile boolean initialized;

		/**
		 * Constructor InitializableProducerHolder creates a new InitializableProducerHolder instance.
		 *
		 * @param producer
		 * 		of type XAResourceProducer
		 */
		private InitializableProducerHolder(XAResourceProducer producer)
		{
			super(producer);
		}

		/**
		 * Method initialize ...
		 */
		void initialize()
		{
			initialized = true;
		}

		@Override
		public int hashCode()
		{
			return super.hashCode();
		}

		@Override
		public boolean equals(Object o)
		{
			return super.equals(o);
		}

		/**
		 * Method isInitialized returns the initialized of this InitializableProducerHolder object.
		 *
		 * @return the initialized (type boolean) of this InitializableProducerHolder object.
		 */
		@Override
		boolean isInitialized()
		{
			return initialized;
		}
	}
}
