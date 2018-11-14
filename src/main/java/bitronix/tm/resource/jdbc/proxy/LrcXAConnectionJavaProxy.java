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
package bitronix.tm.resource.jdbc.proxy;

import bitronix.tm.internal.LogDebugCheck;
import bitronix.tm.resource.jdbc.lrc.LrcXAResource;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import javax.transaction.xa.XAResource;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Brett Wooldridge
 */
@SuppressWarnings("unused")
public class LrcXAConnectionJavaProxy
		extends JavaProxyBase<Connection>
{

	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(LrcXAConnectionJavaProxy.class.toString());

	private static final Map<String, Method> selfMethodMap = createMethodMap(LrcXAConnectionJavaProxy.class);

	private final LrcXAResource xaResource;
	private final List<ConnectionEventListener> connectionEventListeners = new CopyOnWriteArrayList<>();

	/**
	 * Constructor LrcXAConnectionJavaProxy creates a new LrcXAConnectionJavaProxy instance.
	 *
	 * @param connection
	 * 		of type Connection
	 */
	public LrcXAConnectionJavaProxy(Connection connection)
	{
		this.xaResource = new LrcXAResource(connection);
		this.delegate = new JdbcJavaProxyFactory().getProxyConnection(xaResource, connection);
	}

	/**
	 * Method getXAResource returns the XAResource of this LrcXAConnectionJavaProxy object.
	 *
	 * @return the XAResource (type XAResource) of this LrcXAConnectionJavaProxy object.
	 */
	public XAResource getXAResource()
	{
		return xaResource;
	}

	/**
	 * Method close ...
	 *
	 * @throws SQLException
	 * 		when
	 */
	public void close() throws SQLException
	{
		delegate.close();
		fireCloseEvent();
	}

	/**
	 * Method fireCloseEvent ...
	 */
	private void fireCloseEvent()
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("notifying " + connectionEventListeners.size() + " connectionEventListeners(s) about closing of " + this);
		}
		for (ConnectionEventListener connectionEventListener : connectionEventListeners)
		{
			connectionEventListener.connectionClosed(new ConnectionEvent((PooledConnection) delegate));
		}
	}

	/**
	 * Method getConnection returns the connection of this LrcXAConnectionJavaProxy object.
	 *
	 * @return the connection (type Connection) of this LrcXAConnectionJavaProxy object.
	 */
	public Connection getConnection()
	{
		return delegate;
	}

	/**
	 * Method addConnectionEventListener ...
	 *
	 * @param listener
	 * 		of type ConnectionEventListener
	 */
	public void addConnectionEventListener(ConnectionEventListener listener)
	{
		connectionEventListeners.add(listener);
	}

	/**
	 * Method removeConnectionEventListener ...
	 *
	 * @param listener
	 * 		of type ConnectionEventListener
	 */
	public void removeConnectionEventListener(ConnectionEventListener listener)
	{
		connectionEventListeners.remove(listener);
	}

	/**
	 * Method hashCode ...
	 *
	 * @return int
	 */
	@Override
	public int hashCode()
	{
		return this.delegate.hashCode();
	}

	/**
	 * Method equals ...
	 *
	 * @param obj
	 * 		of type Object
	 *
	 * @return boolean
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof LrcXAConnectionJavaProxy))
		{
			return false;
		}

		LrcXAConnectionJavaProxy other = (LrcXAConnectionJavaProxy) obj;
		return this.delegate.equals(other.delegate);
	}

	/**
	 * Method toString ...
	 *
	 * @return String
	 */
	@Override
	public String toString()
	{
		return "a JDBC LrcXAConnection on " + delegate;
	}

	/* Overridden methods of JavaProxyBase */

	/**
	 * Method getMethodMap returns the methodMap of this LrcXAConnectionJavaProxy object.
	 *
	 * @return the methodMap (type Map String, Method ) of this LrcXAConnectionJavaProxy object.
	 */
	@Override
	protected Map<String, Method> getMethodMap()
	{
		return selfMethodMap;
	}
}
