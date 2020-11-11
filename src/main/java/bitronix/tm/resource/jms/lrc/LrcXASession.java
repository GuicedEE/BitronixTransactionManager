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

import bitronix.tm.internal.LogDebugCheck;

import jakarta.jms.*;
import javax.transaction.xa.XAResource;
import java.io.Serializable;

/**
 * XASession implementation for a non-XA JMS resource emulating XA with Last Resource Commit.
 *
 * @author Ludovic Orban
 */
public class LrcXASession
		implements XASession
{

	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(LrcXASession.class.toString());

	private final Session nonXaSession;
	private final XAResource xaResource;

	/**
	 * Constructor LrcXASession creates a new LrcXASession instance.
	 *
	 * @param session
	 * 		of type Session
	 */
	public LrcXASession(Session session)
	{
		this.nonXaSession = session;
		this.xaResource = new LrcXAResource(session);
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("creating new LrcXASession with " + xaResource);
		}
	}

	/**
	 * Method getSession returns the session of this LrcXASession object.
	 *
	 * @return the session (type Session) of this LrcXASession object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public Session getSession() throws JMSException
	{
		return nonXaSession;
	}

	/**
	 * Method getXAResource returns the XAResource of this LrcXASession object.
	 *
	 * @return the XAResource (type XAResource) of this LrcXASession object.
	 */
	@Override
	public XAResource getXAResource()
	{
		return xaResource;
	}

	/**
	 * Method getTransacted returns the transacted of this LrcXASession object.
	 *
	 * @return the transacted (type boolean) of this LrcXASession object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public boolean getTransacted() throws JMSException
	{
		return nonXaSession.getTransacted();
	}

	/**
	 * Method commit ...
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void commit() throws JMSException
	{
		nonXaSession.commit();
	}

	/**
	 * Method rollback ...
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void rollback() throws JMSException
	{
		nonXaSession.rollback();
	}

	/**
	 * Method createBytesMessage ...
	 *
	 * @return BytesMessage
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public BytesMessage createBytesMessage() throws JMSException
	{
		return nonXaSession.createBytesMessage();
	}

	/**
	 * Method createMapMessage ...
	 *
	 * @return MapMessage
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public MapMessage createMapMessage() throws JMSException
	{
		return nonXaSession.createMapMessage();
	}

	/**
	 * Method createMessage ...
	 *
	 * @return Message
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public Message createMessage() throws JMSException
	{
		return nonXaSession.createMessage();
	}

	/**
	 * Method createObjectMessage ...
	 *
	 * @return ObjectMessage
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public ObjectMessage createObjectMessage() throws JMSException
	{
		return nonXaSession.createObjectMessage();
	}

	/**
	 * Method createObjectMessage ...
	 *
	 * @param serializable
	 * 		of type Serializable
	 *
	 * @return ObjectMessage
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public ObjectMessage createObjectMessage(Serializable serializable) throws JMSException
	{
		return nonXaSession.createObjectMessage(serializable);
	}

	/**
	 * Method createStreamMessage ...
	 *
	 * @return StreamMessage
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public StreamMessage createStreamMessage() throws JMSException
	{
		return nonXaSession.createStreamMessage();
	}

	/**
	 * Method createTextMessage ...
	 *
	 * @return TextMessage
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public TextMessage createTextMessage() throws JMSException
	{
		return nonXaSession.createTextMessage();
	}

	/**
	 * Method createTextMessage ...
	 *
	 * @param text
	 * 		of type String
	 *
	 * @return TextMessage
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public TextMessage createTextMessage(String text) throws JMSException
	{
		return nonXaSession.createTextMessage(text);
	}

	/**
	 * Method getAcknowledgeMode returns the acknowledgeMode of this LrcXASession object.
	 *
	 * @return the acknowledgeMode (type int) of this LrcXASession object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public int getAcknowledgeMode() throws JMSException
	{
		return nonXaSession.getAcknowledgeMode();
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
		nonXaSession.close();
	}

	/**
	 * Method recover ...
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void recover() throws JMSException
	{
		nonXaSession.recover();
	}

	/**
	 * Method getMessageListener returns the messageListener of this LrcXASession object.
	 *
	 * @return the messageListener (type MessageListener) of this LrcXASession object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public MessageListener getMessageListener() throws JMSException
	{
		return nonXaSession.getMessageListener();
	}

	/**
	 * Method setMessageListener sets the messageListener of this LrcXASession object.
	 *
	 * @param messageListener
	 * 		the messageListener of this LrcXASession object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void setMessageListener(MessageListener messageListener) throws JMSException
	{
		nonXaSession.setMessageListener(messageListener);
	}

	/**
	 * Method run ...
	 */
	@Override
	public void run()
	{
		nonXaSession.run();
	}

	/**
	 * Method createProducer ...
	 *
	 * @param destination
	 * 		of type Destination
	 *
	 * @return MessageProducer
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public MessageProducer createProducer(Destination destination) throws JMSException
	{
		return nonXaSession.createProducer(destination);
	}

	/**
	 * Method createConsumer ...
	 *
	 * @param destination
	 * 		of type Destination
	 *
	 * @return MessageConsumer
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public MessageConsumer createConsumer(Destination destination) throws JMSException
	{
		return nonXaSession.createConsumer(destination);
	}

	/**
	 * Method createConsumer ...
	 *
	 * @param destination
	 * 		of type Destination
	 * @param messageSelector
	 * 		of type String
	 *
	 * @return MessageConsumer
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public MessageConsumer createConsumer(Destination destination, String messageSelector) throws JMSException
	{
		return nonXaSession.createConsumer(destination, messageSelector);
	}

	/**
	 * Method createConsumer ...
	 *
	 * @param destination
	 * 		of type Destination
	 * @param messageSelector
	 * 		of type String
	 * @param noLocal
	 * 		of type boolean
	 *
	 * @return MessageConsumer
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public MessageConsumer createConsumer(Destination destination, String messageSelector, boolean noLocal) throws JMSException
	{
		return nonXaSession.createConsumer(destination, messageSelector, noLocal);
	}

	/**
	 * Method createQueue ...
	 *
	 * @param queueName
	 * 		of type String
	 *
	 * @return Queue
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public Queue createQueue(String queueName) throws JMSException
	{
		return nonXaSession.createQueue(queueName);
	}

	/**
	 * Method createTopic ...
	 *
	 * @param topicName
	 * 		of type String
	 *
	 * @return Topic
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public Topic createTopic(String topicName) throws JMSException
	{
		return nonXaSession.createTopic(topicName);
	}

	/**
	 * Method createDurableSubscriber ...
	 *
	 * @param topic
	 * 		of type Topic
	 * @param name
	 * 		of type String
	 *
	 * @return TopicSubscriber
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public TopicSubscriber createDurableSubscriber(Topic topic, String name) throws JMSException
	{
		return nonXaSession.createDurableSubscriber(topic, name);
	}

	/**
	 * Method createDurableSubscriber ...
	 *
	 * @param topic
	 * 		of type Topic
	 * @param name
	 * 		of type String
	 * @param messageSelector
	 * 		of type String
	 * @param noLocal
	 * 		of type boolean
	 *
	 * @return TopicSubscriber
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public TopicSubscriber createDurableSubscriber(Topic topic, String name, String messageSelector, boolean noLocal) throws JMSException
	{
		return nonXaSession.createDurableSubscriber(topic, name, messageSelector, noLocal);
	}

	/**
	 * Method createBrowser ...
	 *
	 * @param queue
	 * 		of type Queue
	 *
	 * @return QueueBrowser
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public QueueBrowser createBrowser(Queue queue) throws JMSException
	{
		return nonXaSession.createBrowser(queue);
	}

	/**
	 * Method createBrowser ...
	 *
	 * @param queue
	 * 		of type Queue
	 * @param messageSelector
	 * 		of type String
	 *
	 * @return QueueBrowser
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSException
	{
		return nonXaSession.createBrowser(queue, messageSelector);
	}

	/**
	 * Method createTemporaryQueue ...
	 *
	 * @return TemporaryQueue
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public TemporaryQueue createTemporaryQueue() throws JMSException
	{
		return nonXaSession.createTemporaryQueue();
	}

	/**
	 * Method createTemporaryTopic ...
	 *
	 * @return TemporaryTopic
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public TemporaryTopic createTemporaryTopic() throws JMSException
	{
		return nonXaSession.createTemporaryTopic();
	}

	/**
	 * Method unsubscribe ...
	 *
	 * @param name
	 * 		of type String
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void unsubscribe(String name) throws JMSException
	{
		nonXaSession.unsubscribe(name);
	}

	/**
	 * Method toString ...
	 *
	 * @return String
	 */
	@Override
	public String toString()
	{
		return "a JMS LrcXASession on " + nonXaSession;
	}
}
