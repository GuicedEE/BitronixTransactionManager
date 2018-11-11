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

import bitronix.tm.resource.jdbc.JdbcPooledConnection;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author Brett Wooldridge
 */
public class CallableStatementJavaProxy
		extends JavaProxyBase<CallableStatement>
{

	private final static Map<String, Method> selfMethodMap = createMethodMap(CallableStatementJavaProxy.class);

	private JdbcPooledConnection jdbcPooledConnection;

	/**
	 * Constructor CallableStatementJavaProxy creates a new CallableStatementJavaProxy instance.
	 */
	public CallableStatementJavaProxy()
	{
		// Default constructor
	}

	/**
	 * Constructor CallableStatementJavaProxy creates a new CallableStatementJavaProxy instance.
	 *
	 * @param jdbcPooledConnection
	 * 		of type JdbcPooledConnection
	 * @param statement
	 * 		of type CallableStatement
	 */
	public CallableStatementJavaProxy(JdbcPooledConnection jdbcPooledConnection, CallableStatement statement)
	{
		initialize(jdbcPooledConnection, statement);
	}

	/**
	 * Method initialize ...
	 *
	 * @param jdbcPooledConnection
	 * 		of type JdbcPooledConnection
	 * @param statement
	 * 		of type CallableStatement
	 */
	private void initialize(JdbcPooledConnection jdbcPooledConnection, CallableStatement statement)
	{
		this.proxy = this;
		this.jdbcPooledConnection = jdbcPooledConnection;
		this.delegate = statement;
	}

	/* Overridden methods of java.sql.CallableStatement */

	/**
	 * Method close ...
	 *
	 * @throws SQLException
	 * 		when
	 */
	public void close() throws SQLException
	{
		if (delegate == null)
		{
			return;
		}

		jdbcPooledConnection.unregisterUncachedStatement(delegate);
		delegate.close();
	}

	/**
	 * Method executeQuery ...
	 *
	 * @return ResultSet
	 *
	 * @throws SQLException
	 * 		when
	 */
	public ResultSet executeQuery() throws SQLException
	{
		return JdbcProxyFactory.INSTANCE.getProxyResultSet(this.getProxy(), delegate.executeQuery());
	}

	/**
	 * Method executeQuery ...
	 *
	 * @param sql
	 * 		of type String
	 *
	 * @return ResultSet
	 *
	 * @throws SQLException
	 * 		when
	 */
	public ResultSet executeQuery(String sql) throws SQLException
	{
		return JdbcProxyFactory.INSTANCE.getProxyResultSet(this.getProxy(), delegate.executeQuery(sql));
	}

	/**
	 * Method getGeneratedKeys returns the generatedKeys of this CallableStatementJavaProxy object.
	 *
	 * @return the generatedKeys (type ResultSet) of this CallableStatementJavaProxy object.
	 *
	 * @throws SQLException
	 * 		when
	 */
	public ResultSet getGeneratedKeys() throws SQLException
	{
		return JdbcProxyFactory.INSTANCE.getProxyResultSet(this.getProxy(), delegate.getGeneratedKeys());
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
	public boolean isWrapperFor(Class<?> iface)
	{
		return iface.isAssignableFrom(delegate.getClass()) || isWrapperFor(delegate, iface);
	}

	/**
	 * Method getMethodMap returns the methodMap of this CallableStatementJavaProxy object.
	 *
	 * @return the methodMap (type Map<String, Method>) of this CallableStatementJavaProxy object.
	 */
	@Override
	protected Map<String, Method> getMethodMap()
	{
		return selfMethodMap;
	}
}
