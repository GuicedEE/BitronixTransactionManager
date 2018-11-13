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
import bitronix.tm.resource.jdbc.LruStatementCache.CacheKey;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;
import java.util.logging.Level;

/**
 * @author Brett Wooldridge
 */
@SuppressWarnings("unused")
public class PreparedStatementJavaProxy
		extends JavaProxyBase<PreparedStatement>
{
	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(PreparedStatementJavaProxy.class.toString());
	private static final Map<String, Method> selfMethodMap = createMethodMap(PreparedStatementJavaProxy.class);

	private JdbcPooledConnection jdbcPooledConnection;
	private CacheKey cacheKey;
	private boolean pretendClosed;

	/**
	 * Constructor PreparedStatementJavaProxy creates a new PreparedStatementJavaProxy instance.
	 *
	 * @param jdbcPooledConnection
	 * 		of type JdbcPooledConnection
	 * @param statement
	 * 		of type PreparedStatement
	 * @param cacheKey
	 * 		of type CacheKey
	 */
	public PreparedStatementJavaProxy(JdbcPooledConnection jdbcPooledConnection, PreparedStatement statement, CacheKey cacheKey)
	{
		this();
		initialize(jdbcPooledConnection, statement, cacheKey);
	}

	/**
	 * Constructor PreparedStatementJavaProxy creates a new PreparedStatementJavaProxy instance.
	 */
	public PreparedStatementJavaProxy()
	{
		// Default constructor
	}

	/**
	 * Method initialize ...
	 *
	 * @param jdbcPooledConnection
	 * 		of type JdbcPooledConnection
	 * @param statement
	 * 		of type PreparedStatement
	 * @param cacheKey
	 * 		of type CacheKey
	 */
	void initialize(JdbcPooledConnection jdbcPooledConnection, PreparedStatement statement, CacheKey cacheKey)
	{
		this.proxy = this;
		this.jdbcPooledConnection = jdbcPooledConnection;
		this.delegate = statement;
		this.cacheKey = cacheKey;
		this.pretendClosed = false;
	}

	/**
	 * Method toString ...
	 *
	 * @return String
	 */
	@Override
	public String toString()
	{
		return "a PreparedStatementJavaProxy wrapping [" + delegate + "]";
	}

	/* Overridden methods of java.sql.PreparedStatement */

	/**
	 * Method close ...
	 *
	 * @throws SQLException
	 * 		when
	 */
	public void close() throws SQLException
	{
		if (pretendClosed || delegate == null)
		{
			return;
		}

		pretendClosed = true;

		if (cacheKey == null)
		{
			jdbcPooledConnection.unregisterUncachedStatement(delegate);
			delegate.close();
		}
		else
		{
			// Clear the parameters so the next use of this cached statement
			// doesn't pick up unexpected values.
			delegate.clearParameters();
			delegate.clearWarnings();
			try
			{
				delegate.clearBatch();
			}
			catch (SQLFeatureNotSupportedException e)
			{
				// Driver doesn't support batch updates.
				log.log(Level.FINEST, "Not Supported To Do Batch", e);
			}
			// Return to cache so the usage count can be updated
			jdbcPooledConnection.putCachedStatement(cacheKey, delegate);
		}
	}

	/**
	 * Method isClosed returns the closed of this PreparedStatementJavaProxy object.
	 *
	 * @return the closed (type boolean) of this PreparedStatementJavaProxy object.
	 */
	public boolean isClosed()
	{
		return pretendClosed;
	}

	/**
	 * Method getResultSet returns the resultSet of this PreparedStatementJavaProxy object.
	 *
	 * @return the resultSet (type ResultSet) of this PreparedStatementJavaProxy object.
	 *
	 * @throws SQLException
	 * 		when
	 */
	public ResultSet getResultSet() throws SQLException
	{
		ResultSet resultSet = delegate.getResultSet();
		return JdbcProxyFactory.INSTANCE.getProxyResultSet(this.getProxy(), resultSet);
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
		ResultSet resultSet = delegate.executeQuery();
		return JdbcProxyFactory.INSTANCE.getProxyResultSet(this.getProxy(), resultSet);
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
	 * Method getGeneratedKeys returns the generatedKeys of this PreparedStatementJavaProxy object.
	 *
	 * @return the generatedKeys (type ResultSet) of this PreparedStatementJavaProxy object.
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
	 * Method getMethodMap returns the methodMap of this PreparedStatementJavaProxy object.
	 *
	 * @return the methodMap (type Map String, Method ) of this PreparedStatementJavaProxy object.
	 */
	@Override
	protected Map<String, Method> getMethodMap()
	{
		return selfMethodMap;
	}
}
