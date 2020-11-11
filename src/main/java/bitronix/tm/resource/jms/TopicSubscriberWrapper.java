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

import jakarta.jms.JMSException;
import jakarta.jms.Topic;
import jakarta.jms.TopicSubscriber;

/**
 * {@link TopicSubscriber} wrapper that adds XA enlistment semantics.
 *
 * @author Ludovic Orban
 */
public class TopicSubscriberWrapper
		extends MessageConsumerWrapper
		implements TopicSubscriber
{

	/**
	 * Constructor TopicSubscriberWrapper creates a new TopicSubscriberWrapper instance.
	 *
	 * @param topicSubscriber
	 * 		of type TopicSubscriber
	 * @param session
	 * 		of type DualSessionWrapper
	 * @param poolingConnectionFactory
	 * 		of type PoolingConnectionFactory
	 */
	public TopicSubscriberWrapper(TopicSubscriber topicSubscriber, DualSessionWrapper session, PoolingConnectionFactory poolingConnectionFactory)
	{
		super(topicSubscriber, session, poolingConnectionFactory);
	}

	/**
	 * Method getTopic returns the topic of this TopicSubscriberWrapper object.
	 *
	 * @return the topic (type Topic) of this TopicSubscriberWrapper object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public Topic getTopic() throws JMSException
	{
		return ((TopicSubscriber) getMessageConsumer()).getTopic();
	}

	/**
	 * Method getNoLocal returns the noLocal of this TopicSubscriberWrapper object.
	 *
	 * @return the noLocal (type boolean) of this TopicSubscriberWrapper object.
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public boolean getNoLocal() throws JMSException
	{
		return ((TopicSubscriber) getMessageConsumer()).getNoLocal();
	}

	/**
	 * Method toString ...
	 *
	 * @return String
	 */
	@Override
	public String toString()
	{
		return "a TopicSubscriberWrapper of " + session;
	}

}
