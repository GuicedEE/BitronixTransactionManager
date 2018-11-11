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

import bitronix.tm.resource.common.TransactionContextHelper;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

/**
 * {@link MessageProducer} wrapper that adds XA enlistment semantics.
 *
 * @author Ludovic Orban
 */
public class MessageProducerWrapper
		implements MessageProducer
{

	protected final DualSessionWrapper session;
	private final MessageProducer messageProducer;
	private final PoolingConnectionFactory poolingConnectionFactory;

	/**
	 * Constructor MessageProducerWrapper creates a new MessageProducerWrapper instance.
	 *
	 * @param messageProducer
	 * 		of type MessageProducer
	 * @param session
	 * 		of type DualSessionWrapper
	 * @param poolingConnectionFactory
	 * 		of type PoolingConnectionFactory
	 */
	public MessageProducerWrapper(MessageProducer messageProducer, DualSessionWrapper session, PoolingConnectionFactory poolingConnectionFactory)
	{
		this.messageProducer = messageProducer;
		this.session = session;
		this.poolingConnectionFactory = poolingConnectionFactory;
	}

	/**
	 * Method toString ...
	 *
	 * @return String
	 */
	@Override
	public String toString()
	{
		return "a MessageProducerWrapper of " + session;
	}

	/**
	 * Method getMessageProducer returns the messageProducer of this MessageProducerWrapper object.
	 *
	 * @return the messageProducer (type MessageProducer) of this MessageProducerWrapper object.
	 */
	public MessageProducer getMessageProducer()
	{
		return messageProducer;
	}

	/**
	 * Enlist this session into the current transaction if automaticEnlistingEnabled = true for this resource.
	 * If no transaction is running then this method does nothing.
	 *
	 * @throws JMSException
	 * 		if an exception occurs
	 */
	protected void enlistResource() throws JMSException
	{
		if (poolingConnectionFactory.getAutomaticEnlistingEnabled())
		{
			session.getSession(); // make sure the session is created before enlisting it
			try
			{
				TransactionContextHelper.enlistInCurrentTransaction(session);
			}
			catch (SystemException ex)
			{
				throw (JMSException) new JMSException("error enlisting " + this).initCause(ex);
			}
			catch (RollbackException ex)
			{
				throw (JMSException) new JMSException("error enlisting " + this).initCause(ex);
			}
		} // if getAutomaticEnlistingEnabled
	}



	/* MessageProducer with special XA semantics implementation */

	/**
	 * Method send ...
	 *
	 * @param message
	 * 		of type Message
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void send(Message message) throws JMSException
	{
		enlistResource();
		getMessageProducer().send(message);
	}

	/**
	 * Method send ...
	 *
	 * @param message
	 * 		of type Message
	 * @param deliveryMode
	 * 		of type int
	 * @param priority
	 * 		of type int
	 * @param timeToLive
	 * 		of type long
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void send(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException
	{
		enlistResource();
		getMessageProducer().send(message, deliveryMode, priority, timeToLive);
	}

	/**
	 * Method send ...
	 *
	 * @param destination
	 * 		of type Destination
	 * @param message
	 * 		of type Message
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void send(Destination destination, Message message) throws JMSException
	{
		enlistResource();
		getMessageProducer().send(destination, message);
	}

	/**
	 * Method send ...
	 *
	 * @param destination
	 * 		of type Destination
	 * @param message
	 * 		of type Message
	 * @param deliveryMode
	 * 		of type int
	 * @param priority
	 * 		of type int
	 * @param timeToLive
	 * 		of type long
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive) throws JMSException
	{
		enlistResource();
		getMessageProducer().send(destination, message, deliveryMode, priority, timeToLive);
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
		// do nothing as the close is handled by the session handle
	}

	/* dumb wrapping of MessageProducer methods */

	/**
	 * Method setDisableMessageID sets the disableMessageID of this MessageProducerWrapper object.
	 *
	 * @param value
	 * 		the disableMessageID of this MessageProducerWrapper object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void setDisableMessageID(boolean value) throws JMSException
	{
		getMessageProducer().setDisableMessageID(value);
	}

	/**
	 * Method getDisableMessageID returns the disableMessageID of this MessageProducerWrapper object.
	 *
	 * @return the disableMessageID (type boolean) of this MessageProducerWrapper object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public boolean getDisableMessageID() throws JMSException
	{
		return getMessageProducer().getDisableMessageID();
	}

	/**
	 * Method setDisableMessageTimestamp sets the disableMessageTimestamp of this MessageProducerWrapper object.
	 *
	 * @param value
	 * 		the disableMessageTimestamp of this MessageProducerWrapper object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void setDisableMessageTimestamp(boolean value) throws JMSException
	{
		getMessageProducer().setDisableMessageTimestamp(value);
	}

	/**
	 * Method getDisableMessageTimestamp returns the disableMessageTimestamp of this MessageProducerWrapper object.
	 *
	 * @return the disableMessageTimestamp (type boolean) of this MessageProducerWrapper object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public boolean getDisableMessageTimestamp() throws JMSException
	{
		return getMessageProducer().getDisableMessageTimestamp();
	}

	/**
	 * Method setDeliveryMode sets the deliveryMode of this MessageProducerWrapper object.
	 *
	 * @param deliveryMode
	 * 		the deliveryMode of this MessageProducerWrapper object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void setDeliveryMode(int deliveryMode) throws JMSException
	{
		getMessageProducer().setDeliveryMode(deliveryMode);
	}

	/**
	 * Method getDeliveryMode returns the deliveryMode of this MessageProducerWrapper object.
	 *
	 * @return the deliveryMode (type int) of this MessageProducerWrapper object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public int getDeliveryMode() throws JMSException
	{
		return getMessageProducer().getDeliveryMode();
	}

	/**
	 * Method setPriority sets the priority of this MessageProducerWrapper object.
	 *
	 * @param defaultPriority
	 * 		the priority of this MessageProducerWrapper object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void setPriority(int defaultPriority) throws JMSException
	{
		getMessageProducer().setPriority(defaultPriority);
	}

	/**
	 * Method getPriority returns the priority of this MessageProducerWrapper object.
	 *
	 * @return the priority (type int) of this MessageProducerWrapper object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public int getPriority() throws JMSException
	{
		return getMessageProducer().getPriority();
	}

	/**
	 * Method setTimeToLive sets the timeToLive of this MessageProducerWrapper object.
	 *
	 * @param timeToLive
	 * 		the timeToLive of this MessageProducerWrapper object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void setTimeToLive(long timeToLive) throws JMSException
	{
		getMessageProducer().setTimeToLive(timeToLive);
	}

	/**
	 * Method getTimeToLive returns the timeToLive of this MessageProducerWrapper object.
	 *
	 * @return the timeToLive (type long) of this MessageProducerWrapper object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public long getTimeToLive() throws JMSException
	{
		return getMessageProducer().getTimeToLive();
	}

	/**
	 * Method getDestination returns the destination of this MessageProducerWrapper object.
	 *
	 * @return the destination (type Destination) of this MessageProducerWrapper object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public Destination getDestination() throws JMSException
	{
		return getMessageProducer().getDestination();
	}

}
