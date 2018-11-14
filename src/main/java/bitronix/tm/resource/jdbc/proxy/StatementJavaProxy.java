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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

/**
 * @author Brett Wooldridge
 */
@SuppressWarnings("unused")
public class StatementJavaProxy
		extends JavaProxyBase<Statement>
{

	private static final Map<String, Method> selfMethodMap = createMethodMap(StatementJavaProxy.class);

	private JdbcPooledConnection jdbcPooledConnection;

	/**
	 * Constructor StatementJavaProxy creates a new StatementJavaProxy instance.
	 *
	 * @param jdbcPooledConnection
	 * 		of type JdbcPooledConnection
	 * @param statement
	 * 		of type Statement
	 */
	public StatementJavaProxy(JdbcPooledConnection jdbcPooledConnection, Statement statement)
	{
		this();
		initialize(jdbcPooledConnection, statement);
	}

	/**
	 * Constructor StatementJavaProxy creates a new StatementJavaProxy instance.
	 */
	public StatementJavaProxy()
	{
		// Default constructor
	}

	/**
	 * Method initialize ...
	 *
	 * @param jdbcPooledConnection
	 * 		of type JdbcPooledConnection
	 * @param statement
	 * 		of type Statement
	 */
	void initialize(JdbcPooledConnection jdbcPooledConnection, Statement statement)
	{
		this.proxy = this;
		this.jdbcPooledConnection = jdbcPooledConnection;
		this.delegate = statement;
	}

	/* Overridden methods of java.sql.Statement */

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
		ResultSet resultSet = delegate.executeQuery(sql);
		return JdbcProxyFactory.INSTANCE.getProxyResultSet(this.getProxy(), resultSet);
	}

	/**
	 * Method getGeneratedKeys returns the generatedKeys of this StatementJavaProxy object.
	 *
	 * @return the generatedKeys (type ResultSet) of this StatementJavaProxy object.
	 *
	 * @throws SQLException
	 * 		when
	 */
	public ResultSet getGeneratedKeys() throws SQLException
	{
		ResultSet generatedKeys = delegate.getGeneratedKeys();
		return JdbcProxyFactory.INSTANCE.getProxyResultSet(this.getProxy(), generatedKeys);
	}

	/* java.sql.Wrapper implementation */

	/**
	 * Method unwrap ...
	 *
	 * @param iface
	 * 		of type Class T
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
	 * 		of type Class ?
	 *
	 * @return boolean
	 */
	public boolean isWrapperFor(Class<?> iface)
	{
		return iface.isAssignableFrom(delegate.getClass()) || isWrapperFor(delegate, iface);
	}

	/* Overridden methods of JavaProxyBase */

	/**
	 * Method getMethodMap returns the methodMap of this StatementJavaProxy object.
	 *
	 * @return the methodMap (type Map String, Method ) of this StatementJavaProxy object.
	 */
	@Override
	protected Map<String, Method> getMethodMap()
	{
		return selfMethodMap;
	}
}
