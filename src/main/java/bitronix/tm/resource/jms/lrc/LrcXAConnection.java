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
package bitronix.tm.resource.jms.lrc;

import jakarta.jms.*;

/**
 * XAConnection implementation for a non-XA JMS resource emulating XA with Last Resource Commit.
 *
 * @author Ludovic Orban
 */
public class LrcXAConnection
		implements XAConnection
{

	private final Connection nonXaConnection;

	/**
	 * Constructor LrcXAConnection creates a new LrcXAConnection instance.
	 *
	 * @param connection
	 * 		of type Connection
	 */
	public LrcXAConnection(Connection connection)
	{
		this.nonXaConnection = connection;
	}

	/**
	 * Method createXASession ...
	 *
	 * @return XASession
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public XASession createXASession() throws JMSException
	{
		return new LrcXASession(nonXaConnection.createSession(true, Session.AUTO_ACKNOWLEDGE));
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
		throw new JMSException(LrcXAConnection.class.getName() + " can only respond to createXASession()");
	}

	/**
	 * Method getClientID returns the clientID of this LrcXAConnection object.
	 *
	 * @return the clientID (type String) of this LrcXAConnection object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public String getClientID() throws JMSException
	{
		return nonXaConnection.getClientID();
	}

	/**
	 * Method setClientID sets the clientID of this LrcXAConnection object.
	 *
	 * @param clientID
	 * 		the clientID of this LrcXAConnection object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void setClientID(String clientID) throws JMSException
	{
		nonXaConnection.setClientID(clientID);
	}

	/**
	 * Method getMetaData returns the metaData of this LrcXAConnection object.
	 *
	 * @return the metaData (type ConnectionMetaData) of this LrcXAConnection object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public ConnectionMetaData getMetaData() throws JMSException
	{
		return nonXaConnection.getMetaData();
	}

	/**
	 * Method getExceptionListener returns the exceptionListener of this LrcXAConnection object.
	 *
	 * @return the exceptionListener (type ExceptionListener) of this LrcXAConnection object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public ExceptionListener getExceptionListener() throws JMSException
	{
		return nonXaConnection.getExceptionListener();
	}

	/**
	 * Method setExceptionListener sets the exceptionListener of this LrcXAConnection object.
	 *
	 * @param exceptionListener
	 * 		the exceptionListener of this LrcXAConnection object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void setExceptionListener(ExceptionListener exceptionListener) throws JMSException
	{
		nonXaConnection.setExceptionListener(exceptionListener);
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
		nonXaConnection.start();
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
		nonXaConnection.stop();
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
		nonXaConnection.close();
	}

	/**
	 * Method createConnectionConsumer ...
	 *
	 * @param destination
	 * 		of type Destination
	 * @param messageSelector
	 * 		of type String
	 * @param serverSessionPool
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
	public ConnectionConsumer createConnectionConsumer(Destination destination, String messageSelector, ServerSessionPool serverSessionPool, int maxMessages) throws JMSException
	{
		return nonXaConnection.createConnectionConsumer(destination, messageSelector, serverSessionPool, maxMessages);
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
	 * @param serverSessionPool
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
	public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName, String messageSelector, ServerSessionPool serverSessionPool, int maxMessages) throws JMSException
	{
		return nonXaConnection.createDurableConnectionConsumer(topic, subscriptionName, messageSelector, serverSessionPool, maxMessages);
	}

	/**
	 * Method toString ...
	 *
	 * @return String
	 */
	@Override
	public String toString()
	{
		return "a JMS LrcXAConnection on " + nonXaConnection;
	}
}
