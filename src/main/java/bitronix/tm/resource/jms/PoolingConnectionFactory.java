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
package bitronix.tm.resource.jms;

import bitronix.tm.internal.LogDebugCheck;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.ResourceConfigurationException;
import bitronix.tm.resource.ResourceObjectFactory;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.RecoveryXAResourceHolder;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.XAPool;
import bitronix.tm.resource.common.XAResourceProducer;
import bitronix.tm.utils.ManagementRegistrar;

import javax.jms.*;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.transaction.xa.XAResource;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation of a JMS {@link ConnectionFactory} wrapping vendor's {@link XAConnectionFactory} implementation.
 *
 * @author Ludovic Orban
 */
@SuppressWarnings({"serial", "unused"})
public class PoolingConnectionFactory
		extends ResourceBean
		implements ConnectionFactory, XAResourceProducer<DualSessionWrapper, JmsPooledConnection>,
				           PoolingConnectionFactoryMBean
{

	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(PoolingConnectionFactory.class.toString());
	private final transient List<JmsPooledConnection> xaStatefulHolders;
	private transient volatile XAPool<DualSessionWrapper, JmsPooledConnection> pool;
	private transient volatile XAConnectionFactory xaConnectionFactory;
	private transient volatile JmsPooledConnection recoveryPooledConnection;
	private transient volatile RecoveryXAResourceHolder recoveryXAResourceHolder;
	private volatile boolean cacheProducersConsumers = true;
	private volatile boolean testConnections = false;
	private volatile String user;
	private volatile String password;
	private transient volatile JmsConnectionHandle recoveryConnectionHandle;
	private volatile String jmxName;

	/**
	 * Initialize all properties with their default values.
	 */
	public PoolingConnectionFactory()
	{
		xaStatefulHolders = new CopyOnWriteArrayList<>();
	}

	/**
	 * Method getCacheProducersConsumers returns the cacheProducersConsumers of this PoolingConnectionFactory object.
	 *
	 * @return the cacheProducersConsumers (type boolean) of this PoolingConnectionFactory object.
	 */
	public boolean getCacheProducersConsumers()
	{
		return cacheProducersConsumers;
	}

	/**
	 * Method setCacheProducersConsumers sets the cacheProducersConsumers of this PoolingConnectionFactory object.
	 *
	 * @param cacheProducersConsumers
	 * 		the cacheProducersConsumers of this PoolingConnectionFactory object.
	 */
	public void setCacheProducersConsumers(boolean cacheProducersConsumers)
	{
		this.cacheProducersConsumers = cacheProducersConsumers;
	}

	/**
	 * Method getTestConnections returns the testConnections of this PoolingConnectionFactory object.
	 *
	 * @return the testConnections (type boolean) of this PoolingConnectionFactory object.
	 */
	public boolean getTestConnections()
	{
		return testConnections;
	}

	/**
	 * Method setTestConnections sets the testConnections of this PoolingConnectionFactory object.
	 *
	 * @param testConnections
	 * 		the testConnections of this PoolingConnectionFactory object.
	 */
	public void setTestConnections(boolean testConnections)
	{
		this.testConnections = testConnections;
	}

	/**
	 * Method getUser returns the user of this PoolingConnectionFactory object.
	 *
	 * @return the user (type String) of this PoolingConnectionFactory object.
	 */
	public String getUser()
	{
		return user;
	}

	/**
	 * Method setUser sets the user of this PoolingConnectionFactory object.
	 *
	 * @param user
	 * 		the user of this PoolingConnectionFactory object.
	 */
	public void setUser(String user)
	{
		this.user = user;
	}

	/**
	 * Method getPassword returns the password of this PoolingConnectionFactory object.
	 *
	 * @return the password (type String) of this PoolingConnectionFactory object.
	 */
	public String getPassword()
	{
		return password;
	}

	/**
	 * Method setPassword sets the password of this PoolingConnectionFactory object.
	 *
	 * @param password
	 * 		the password of this PoolingConnectionFactory object.
	 */
	public void setPassword(String password)
	{
		this.password = password;
	}

	/**
	 * @return the wrapped XAConnectionFactory.
	 */
	public XAConnectionFactory getXaConnectionFactory()
	{
		return xaConnectionFactory;
	}

	/**
	 * Inject a pre-configured XAConnectionFactory instead of relying on className and driverProperties
	 * to build one. Upon deserialization the xaConnectionFactory will be null and will need to be
	 * manually re-injected.
	 *
	 * @param xaConnectionFactory
	 * 		the pre-configured XAConnectionFactory.
	 */
	public void setXaConnectionFactory(XAConnectionFactory xaConnectionFactory)
	{
		this.xaConnectionFactory = xaConnectionFactory;
	}

	/**
	 * Method unregister ...
	 *
	 * @param jmsPooledConnection
	 * 		of type JmsPooledConnection
	 */
	void unregister(JmsPooledConnection jmsPooledConnection)
	{
		xaStatefulHolders.remove(jmsPooledConnection);
	}

	/**
	 * Method toString ...
	 *
	 * @return String
	 */
	@Override
	public String toString()
	{
		return "a PoolingConnectionFactory with " + pool;
	}

	/**
	 * Prepare the recoverable {@link javax.transaction.xa.XAResource} producer for recovery.
	 *
	 * @return a {@link bitronix.tm.internal.XAResourceHolderState} object that can be used to call <code>recover()</code>.
	 *
	 * @throws bitronix.tm.recovery.RecoveryException
	 * 		thrown when a {@link bitronix.tm.internal.XAResourceHolderState} cannot be acquired.
	 */
	@Override
	public XAResourceHolderState startRecovery() throws RecoveryException
	{
		init();
		if (recoveryPooledConnection != null)
		{
			throw new RecoveryException("recovery already in progress on " + this);
		}

		try
		{
			recoveryConnectionHandle = (JmsConnectionHandle) pool.getConnectionHandle(false);
			recoveryPooledConnection = recoveryConnectionHandle.getPooledConnection();
			recoveryXAResourceHolder = recoveryPooledConnection.createRecoveryXAResourceHolder();
			return new XAResourceHolderState(recoveryXAResourceHolder, recoveryPooledConnection.getPoolingConnectionFactory());
		}
		catch (Exception ex)
		{
			throw new RecoveryException("error starting recovery", ex);
		}
	}

	/**
	 * Release internal resources held after call to <code>startRecovery()</code>.
	 *
	 * @throws bitronix.tm.recovery.RecoveryException
	 * 		thrown when an error occurred while releasing reserved resources.
	 */
	@Override
	public void endRecovery() throws RecoveryException
	{
		if (recoveryPooledConnection == null)
		{
			return;
		}

		try
		{
			if (recoveryConnectionHandle != null)
			{
				try
				{
					if (LogDebugCheck.isDebugEnabled())
					{
						log.finer("recovery connection handle is being closed: " + recoveryConnectionHandle);
					}
					recoveryConnectionHandle.close();
				}
				catch (Exception ex)
				{
					throw new RecoveryException("error ending recovery", ex);
				}
			}

			if (recoveryXAResourceHolder != null)
			{
				try
				{
					if (LogDebugCheck.isDebugEnabled())
					{
						log.finer("recovery xa resource is being closed: " + recoveryXAResourceHolder);
					}
					recoveryXAResourceHolder.close();
				}
				catch (Exception ex)
				{
					throw new RecoveryException("error ending recovery", ex);
				}
			}
		}
		finally
		{
			recoveryConnectionHandle = null;
			recoveryXAResourceHolder = null;
			recoveryPooledConnection = null;
		}
	}

	/**
	 * Method buildXAPool ...
	 *
	 * @throws Exception
	 * 		when
	 */
	private void buildXAPool() throws Exception
	{
		if (pool != null)
		{
			return;
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("building JMS XA pool for " + getUniqueName() + " with " + getMinPoolSize() + " connection(s)");
		}
		pool = new XAPool<>(this, this, xaConnectionFactory);
		boolean builtXaFactory = false;
		if (this.xaConnectionFactory == null)
		{
			this.xaConnectionFactory = (XAConnectionFactory) pool.getXAFactory();
			builtXaFactory = true;
		}
		try
		{
			ResourceRegistrar.register(this);
		}
		catch (RecoveryException ex)
		{
			if (builtXaFactory)
			{
				xaConnectionFactory = null;
			}
			pool = null;
			throw ex;
		}
	}

	/**
	 * Method getInPoolSize returns the inPoolSize of this PoolingConnectionFactory object.
	 *
	 * @return the inPoolSize (type long) of this PoolingConnectionFactory object.
	 */
	@Override
	public long getInPoolSize()
	{
		return pool.inPoolSize();
	}

	/**
	 * Method getTotalPoolSize returns the totalPoolSize of this PoolingConnectionFactory object.
	 *
	 * @return the totalPoolSize (type long) of this PoolingConnectionFactory object.
	 */
	@Override
	public long getTotalPoolSize()
	{
		return pool.totalPoolSize();
	}

	/**
	 * Method isFailed returns the failed of this PoolingConnectionFactory object.
	 *
	 * @return the failed (type boolean) of this PoolingConnectionFactory object.
	 */
	@Override
	public boolean isFailed()
	{
		return pool.isFailed();
	}

	/**
	 * Mark this resource producer as failed or not. A resource is considered failed if recovery fails to run on it.
	 *
	 * @param failed
	 * 		true is the resource must be considered failed, false it it must be considered sane.
	 */
	@Override
	public void setFailed(boolean failed)
	{
		pool.setFailed(failed);
	}

	/**
	 * Find in the {@link bitronix.tm.resource.common.XAResourceHolder}s created by this {@link bitronix.tm.resource.common.XAResourceProducer} the one which this
	 * {@link javax.transaction.xa.XAResource} belongs to.
	 *
	 * @param xaResource
	 * 		the {@link javax.transaction.xa.XAResource} to look for.
	 *
	 * @return the associated {@link bitronix.tm.resource.common.XAResourceHolder} or null if the {@link javax.transaction.xa.XAResource} does not belong to this
	 * 		{@link bitronix.tm.resource.common.XAResourceProducer}.
	 */
	@Override
	public DualSessionWrapper findXAResourceHolder(XAResource xaResource)
	{
		synchronized (xaStatefulHolders)
		{
			for (JmsPooledConnection jmsPooledConnection : xaStatefulHolders)
			{
				DualSessionWrapper xaResourceHolder = jmsPooledConnection.getXAResourceHolderForXaResource(xaResource);
				if (xaResourceHolder != null)
				{
					return xaResourceHolder;
				}
			}
			return null;
		}
	}

	/**
	 * Initialize the pool by creating the initial amount of connections.
	 */
	@Override
	public synchronized void init()
	{
		try
		{
			if (pool != null)
			{
				return;
			}

			buildXAPool();
			this.jmxName = "bitronix.tm:type=JMS,UniqueName=" + ManagementRegistrar.makeValidName(getUniqueName());
			ManagementRegistrar.register(jmxName, this);
		}
		catch (Exception ex)
		{
			throw new ResourceConfigurationException("cannot create JMS connection factory named " + getUniqueName(), ex);
		}
	}

	/**
	 * Release this {@link bitronix.tm.resource.common.XAResourceProducer}'s internal resources.
	 */
	@Override
	public void close()
	{
		if (pool == null)
		{
			return;
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("closing " + pool);
		}
		pool.close();
		pool = null;

		ManagementRegistrar.unregister(jmxName);
		jmxName = null;

		ResourceRegistrar.unregister(this);
	}

	/**
	 * Create a {@link bitronix.tm.resource.common.XAStatefulHolder} that will be placed in an {@link bitronix.tm.resource.common.XAPool}.
	 *
	 * @param xaFactory
	 * 		the vendor's resource-specific XA factory.
	 * @param bean
	 * 		the resource-specific bean describing the resource parameters.
	 *
	 * @return a {@link bitronix.tm.resource.common.XAStatefulHolder} that will be placed in an {@link bitronix.tm.resource.common.XAPool}.
	 *
	 * @throws Exception
	 * 		thrown when the {@link bitronix.tm.resource.common.XAStatefulHolder} cannot be created.
	 */
	@Override
	public JmsPooledConnection createPooledConnection(Object xaFactory, ResourceBean bean) throws Exception
	{
		if (!(xaFactory instanceof XAConnectionFactory))
		{
			throw new IllegalArgumentException("class '" + xaFactory.getClass()
			                                                        .getName() + "' does not implement " + XAConnectionFactory.class.getName());
		}
		XAConnectionFactory innerXaConnectionFactory = (XAConnectionFactory) xaFactory;

		XAConnection xaConnection;
		if (user == null || password == null)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("creating new JMS XAConnection with no credentials");
			}
			xaConnection = innerXaConnectionFactory.createXAConnection();
		}
		else
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("creating new JMS XAConnection with user <" + user + "> and password <" + password + ">");
			}
			xaConnection = innerXaConnectionFactory.createXAConnection(user, password);
		}

		JmsPooledConnection jmsPooledConnection = new JmsPooledConnection(this, xaConnection);
		xaStatefulHolders.add(jmsPooledConnection);
		return jmsPooledConnection;
	}

	/**
	 * Method reset ...
	 *
	 * @throws Exception
	 * 		when
	 */
	@Override
	public void reset() throws Exception
	{
		pool.reset();
	}

	/**
	 * {@link PoolingConnectionFactory} must alway have a unique name so this method builds a reference to this object
	 * using the unique name as {@link javax.naming.RefAddr}.
	 *
	 * @return a reference to this {@link PoolingConnectionFactory}.
	 */
	@Override
	public Reference getReference() throws NamingException
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("creating new JNDI reference of " + this);
		}
		return new Reference(
				PoolingConnectionFactory.class.getName(),
				new StringRefAddr("uniqueName", getUniqueName()),
				ResourceObjectFactory.class.getName(),
				null);
	}

	/**
	 * Method createConnection ...
	 *
	 * @return Connection
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public Connection createConnection() throws JMSException
	{
		if (isDisabled())
		{
			throw new JMSException("JMS connection pool '" + getUniqueName() + "' is disabled, cannot get a connection from it");
		}

		try
		{
			init();
			return (Connection) pool.getConnectionHandle();
		}
		catch (Exception ex)
		{
			throw (JMSException) new JMSException("unable to get a connection from pool of " + this).initCause(ex);
		}
	}

	/**
	 * Method createConnection ...
	 *
	 * @param userName
	 * 		of type String
	 * @param password
	 * 		of type String
	 *
	 * @return Connection
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public Connection createConnection(String userName, String password) throws JMSException
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("JMS connections are pooled, username and password ignored");
		}
		return createConnection();
	}






	/* XAResourceProducer implementation */















	/* Referenceable implementation */



	/* management */


}
