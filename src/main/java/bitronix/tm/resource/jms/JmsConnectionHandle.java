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

import jakarta.jms.*;

/**
 * Disposable Connection handle.
 *
 * @author Ludovic Orban
 */
public class JmsConnectionHandle
		implements Connection
{

	private final XAConnection xaConnection;
	private final JmsPooledConnection pooledConnection;
	private volatile boolean closed = false;

	/**
	 * Constructor JmsConnectionHandle creates a new JmsConnectionHandle instance.
	 *
	 * @param pooledConnection
	 * 		of type JmsPooledConnection
	 * @param xaConnection
	 * 		of type XAConnection
	 */
	public JmsConnectionHandle(JmsPooledConnection pooledConnection, XAConnection xaConnection)
	{
		this.pooledConnection = pooledConnection;
		this.xaConnection = xaConnection;
	}

	/**
	 * Method getPooledConnection returns the pooledConnection of this JmsConnectionHandle object.
	 *
	 * @return the pooledConnection (type JmsPooledConnection) of this JmsConnectionHandle object.
	 */
	public JmsPooledConnection getPooledConnection()
	{
		return pooledConnection;
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
	@Override
	public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException
	{
		return pooledConnection.createSession(transacted, acknowledgeMode);
	}

	/**
	 * Method getClientID returns the clientID of this JmsConnectionHandle object.
	 *
	 * @return the clientID (type String) of this JmsConnectionHandle object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public String getClientID() throws JMSException
	{
		return getXAConnection().getClientID();
	}

	/**
	 * Method getXAConnection returns the XAConnection of this JmsConnectionHandle object.
	 *
	 * @return the XAConnection (type XAConnection) of this JmsConnectionHandle object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	public XAConnection getXAConnection() throws JMSException
	{
		if (xaConnection == null)
		{
			throw new JMSException("XA connection handle has been closed");
		}
		return xaConnection;
	}

	/**
	 * Method setClientID sets the clientID of this JmsConnectionHandle object.
	 *
	 * @param jndiName
	 * 		the clientID of this JmsConnectionHandle object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void setClientID(String jndiName) throws JMSException
	{
		getXAConnection().setClientID(jndiName);
	}


	/* Connection implementation */

	/**
	 * Method getMetaData returns the metaData of this JmsConnectionHandle object.
	 *
	 * @return the metaData (type ConnectionMetaData) of this JmsConnectionHandle object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public ConnectionMetaData getMetaData() throws JMSException
	{
		return getXAConnection().getMetaData();
	}

	/**
	 * Method getExceptionListener returns the exceptionListener of this JmsConnectionHandle object.
	 *
	 * @return the exceptionListener (type ExceptionListener) of this JmsConnectionHandle object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public ExceptionListener getExceptionListener() throws JMSException
	{
		return getXAConnection().getExceptionListener();
	}

	/**
	 * Method setExceptionListener sets the exceptionListener of this JmsConnectionHandle object.
	 *
	 * @param listener
	 * 		the exceptionListener of this JmsConnectionHandle object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void setExceptionListener(ExceptionListener listener) throws JMSException
	{
		getXAConnection().setExceptionListener(listener);
	}

	/**
	 * Method start ...
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void start() throws JMSException
	{
		getXAConnection().start();
	}

	/**
	 * Method stop ...
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void stop() throws JMSException
	{
		getXAConnection().stop();
	}

	/**
	 * Method close ...
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void close() throws JMSException
	{
		if (closed)
		{
			return;
		}

		closed = true;
		pooledConnection.release();
	}

	/**
	 * Method createConnectionConsumer ...
	 *
	 * @param destination
	 * 		of type Destination
	 * @param messageSelector
	 * 		of type String
	 * @param sessionPool
	 * 		of type ServerSessionPool
	 * @param maxMessages
	 * 		of type int
	 *
	 * @return ConnectionConsumer
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public ConnectionConsumer createConnectionConsumer(Destination destination, String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException
	{
		return getXAConnection().createConnectionConsumer(destination, messageSelector, sessionPool, maxMessages);
	}

	/**
	 * Method createDurableConnectionConsumer ...
	 *
	 * @param topic
	 * 		of type Topic
	 * @param subscriptionName
	 * 		of type String
	 * @param messageSelector
	 * 		of type String
	 * @param sessionPool
	 * 		of type ServerSessionPool
	 * @param maxMessages
	 * 		of type int
	 *
	 * @return ConnectionConsumer
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName, String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException
	{
		return getXAConnection().createDurableConnectionConsumer(topic, subscriptionName, messageSelector, sessionPool, maxMessages);
	}

	/**
	 * Method toString ...
	 *
	 * @return String
	 */
	@Override
	public String toString()
	{
		return "a JmsConnectionHandle of " + pooledConnection;
	}

}
