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

import bitronix.tm.internal.BitronixSystemException;
import bitronix.tm.internal.LogDebugCheck;
import bitronix.tm.resource.common.AbstractXAStatefulHolder;
import bitronix.tm.resource.common.RecoveryXAResourceHolder;
import bitronix.tm.resource.common.StateChangeListener;
import bitronix.tm.resource.common.TransactionContextHelper;
import bitronix.tm.resource.jms.lrc.LrcXAConnectionFactory;
import bitronix.tm.utils.ManagementRegistrar;
import bitronix.tm.utils.MonotonicClock;
import bitronix.tm.utils.Scheduler;

import javax.jms.*;
import javax.transaction.xa.XAResource;
import java.util.*;
import java.util.logging.Level;

/**
 * Implementation of a JMS pooled connection wrapping vendor's {@link XAConnection} implementation.
 *
 * @author Ludovic Orban
 */
@SuppressWarnings("unused")
public class JmsPooledConnection
		extends AbstractXAStatefulHolder<JmsPooledConnection>
		implements JmsPooledConnectionMBean
{

	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(JmsPooledConnection.class.toString());
	private static final String EMULATE = "emulating XA for resource ";

	private final PoolingConnectionFactory poolingConnectionFactory;
	private final Set<DualSessionWrapper> sessions = Collections.synchronizedSet(new HashSet<>());
	/* management */
	private final String jmxName;
	private volatile XAConnection xaConnection;
	private volatile Date acquisitionDate;
	private volatile Date lastReleaseDate;

	/**
	 * Constructor JmsPooledConnection creates a new JmsPooledConnection instance.
	 *
	 * @param poolingConnectionFactory
	 * 		of type PoolingConnectionFactory
	 * @param connection
	 * 		of type XAConnection
	 */
	protected JmsPooledConnection(PoolingConnectionFactory poolingConnectionFactory, XAConnection connection)
	{
		this.poolingConnectionFactory = poolingConnectionFactory;
		this.xaConnection = connection;
		this.lastReleaseDate = new Date(MonotonicClock.currentTimeMillis());
		addStateChangeEventListener(new JmsPooledConnectionStateChangeListener());

		if (LrcXAConnectionFactory.class.getName()
		                                .equals(poolingConnectionFactory.getClassName()))
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer(EMULATE + poolingConnectionFactory.getUniqueName() + " - changing twoPcOrderingPosition to ALWAYS_LAST_POSITION");
			}
			poolingConnectionFactory.setTwoPcOrderingPosition(Scheduler.ALWAYS_LAST_POSITION);
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer(EMULATE + poolingConnectionFactory.getUniqueName() + " - changing deferConnectionRelease to true");
			}
			poolingConnectionFactory.setDeferConnectionRelease(true);
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer(EMULATE + poolingConnectionFactory.getUniqueName() + " - changing useTmJoin to true");
			}
			poolingConnectionFactory.setUseTmJoin(true);
		}

		this.jmxName = "bitronix.tm:type=JMS,UniqueName=" + ManagementRegistrar.makeValidName(poolingConnectionFactory.getUniqueName()) + ",Id=" +
		               poolingConnectionFactory.incCreatedResourcesCounter();
		ManagementRegistrar.register(jmxName, this);
	}

	/**
	 * Method getXAConnection returns the XAConnection of this JmsPooledConnection object.
	 *
	 * @return the XAConnection (type XAConnection) of this JmsPooledConnection object.
	 */
	public XAConnection getXAConnection()
	{
		return xaConnection;
	}

	/**
	 * Method getPoolingConnectionFactory returns the poolingConnectionFactory of this JmsPooledConnection object.
	 *
	 * @return the poolingConnectionFactory (type PoolingConnectionFactory) of this JmsPooledConnection object.
	 */
	public PoolingConnectionFactory getPoolingConnectionFactory()
	{
		return poolingConnectionFactory;
	}

	/**
	 * Method createRecoveryXAResourceHolder ...
	 *
	 * @return RecoveryXAResourceHolder
	 *
	 * @throws JMSException
	 * 		when
	 */
	public synchronized RecoveryXAResourceHolder createRecoveryXAResourceHolder() throws JMSException
	{
		DualSessionWrapper dualSessionWrapper = new DualSessionWrapper(this, false, 0);
		dualSessionWrapper.getSession(true); // force creation of XASession to allow access to XAResource
		return new RecoveryXAResourceHolder(dualSessionWrapper);
	}

	/**
	 * Get the list of {@link bitronix.tm.resource.common.XAResourceHolder}s created by this
	 * {@link bitronix.tm.resource.common.XAStatefulHolder} that are still open.
	 * <p>This method is thread-safe.</p>
	 *
	 * @return the list of {@link bitronix.tm.resource.common.XAResourceHolder}s created by this
	 * 		{@link bitronix.tm.resource.common.XAStatefulHolder} that are still open.
	 */
	@Override
	public List<DualSessionWrapper> getXAResourceHolders()
	{
		synchronized (sessions)
		{
			return new ArrayList<>(sessions);
		}
	}

	/**
	 * Create a disposable handler used to drive a pooled instance of
	 * {@link bitronix.tm.resource.common.XAStatefulHolder}.
	 * <p>This method is thread-safe.</p>
	 *
	 * @return a resource-specific disposable connection object.
	 *
	 * @throws Exception
	 * 		a resource-specific exception thrown when the disposable connection cannot be created.
	 */
	@Override
	public Object getConnectionHandle() throws Exception
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("getting connection handle from " + this);
		}
		State oldState = getState();

		setState(State.ACCESSIBLE);

		if (oldState == State.IN_POOL)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("connection " + xaConnection + " was in state IN_POOL, testing it");
			}
			testXAConnection();
		}
		else
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("connection " + xaConnection + " was in state " + oldState + ", no need to test it");
			}
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("got connection handle from " + this);
		}
		return new JmsConnectionHandle(this, xaConnection);
	}

	/**
	 * Close the physical connection that this {@link bitronix.tm.resource.common.XAStatefulHolder} represents.
	 *
	 * @throws Exception
	 * 		a resource-specific exception thrown when there is an error closing the physical connection.
	 */
	@Override
	public synchronized void close() throws JMSException
	{
		if (xaConnection != null)
		{
			poolingConnectionFactory.unregister(this);
			setState(State.CLOSED);
			try
			{
				xaConnection.close();
			}
			finally
			{
				xaConnection = null;
			}
		}
	}

	/**
	 * Get the date at which this object was last released to the pool. This is required to check if it is eligible
	 * for discard when the containing pool needs to shrink.
	 *
	 * @return the date at which this object was last released to the pool or null if it never left the pool.
	 */
	@Override
	public Date getLastReleaseDate()
	{
		return lastReleaseDate;
	}

	/**
	 * Method testXAConnection ...
	 *
	 * @throws JMSException
	 * 		when
	 */
	private void testXAConnection() throws JMSException
	{
		if (!poolingConnectionFactory.getTestConnections())
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("not testing connection of " + this);
			}
			return;
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("testing connection of " + this);
		}
		XASession xaSession = xaConnection.createXASession();
		try
		{
			TemporaryQueue tq = xaSession.createTemporaryQueue();
			tq.delete();
		}
		finally
		{
			xaSession.close();
		}
	}

	/**
	 * Method release ...
	 *
	 * @throws JMSException
	 * 		when
	 */
	protected void release() throws JMSException
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("releasing to pool " + this);
		}
		closePendingSessions();

		// requeuing
		try
		{
			TransactionContextHelper.requeue(this, poolingConnectionFactory);
		}
		catch (BitronixSystemException ex)
		{
			throw (JMSException) new JMSException("error requeueing " + this).initCause(ex);
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("released to pool " + this);
		}
	}

	/**
	 * Method closePendingSessions ...
	 */
	private void closePendingSessions()
	{
		synchronized (sessions)
		{
			for (DualSessionWrapper dualSessionWrapper : sessions)
			{
				if (dualSessionWrapper.getState() != State.ACCESSIBLE)
				{
					continue;
				}

				try
				{
					if (LogDebugCheck.isDebugEnabled())
					{
						log.finer("trying to close pending session " + dualSessionWrapper);
					}
					dualSessionWrapper.close();
				}
				catch (JMSException ex)
				{
					log.log(Level.WARNING, "error closing pending session " + dualSessionWrapper, ex);
				}
			}
		}
	}

	/**
	 * Method createSession ...
	 *
	 * @param transacted
	 * 		of type boolean
	 * @param acknowledgeMode
	 * 		of type int
	 *
	 * @return Session
	 *
	 * @throws JMSException
	 * 		when
	 */
	protected Session createSession(boolean transacted, int acknowledgeMode)
	{
		synchronized (sessions)
		{
			DualSessionWrapper sessionHandle = getNotAccessibleSession();

			if (sessionHandle == null)
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("no session handle found in NOT_ACCESSIBLE state, creating new session");
				}
				sessionHandle = new DualSessionWrapper(this, transacted, acknowledgeMode);
				sessionHandle.addStateChangeEventListener(new JmsConnectionHandleStateChangeListener());
				sessions.add(sessionHandle);
			}
			else
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("found session handle in NOT_ACCESSIBLE state, recycling it: " + sessionHandle);
				}
				sessionHandle.setState(State.ACCESSIBLE);
			}

			return sessionHandle;
		}
	}

	/**
	 * Method getNotAccessibleSession returns the notAccessibleSession of this JmsPooledConnection object.
	 *
	 * @return the notAccessibleSession (type DualSessionWrapper) of this JmsPooledConnection object.
	 */
	private DualSessionWrapper getNotAccessibleSession()
	{
		synchronized (sessions)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer(sessions.size() + " session(s) open from " + this);
			}
			for (DualSessionWrapper sessionHandle : sessions)
			{
				if (sessionHandle.getState() == State.NOT_ACCESSIBLE)
				{
					return sessionHandle;
				}
			}
			return null;
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
		return "a JmsPooledConnection of pool " + poolingConnectionFactory.getUniqueName() + " in state " +
		       getState() + " with underlying connection " + xaConnection;
	}

	/* management */

	/**
	 * Method getStateDescription returns the stateDescription of this JmsPooledConnection object.
	 *
	 * @return the stateDescription (type String) of this JmsPooledConnection object.
	 */
	@Override
	public String getStateDescription()
	{
		return getState().toString();
	}

	/**
	 * Method getAcquisitionDate returns the acquisitionDate of this JmsPooledConnection object.
	 *
	 * @return the acquisitionDate (type Date) of this JmsPooledConnection object.
	 */
	@Override
	public Date getAcquisitionDate()
	{
		return acquisitionDate;
	}

	/**
	 * Method getTransactionGtridsCurrentlyHoldingThis returns the transactionGtridsCurrentlyHoldingThis of this JmsPooledConnection object.
	 *
	 * @return the transactionGtridsCurrentlyHoldingThis (type Collection<String>) of this JmsPooledConnection object.
	 */
	@Override
	public Collection<String> getTransactionGtridsCurrentlyHoldingThis()
	{
		synchronized (sessions)
		{
			Set<String> result = new HashSet<>();
			for (DualSessionWrapper dsw : sessions)
			{
				result.addAll(dsw.getXAResourceHolderStateGtrids());
			}
			return result;
		}
	}

	/**
	 * Method getXAResourceHolderForXaResource ...
	 *
	 * @param xaResource
	 * 		of type XAResource
	 *
	 * @return DualSessionWrapper
	 */
	public DualSessionWrapper getXAResourceHolderForXaResource(XAResource xaResource)
	{
		synchronized (sessions)
		{
			for (DualSessionWrapper xaResourceHolder : sessions)
			{
				if (xaResourceHolder.getXAResource() == xaResource)
				{
					return xaResourceHolder;
				}
			}
			return null;
		}
	}

	/**
	 * {@link JmsPooledConnection} {@link bitronix.tm.resource.common.StateChangeListener}.
	 * When state changes to State.CLOSED, the connection is unregistered from
	 * {@link bitronix.tm.utils.ManagementRegistrar}.
	 */
	private final class JmsPooledConnectionStateChangeListener
			implements StateChangeListener<JmsPooledConnection>
	{
		/**
		 * Method stateChanged ...
		 *
		 * @param source
		 * 		of type JmsPooledConnection
		 * @param oldState
		 * 		of type State
		 * @param newState
		 * 		of type State
		 */
		@Override
		public void stateChanged(JmsPooledConnection source, State oldState, State newState)
		{
			if (newState == State.IN_POOL)
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("requeued JMS connection of " + poolingConnectionFactory);
				}
				lastReleaseDate = new Date(MonotonicClock.currentTimeMillis());
			}
			if (oldState == State.IN_POOL && newState == State.ACCESSIBLE)
			{
				acquisitionDate = new Date(MonotonicClock.currentTimeMillis());
			}
			if (newState == State.CLOSED)
			{
				ManagementRegistrar.unregister(jmxName);
			}
		}

		/**
		 * Method stateChanging ...
		 *
		 * @param source
		 * 		of type JmsPooledConnection
		 * @param currentState
		 * 		of type State
		 * @param futureState
		 * 		of type State
		 */
		@Override
		public void stateChanging(JmsPooledConnection source, State currentState, State futureState)
		{
			//Nothing needed
		}
	}

	/**
	 * {@link JmsConnectionHandle} {@link bitronix.tm.resource.common.StateChangeListener}.
	 * When state changes to State.CLOSED, the session is removed from the list of opened sessions.
	 */
	private final class JmsConnectionHandleStateChangeListener
			implements StateChangeListener<DualSessionWrapper>
	{
		/**
		 * Method stateChanged ...
		 *
		 * @param source
		 * 		of type DualSessionWrapper
		 * @param oldState
		 * 		of type State
		 * @param newState
		 * 		of type State
		 */
		@Override
		public void stateChanged(DualSessionWrapper source, State oldState, State newState)
		{
			if (newState == State.CLOSED)
			{
				synchronized (sessions)
				{
					sessions.remove(source);
					if (LogDebugCheck.isDebugEnabled())
					{
						log.finer("DualSessionWrapper has been closed, " + sessions.size() + " session(s) left open in pooled connection");
					}
				}
			}
		}

		/**
		 * Method stateChanging ...
		 *
		 * @param source
		 * 		of type DualSessionWrapper
		 * @param currentState
		 * 		of type State
		 * @param futureState
		 * 		of type State
		 */
		@Override
		public void stateChanging(DualSessionWrapper source, State currentState, State futureState)
		{
			//Nothing needed
		}
	}
}
