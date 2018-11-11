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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

/**
 * {@link MessageConsumer} wrapper that adds XA enlistment semantics.
 *
 * @author Ludovic Orban
 */
public class MessageConsumerWrapper
		implements MessageConsumer
{

	protected final DualSessionWrapper session;
	private final MessageConsumer messageConsumer;
	private final PoolingConnectionFactory poolingConnectionFactory;

	/**
	 * Constructor MessageConsumerWrapper creates a new MessageConsumerWrapper instance.
	 *
	 * @param messageConsumer
	 * 		of type MessageConsumer
	 * @param session
	 * 		of type DualSessionWrapper
	 * @param poolingConnectionFactory
	 * 		of type PoolingConnectionFactory
	 */
	public MessageConsumerWrapper(MessageConsumer messageConsumer, DualSessionWrapper session, PoolingConnectionFactory poolingConnectionFactory)
	{
		this.messageConsumer = messageConsumer;
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
		return "a MessageConsumerWrapper of " + session;
	}

	/**
	 * Method getMessageSelector returns the messageSelector of this MessageConsumerWrapper object.
	 *
	 * @return the messageSelector (type String) of this MessageConsumerWrapper object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public String getMessageSelector() throws JMSException
	{
		return getMessageConsumer().getMessageSelector();
	}

	/**
	 * Method getMessageListener returns the messageListener of this MessageConsumerWrapper object.
	 *
	 * @return the messageListener (type MessageListener) of this MessageConsumerWrapper object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public MessageListener getMessageListener() throws JMSException
	{
		return getMessageConsumer().getMessageListener();
	}

	/* MessageProducer with special XA semantics implementation */

	/**
	 * Method setMessageListener sets the messageListener of this MessageConsumerWrapper object.
	 *
	 * @param listener
	 * 		the messageListener of this MessageConsumerWrapper object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void setMessageListener(MessageListener listener) throws JMSException
	{
		getMessageConsumer().setMessageListener(listener);
	}

	/**
	 * Method receive ...
	 *
	 * @return Message
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public Message receive() throws JMSException
	{
		enlistResource();
		return getMessageConsumer().receive();
	}

	/**
	 * Enlist this session into the current transaction if automaticEnlistingEnabled = true for this resource.
	 * If no transaction is running then this method does nothing.
	 *
	 * @throws javax.jms.JMSException
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

	/**
	 * Method receive ...
	 *
	 * @param timeout
	 * 		of type long
	 *
	 * @return Message
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public Message receive(long timeout) throws JMSException
	{
		enlistResource();
		return getMessageConsumer().receive(timeout);
	}

	/* dumb wrapping of MessageProducer methods */

	/**
	 * Method receiveNoWait ...
	 *
	 * @return Message
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public Message receiveNoWait() throws JMSException
	{
		enlistResource();
		return getMessageConsumer().receiveNoWait();
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

	/**
	 * Method getMessageConsumer returns the messageConsumer of this MessageConsumerWrapper object.
	 *
	 * @return the messageConsumer (type MessageConsumer) of this MessageConsumerWrapper object.
	 */
	public MessageConsumer getMessageConsumer()
	{
		return messageConsumer;
	}

}
