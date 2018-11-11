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

import bitronix.tm.BitronixTransaction;
import bitronix.tm.internal.BitronixRollbackSystemException;
import bitronix.tm.internal.BitronixSystemException;
import bitronix.tm.internal.LogDebugCheck;
import bitronix.tm.resource.common.AbstractXAResourceHolder;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.StateChangeListener;
import bitronix.tm.resource.common.TransactionContextHelper;

import javax.jms.*;
import javax.jms.IllegalStateException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

/**
 * JMS Session wrapper that will send calls to either a XASession or to a non-XA Session depending on the calling
 * context.
 *
 * @author Ludovic Orban
 */
public class DualSessionWrapper
		extends AbstractXAResourceHolder<DualSessionWrapper>
		implements Session, StateChangeListener<DualSessionWrapper>
{

	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(DualSessionWrapper.class.toString());

	private final JmsPooledConnection pooledConnection;
	private final boolean transacted;
	private final int acknowledgeMode;
	private final Map<MessageProducerConsumerKey, MessageProducer> messageProducers = new HashMap<>();
	private final Map<MessageProducerConsumerKey, MessageConsumer> messageConsumers = new HashMap<>();
	private final Map<MessageProducerConsumerKey, TopicSubscriberWrapper> topicSubscribers = new HashMap<>();
	private XASession xaSession;
	private Session session;
	private XAResource xaResource;
	private MessageListener listener;

	/**
	 * Constructor DualSessionWrapper creates a new DualSessionWrapper instance.
	 *
	 * @param pooledConnection
	 * 		of type JmsPooledConnection
	 * @param transacted
	 * 		of type boolean
	 * @param acknowledgeMode
	 * 		of type int
	 */
	public DualSessionWrapper(JmsPooledConnection pooledConnection, boolean transacted, int acknowledgeMode)
	{
		this.pooledConnection = pooledConnection;
		this.transacted = transacted;
		this.acknowledgeMode = acknowledgeMode;

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("getting session handle from " + pooledConnection);
		}
		setState(State.ACCESSIBLE);
		addStateChangeEventListener(this);
	}

	/**
	 * Method toString ...
	 *
	 * @return String
	 */
	@Override
	public String toString()
	{
		return "a DualSessionWrapper in state " + getState() + " of " + pooledConnection;
	}

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
	/*
	 * When the session is closed (directly or deferred) the action is to change its state to IN_POOL.
	 * There is no such state for JMS sessions, this just means that it has been closed -> force a
	 * state switch to CLOSED then clean up.
	 */
	@Override
	public void stateChanged(DualSessionWrapper source, State oldState, State newState)
	{
		if (newState == State.IN_POOL)
		{
			setState(State.CLOSED);
		}
		else if (newState == State.CLOSED)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("session state changing to CLOSED, cleaning it up: " + this);
			}

			if (xaSession != null)
			{
				try
				{
					xaSession.close();
				}
				catch (JMSException ex)
				{
					log.log(Level.SEVERE, "error closing XA session", ex);
				}
				xaSession = null;
				xaResource = null;
			}

			if (session != null)
			{
				try
				{
					session.close();
				}
				catch (JMSException ex)
				{
					log.log(Level.SEVERE, "error closing session", ex);
				}
				session = null;
			}

			Iterator<Entry<MessageProducerConsumerKey, MessageProducer>> it = messageProducers.entrySet()
			                                                                                  .iterator();
			while (it.hasNext())
			{
				Entry<MessageProducerConsumerKey, MessageProducer> entry = it.next();
				MessageProducerWrapper messageProducerWrapper = (MessageProducerWrapper) entry.getValue();
				try
				{
					messageProducerWrapper.close();
				}
				catch (JMSException ex)
				{
					log.log(Level.SEVERE, "error closing message producer", ex);
				}
			}
			messageProducers.clear();

			Iterator<Entry<MessageProducerConsumerKey, MessageConsumer>> it2 = messageConsumers.entrySet()
			                                                                                   .iterator();
			while (it2.hasNext())
			{
				Entry<MessageProducerConsumerKey, MessageConsumer> entry = it2.next();
				MessageConsumerWrapper messageConsumerWrapper = (MessageConsumerWrapper) entry.getValue();
				try
				{
					messageConsumerWrapper.close();
				}
				catch (JMSException ex)
				{
					log.log(Level.SEVERE, "error closing message consumer", ex);
				}
			}
			messageConsumers.clear();

		} // if newState == State.CLOSED
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
		//No config required
	}

	/**
	 * Get the vendor's {@link javax.transaction.xa.XAResource} implementation of the wrapped resource.
	 *
	 * @return the vendor's XAResource implementation.
	 */
	@Override
	public XAResource getXAResource()
	{
		return xaResource;
	}

	/**
	 * Get the ResourceBean which created this XAResourceHolder.
	 *
	 * @return the ResourceBean which created this XAResourceHolder.
	 */
	@Override
	public ResourceBean getResourceBean()
	{
		return getPoolingConnectionFactory();
	}

	/**
	 * Method getPoolingConnectionFactory returns the poolingConnectionFactory of this DualSessionWrapper object.
	 *
	 * @return the poolingConnectionFactory (type PoolingConnectionFactory) of this DualSessionWrapper object.
	 */
	public PoolingConnectionFactory getPoolingConnectionFactory()
	{
		return pooledConnection.getPoolingConnectionFactory();
	}


	/* wrapped Session methods that have special XA semantics */

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
		return Collections.singletonList(this);
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
		return null;
	}

	/**
	 * Close the physical connection that this {@link bitronix.tm.resource.common.XAStatefulHolder} represents.
	 *
	 * @throws Exception
	 * 		a resource-specific exception thrown when there is an error closing the physical connection.
	 */
	@Override
	public void close() throws JMSException
	{
		if (getState() != State.ACCESSIBLE)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("not closing already closed " + this);
			}
			return;
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("closing " + this);
		}

		// delisting
		try
		{
			TransactionContextHelper.delistFromCurrentTransaction(this);
		}
		catch (BitronixRollbackSystemException ex)
		{
			throw (JMSException) new TransactionRolledBackException("unilateral rollback of " + this).initCause(ex);
		}
		catch (SystemException ex)
		{
			throw (JMSException) new JMSException("error delisting " + this).initCause(ex);
		}
		finally
		{
			// requeuing
			try
			{
				TransactionContextHelper.requeue(this, pooledConnection.getPoolingConnectionFactory());
			}
			catch (BitronixSystemException ex)
			{
				// this may hide the exception thrown by delistFromCurrentTransaction() but
				// an error requeuing must absolutely be reported as an exception.
				// Too bad if this happens... See JdbcPooledConnection.release() as well.
				throw (JMSException) new JMSException("error requeuing " + this).initCause(ex);
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
		return null;
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
		if (xaResource == this.xaResource)
		{
			return this;
		}
		return null;
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
		return getSession().createBytesMessage();
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
		return getSession().createMapMessage();
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
		return getSession().createMessage();
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
		return getSession().createObjectMessage();
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
		return getSession().createObjectMessage(serializable);
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
		return getSession().createStreamMessage();
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
		return getSession().createTextMessage();
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
		return getSession().createTextMessage(text);
	}

	/* XAResourceHolder implementation */

	/**
	 * Method getTransacted returns the transacted of this DualSessionWrapper object.
	 *
	 * @return the transacted (type boolean) of this DualSessionWrapper object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public boolean getTransacted() throws JMSException
	{
		if (isParticipatingInActiveGlobalTransaction())
		{
			return true; // for consistency with EJB 2.1 spec (17.3.5)
		}

		return getSession().getTransacted();
	}

	/**
	 * Method getAcknowledgeMode returns the acknowledgeMode of this DualSessionWrapper object.
	 *
	 * @return the acknowledgeMode (type int) of this DualSessionWrapper object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public int getAcknowledgeMode() throws JMSException
	{
		if (isParticipatingInActiveGlobalTransaction())
		{
			return 0; // for consistency with EJB 2.1 spec (17.3.5)
		}

		return getSession().getAcknowledgeMode();
	}

	/* XAStatefulHolder implementation */

	/**
	 * Method commit ...
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void commit() throws JMSException
	{
		if (isParticipatingInActiveGlobalTransaction())
		{
			throw new TransactionInProgressException("cannot commit a resource enlisted in a global transaction");
		}

		getSession().commit();
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
		if (isParticipatingInActiveGlobalTransaction())
		{
			throw new TransactionInProgressException("cannot rollback a resource enlisted in a global transaction");
		}

		getSession().rollback();
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
		if (isParticipatingInActiveGlobalTransaction())
		{
			throw new TransactionInProgressException("cannot recover a resource enlisted in a global transaction");
		}

		getSession().recover();
	}

	/* XA-enhanced methods */

	/**
	 * Method getMessageListener returns the messageListener of this DualSessionWrapper object.
	 *
	 * @return the messageListener (type MessageListener) of this DualSessionWrapper object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public MessageListener getMessageListener() throws JMSException
	{
		return listener;
	}

	/**
	 * Method setMessageListener sets the messageListener of this DualSessionWrapper object.
	 *
	 * @param listener
	 * 		the messageListener of this DualSessionWrapper object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public void setMessageListener(MessageListener listener) throws JMSException
	{
		if (getState() == State.CLOSED)
		{
			throw new IllegalStateException("session handle is closed");
		}

		if (session != null)
		{
			session.setMessageListener(listener);
		}
		if (xaSession != null)
		{
			xaSession.setMessageListener(listener);
		}

		this.listener = listener;
	}

	/**
	 * Method run ...
	 */
	@Override
	public void run()
	{
		try
		{
			Session internalSession = getSession(true);
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("running XA session " + internalSession);
			}
			internalSession.run();
		}
		catch (JMSException ex)
		{
			log.log(Level.SEVERE, "error getting session", ex);
		}
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
		MessageProducerConsumerKey key = new MessageProducerConsumerKey(destination);
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("looking for producer based on " + key);
		}
		MessageProducerWrapper messageProducer = (MessageProducerWrapper) messageProducers.get(key);
		if (messageProducer == null)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("found no producer based on " + key + ", creating it");
			}
			messageProducer = new MessageProducerWrapper(getSession().createProducer(destination), this, pooledConnection.getPoolingConnectionFactory());

			if (pooledConnection.getPoolingConnectionFactory()
			                    .getCacheProducersConsumers())
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("caching producer via key " + key);
				}
				messageProducers.put(key, messageProducer);
			}
		}
		else if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("found producer based on " + key + ", recycling it: " + messageProducer);
		}
		return messageProducer;
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
		MessageProducerConsumerKey key = new MessageProducerConsumerKey(destination);
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("looking for consumer based on " + key);
		}
		MessageConsumerWrapper messageConsumer = (MessageConsumerWrapper) messageConsumers.get(key);
		if (messageConsumer == null)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("found no consumer based on " + key + ", creating it");
			}
			messageConsumer = new MessageConsumerWrapper(getSession().createConsumer(destination), this, pooledConnection.getPoolingConnectionFactory());

			if (pooledConnection.getPoolingConnectionFactory()
			                    .getCacheProducersConsumers())
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("caching consumer via key " + key);
				}
				messageConsumers.put(key, messageConsumer);
			}
		}
		else if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("found consumer based on " + key + ", recycling it: " + messageConsumer);
		}
		return messageConsumer;
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
		MessageProducerConsumerKey key = new MessageProducerConsumerKey(destination, messageSelector);
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("looking for consumer based on " + key);
		}
		MessageConsumerWrapper messageConsumer = (MessageConsumerWrapper) messageConsumers.get(key);
		if (messageConsumer == null)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("found no consumer based on " + key + ", creating it");
			}
			messageConsumer = new MessageConsumerWrapper(getSession().createConsumer(destination, messageSelector), this, pooledConnection.getPoolingConnectionFactory());

			if (pooledConnection.getPoolingConnectionFactory()
			                    .getCacheProducersConsumers())
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("caching consumer via key " + key);
				}
				messageConsumers.put(key, messageConsumer);
			}
		}
		else if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("found consumer based on " + key + ", recycling it: " + messageConsumer);
		}
		return messageConsumer;
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
		MessageProducerConsumerKey key = new MessageProducerConsumerKey(destination, messageSelector, noLocal);
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("looking for consumer based on " + key);
		}
		MessageConsumerWrapper messageConsumer = (MessageConsumerWrapper) messageConsumers.get(key);
		if (messageConsumer == null)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("found no consumer based on " + key + ", creating it");
			}
			messageConsumer = new MessageConsumerWrapper(getSession().createConsumer(destination, messageSelector, noLocal), this, pooledConnection.getPoolingConnectionFactory());

			if (pooledConnection.getPoolingConnectionFactory()
			                    .getCacheProducersConsumers())
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("caching consumer via key " + key);
				}
				messageConsumers.put(key, messageConsumer);
			}
		}
		else if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("found consumer based on " + key + ", recycling it: " + messageConsumer);
		}
		return messageConsumer;
	}

	/* dumb wrapping of Session methods */

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
	public javax.jms.Queue createQueue(String queueName) throws JMSException
	{
		return getSession().createQueue(queueName);
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
		return getSession().createTopic(topicName);
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
		MessageProducerConsumerKey key = new MessageProducerConsumerKey(topic);
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("looking for durable subscriber based on " + key);
		}
		TopicSubscriberWrapper topicSubscriber = topicSubscribers.get(key);
		if (topicSubscriber == null)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("found no durable subscriber based on " + key + ", creating it");
			}
			topicSubscriber = new TopicSubscriberWrapper(getSession().createDurableSubscriber(topic, name), this, pooledConnection.getPoolingConnectionFactory());

			if (pooledConnection.getPoolingConnectionFactory()
			                    .getCacheProducersConsumers())
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("caching durable subscriber via key " + key);
				}
				topicSubscribers.put(key, topicSubscriber);
			}
		}
		else if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("found durable subscriber based on " + key + ", recycling it: " + topicSubscriber);
		}
		return topicSubscriber;
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
		MessageProducerConsumerKey key = new MessageProducerConsumerKey(topic, messageSelector, noLocal);
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("looking for durable subscriber based on " + key);
		}
		TopicSubscriberWrapper topicSubscriber = topicSubscribers.get(key);
		if (topicSubscriber == null)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("found no durable subscriber based on " + key + ", creating it");
			}
			topicSubscriber = new TopicSubscriberWrapper(getSession().createDurableSubscriber(topic, name, messageSelector, noLocal), this,
			                                             pooledConnection.getPoolingConnectionFactory());

			if (pooledConnection.getPoolingConnectionFactory()
			                    .getCacheProducersConsumers())
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("caching durable subscriber via key " + key);
				}
				topicSubscribers.put(key, topicSubscriber);
			}
		}
		else if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("found durable subscriber based on " + key + ", recycling it: " + topicSubscriber);
		}
		return topicSubscriber;
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
	public QueueBrowser createBrowser(javax.jms.Queue queue) throws JMSException
	{
		enlistResource();
		return getSession().createBrowser(queue);
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
	public QueueBrowser createBrowser(javax.jms.Queue queue, String messageSelector) throws JMSException
	{
		enlistResource();
		return getSession().createBrowser(queue, messageSelector);
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
		return getSession().createTemporaryQueue();
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
		return getSession().createTemporaryTopic();
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
		getSession().unsubscribe(name);
	}

	/**
	 * Method getSession returns the session of this DualSessionWrapper object.
	 *
	 * @return the session (type Session) of this DualSessionWrapper object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	public Session getSession() throws JMSException
	{
		return getSession(false);
	}

	/**
	 * Method getSession ...
	 *
	 * @param forceXa
	 * 		of type boolean
	 *
	 * @return Session
	 *
	 * @throws JMSException
	 * 		when
	 */
	public Session getSession(boolean forceXa) throws JMSException
	{
		if (getState() == State.CLOSED)
		{
			throw new IllegalStateException("session handle is closed");
		}

		if (forceXa)
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("choosing XA session (forced)");
			}
			return createXASession();
		}
		else
		{
			BitronixTransaction currentTransaction = TransactionContextHelper.currentTransaction();
			if (currentTransaction != null)
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("choosing XA session");
				}
				return createXASession();
			}
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("choosing non-XA session");
			}
			return createNonXASession();
		}
	}

	/**
	 * Method createXASession ...
	 *
	 * @return Session
	 *
	 * @throws JMSException
	 * 		when
	 */
	private Session createXASession() throws JMSException
	{
		// XA
		if (xaSession == null)
		{
			xaSession = pooledConnection.getXAConnection()
			                            .createXASession();
			if (listener != null)
			{
				xaSession.setMessageListener(listener);
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("get XA session registered message listener: " + listener);
				}
			}
			xaResource = xaSession.getXAResource();
		}
		return xaSession.getSession();
	}

	/**
	 * Method createNonXASession ...
	 *
	 * @return Session
	 *
	 * @throws JMSException
	 * 		when
	 */
	private Session createNonXASession() throws JMSException
	{
		// non-XA
		if (session == null)
		{
			session = pooledConnection.getXAConnection()
			                          .createSession(transacted, acknowledgeMode);
			if (listener != null)
			{
				session.setMessageListener(listener);
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("get non-XA session registered message listener: " + listener);
				}
			}
		}
		return session;
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
		PoolingConnectionFactory poolingConnectionFactory = pooledConnection.getPoolingConnectionFactory();
		if (poolingConnectionFactory.getAutomaticEnlistingEnabled())
		{
			getSession(); // make sure the session is created before enlisting it
			try
			{
				TransactionContextHelper.enlistInCurrentTransaction(this);
			}
			catch (SystemException | RollbackException ex)
			{
				throw (JMSException) new JMSException("error enlisting " + this).initCause(ex);
			}
		} // if getAutomaticEnlistingEnabled
	}
}
