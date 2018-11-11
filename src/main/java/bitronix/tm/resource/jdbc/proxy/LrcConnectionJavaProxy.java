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

import bitronix.tm.resource.jdbc.lrc.LrcXAResource;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Map;

/**
 * @author Brett Wooldridge
 */
public class LrcConnectionJavaProxy
		extends JavaProxyBase<Connection>
{

	private final static Map<String, Method> selfMethodMap = createMethodMap(LrcConnectionJavaProxy.class);

	private final LrcXAResource xaResource;

	/**
	 * Constructor LrcConnectionJavaProxy creates a new LrcConnectionJavaProxy instance.
	 *
	 * @param xaResource
	 * 		of type LrcXAResource
	 * @param connection
	 * 		of type Connection
	 */
	public LrcConnectionJavaProxy(LrcXAResource xaResource, Connection connection)
	{
		this.delegate = connection;
		this.xaResource = xaResource;
	}

	/**
	 * Method toString ...
	 *
	 * @return String
	 */
	@Override
	public String toString()
	{
		return "a JDBC LrcConnectionJavaProxy on " + delegate;
	}

	/* wrapped Connection methods that have special XA semantics */

	/**
	 * Method close ...
	 *
	 * @throws SQLException
	 * 		when
	 */
	public void close() throws SQLException
	{
		if (delegate != null)
		{
			delegate.close();
		}

		delegate = null;
	}

	/**
	 * Method isClosed returns the closed of this LrcConnectionJavaProxy object.
	 *
	 * @return the closed (type boolean) of this LrcConnectionJavaProxy object.
	 *
	 * @throws SQLException
	 * 		when
	 */
	public boolean isClosed() throws SQLException
	{
		return delegate == null;
	}

	/**
	 * Method setAutoCommit sets the autoCommit of this LrcConnectionJavaProxy object.
	 *
	 * @param autoCommit
	 * 		the autoCommit of this LrcConnectionJavaProxy object.
	 *
	 * @throws SQLException
	 * 		when
	 */
	public void setAutoCommit(boolean autoCommit) throws SQLException
	{
		if (xaResource.getState() != LrcXAResource.NO_TX && autoCommit)
		{
			throw new SQLException("XA transaction started, cannot enable autocommit mode");
		}
		delegate.setAutoCommit(autoCommit);
	}

	/**
	 * Method commit ...
	 *
	 * @throws SQLException
	 * 		when
	 */
	public void commit() throws SQLException
	{
		if (xaResource.getState() != LrcXAResource.NO_TX)
		{
			throw new SQLException("XA transaction started, cannot call commit directly on connection");
		}
		delegate.commit();
	}

	/**
	 * Method rollback ...
	 *
	 * @throws SQLException
	 * 		when
	 */
	public void rollback() throws SQLException
	{
		if (xaResource.getState() != LrcXAResource.NO_TX)
		{
			throw new SQLException("XA transaction started, cannot call rollback directly on connection");
		}
		delegate.rollback();
	}

	/**
	 * Method rollback ...
	 *
	 * @param savepoint
	 * 		of type Savepoint
	 *
	 * @throws SQLException
	 * 		when
	 */
	public void rollback(Savepoint savepoint) throws SQLException
	{
		if (xaResource.getState() != LrcXAResource.NO_TX)
		{
			throw new SQLException("XA transaction started, cannot call rollback directly on connection");
		}
		delegate.rollback(savepoint);
	}

	/* java.sql.Wrapper implementation */

	/**
	 * Method unwrap ...
	 *
	 * @param iface
	 * 		of type Class<T>
	 *
	 * @return T
	 *
	 * @throws SQLException
	 * 		when
	 */
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException
	{
		if (iface.isAssignableFrom(delegate.getClass()))
		{
			return (T) delegate;
		}
		if (isWrapperFor(iface))
		{
			return unwrap(delegate, iface);
		}
		throw new SQLException(getClass().getName() + " is not a wrapper for " + iface);
	}

	/**
	 * Method isWrapperFor ...
	 *
	 * @param iface
	 * 		of type Class<?>
	 *
	 * @return boolean
	 *
	 * @throws SQLException
	 * 		when
	 */
	public boolean isWrapperFor(Class<?> iface) throws SQLException
	{
		return iface.isAssignableFrom(delegate.getClass()) || isWrapperFor(delegate, iface);
	}

	/* Overridden methods of JavaProxyBase */

	/**
	 * Method getMethodMap returns the methodMap of this LrcConnectionJavaProxy object.
	 *
	 * @return the methodMap (type Map<String, Method>) of this LrcConnectionJavaProxy object.
	 */
	@Override
	protected Map<String, Method> getMethodMap()
	{
		return selfMethodMap;
	}
}
