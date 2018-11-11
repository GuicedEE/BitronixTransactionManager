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
package bitronix.tm.mock.resource.jms;

import bitronix.tm.mock.resource.MockXAResource;
import org.mockito.stubbing.Answer;

import javax.jms.*;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Ludovic Orban
 */
public class MockXAConnectionFactory
		implements XAConnectionFactory
{

	private static JMSException staticCloseXAConnectionException;
	private static JMSException staticCreateXAConnectionException;

	private String endPoint;

	/**
	 * Method setStaticCloseXAConnectionException sets the staticCloseXAConnectionException of this MockXAConnectionFactory object.
	 *
	 * @param e
	 * 		the staticCloseXAConnectionException of this MockXAConnectionFactory object.
	 */
	public static void setStaticCloseXAConnectionException(JMSException e)
	{
		staticCloseXAConnectionException = e;
	}

	/**
	 * Method setStaticCreateXAConnectionException sets the staticCreateXAConnectionException of this MockXAConnectionFactory object.
	 *
	 * @param e
	 * 		the staticCreateXAConnectionException of this MockXAConnectionFactory object.
	 */
	public static void setStaticCreateXAConnectionException(JMSException e)
	{
		staticCreateXAConnectionException = e;
	}

	/**
	 * Method getEndpoint returns the endpoint of this MockXAConnectionFactory object.
	 *
	 * @return the endpoint (type String) of this MockXAConnectionFactory object.
	 */
	public String getEndpoint()
	{
		return endPoint;
	}

	/**
	 * Method setEndpoint sets the endpoint of this MockXAConnectionFactory object.
	 *
	 * @param endPoint
	 * 		the endpoint of this MockXAConnectionFactory object.
	 */
	public void setEndpoint(String endPoint)
	{
		this.endPoint = endPoint;
	}

	/**
	 * Method createXAConnection ...
	 *
	 * @return XAConnection
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public XAConnection createXAConnection() throws JMSException
	{
		if (staticCreateXAConnectionException != null)
		{
			throw staticCreateXAConnectionException;
		}

		Answer xaSessionAnswer = (Answer<XASession>) invocation ->
		{
			XASession mockXASession = mock(XASession.class);
			MessageProducer messageProducer = mock(MessageProducer.class);
			when(mockXASession.createProducer(anyObject())).thenReturn(messageProducer);
			MessageConsumer messageConsumer = mock(MessageConsumer.class);
			when(mockXASession.createConsumer(anyObject())).thenReturn(messageConsumer);
			when(mockXASession.createConsumer(anyObject(), anyString())).thenReturn(messageConsumer);
			when(mockXASession.createConsumer(anyObject(), anyString(), anyBoolean())).thenReturn(messageConsumer);
			Queue queue = mock(Queue.class);
			when(mockXASession.createQueue(anyString())).thenReturn(queue);
			Topic topic = mock(Topic.class);
			when(mockXASession.createTopic(anyString())).thenReturn(topic);
			MockXAResource mockXAResource = new MockXAResource(null);
			when(mockXASession.getXAResource()).thenReturn(mockXAResource);
			Answer<Session> sessionAnswer = invocation1 ->
			{
				Session session = mock(Session.class);
				MessageProducer producer = mock(MessageProducer.class);
				when(session.createProducer(anyObject())).thenReturn(producer);
				return session;
			};
			when(mockXASession.getSession()).thenAnswer(sessionAnswer);

			return mockXASession;
		};

		XAConnection mockXAConnection = mock(XAConnection.class);
		when(mockXAConnection.createXASession()).thenAnswer(xaSessionAnswer);
		when(mockXAConnection.createSession(anyBoolean(), anyInt())).thenAnswer(xaSessionAnswer);
		if (staticCloseXAConnectionException != null)
		{
			doThrow(staticCloseXAConnectionException).when(mockXAConnection)
			                                         .close();
		}

		return mockXAConnection;
	}


	/**
	 * Method createXAConnection ...
	 *
	 * @param jndiName
	 * 		of type String
	 * @param jndiName1
	 * 		of type String
	 *
	 * @return XAConnection
	 *
	 * @throws JMSException
	 * 		when
	 */
	@Override
	public XAConnection createXAConnection(String jndiName, String jndiName1) throws JMSException
	{
		return createXAConnection();
	}


}
