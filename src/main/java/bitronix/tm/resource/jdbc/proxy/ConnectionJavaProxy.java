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
import bitronix.tm.resource.common.TransactionContextHelper;
import bitronix.tm.resource.jdbc.JdbcPooledConnection;
import bitronix.tm.resource.jdbc.LruStatementCache.CacheKey;
import bitronix.tm.resource.jdbc.PooledConnectionProxy;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.Map;

/**
 * @author Brett Wooldridge
 */
@SuppressWarnings({"Duplicates", "unused"})
public class ConnectionJavaProxy
		extends JavaProxyBase<Connection>
		implements PooledConnectionProxy
{

	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(ConnectionJavaProxy.class.toString());
	private static final Map<String, Method> selfMethodMap = createMethodMap(ConnectionJavaProxy.class);
	private static final String CONNECTION_ALREADY_CLOSED = "connection handle already closed";

	private JdbcPooledConnection jdbcPooledConnection;
	private boolean useStatementCache;

	/**
	 * Constructor ConnectionJavaProxy creates a new ConnectionJavaProxy instance.
	 */
	public ConnectionJavaProxy()
	{
		// Default constructor
	}

	/**
	 * Constructor ConnectionJavaProxy creates a new ConnectionJavaProxy instance.
	 *
	 * @param jdbcPooledConnection
	 * 		of type JdbcPooledConnection
	 * @param connection
	 * 		of type Connection
	 */
	public ConnectionJavaProxy(JdbcPooledConnection jdbcPooledConnection, Connection connection)
	{
		initialize(jdbcPooledConnection, connection);
	}

	/**
	 * Method initialize ...
	 *
	 * @param jdbcPooledConnection
	 * 		of type JdbcPooledConnection
	 * @param connection
	 * 		of type Connection
	 */
	private void initialize(JdbcPooledConnection jdbcPooledConnection, Connection connection)
	{
		this.proxy = this;
		this.jdbcPooledConnection = jdbcPooledConnection;
		this.delegate = connection;

		if (jdbcPooledConnection != null)
		{
			useStatementCache = jdbcPooledConnection.getPoolingDataSource()
			                                        .getPreparedStatementCacheSize() > 0;
		}
	}

	/**
	 * Method toString ...
	 *
	 * @return String
	 */
	@Override
	public String toString()
	{
		return "a ConnectionJavaProxy of " + jdbcPooledConnection + " on " + delegate;
	}

	/* PooledConnectionProxy interface methods */

	/**
	 * Method getPooledConnection returns the pooledConnection of this ConnectionJavaProxy object.
	 *
	 * @return the pooledConnection (type JdbcPooledConnection) of this ConnectionJavaProxy object.
	 */
	@Override
	public JdbcPooledConnection getPooledConnection()
	{
		return jdbcPooledConnection;
	}

	/**
	 * Method getProxiedDelegate returns the proxiedDelegate of this ConnectionJavaProxy object.
	 *
	 * @return the proxiedDelegate (type Connection) of this ConnectionJavaProxy object.
	 */
	@Override
	public Connection getProxiedDelegate()
	{
		return delegate;
	}

	/* Overridden methods of java.sql.Connection */

	/**
	 * Method close ...
	 *
	 * @throws SQLException
	 * 		when
	 */
	public void close() throws SQLException
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("closing " + this);
		}

		// in case the connection has already been closed
		if (jdbcPooledConnection == null)
		{
			return;
		}

		jdbcPooledConnection.release();
		jdbcPooledConnection = null;
	}

	/**
	 * Method commit ...
	 *
	 * @throws SQLException
	 * 		when
	 */
	public void commit() throws SQLException
	{
		if (jdbcPooledConnection == null)
		{
			throw new SQLException(CONNECTION_ALREADY_CLOSED);
		}
		if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
		{
			throw new SQLException("cannot commit a resource enlisted in a global transaction");
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
		if (jdbcPooledConnection == null)
		{
			throw new SQLException(CONNECTION_ALREADY_CLOSED);
		}
		if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
		{
			throw new SQLException("cannot rollback a resource enlisted in a global transaction");
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
		if (jdbcPooledConnection == null)
		{
			throw new SQLException(CONNECTION_ALREADY_CLOSED);
		}
		if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
		{
			throw new SQLException("cannot rollback a resource enlisted in a global transaction");
		}

		delegate.rollback(savepoint);
	}

	/**
	 * Method setSavepoint ...
	 *
	 * @return Savepoint
	 *
	 * @throws SQLException
	 * 		when
	 */
	public Savepoint setSavepoint() throws SQLException
	{
		if (jdbcPooledConnection == null)
		{
			throw new SQLException(CONNECTION_ALREADY_CLOSED);
		}
		if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
		{
			throw new SQLException("cannot set a savepoint on a resource enlisted in a global transaction");
		}

		return delegate.setSavepoint();
	}

	/**
	 * Method setSavepoint ...
	 *
	 * @param name
	 * 		of type String
	 *
	 * @return Savepoint
	 *
	 * @throws SQLException
	 * 		when
	 */
	public Savepoint setSavepoint(String name) throws SQLException
	{
		if (jdbcPooledConnection == null)
		{
			throw new SQLException(CONNECTION_ALREADY_CLOSED);
		}
		if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
		{
			throw new SQLException("cannot set a savepoint on a resource enlisted in a global transaction");
		}

		return delegate.setSavepoint(name);
	}

	/**
	 * Method releaseSavepoint ...
	 *
	 * @param savepoint
	 * 		of type Savepoint
	 *
	 * @throws SQLException
	 * 		when
	 */
	public void releaseSavepoint(Savepoint savepoint) throws SQLException
	{
		if (jdbcPooledConnection == null)
		{
			throw new SQLException(CONNECTION_ALREADY_CLOSED);
		}
		if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
		{
			throw new SQLException("cannot release a savepoint on a resource enlisted in a global transaction");
		}

		delegate.releaseSavepoint(savepoint);
	}

	/**
	 * Method getAutoCommit returns the autoCommit of this ConnectionJavaProxy object.
	 *
	 * @return the autoCommit (type boolean) of this ConnectionJavaProxy object.
	 *
	 * @throws SQLException
	 * 		when
	 */
	public boolean getAutoCommit() throws SQLException
	{
		if (jdbcPooledConnection == null)
		{
			throw new SQLException(CONNECTION_ALREADY_CLOSED);
		}

		if (jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
		{
			return false;
		}

		return delegate.getAutoCommit();
	}

	/**
	 * Method setAutoCommit sets the autoCommit of this ConnectionJavaProxy object.
	 *
	 * @param autoCommit
	 * 		the autoCommit of this ConnectionJavaProxy object.
	 *
	 * @throws SQLException
	 * 		when
	 */
	public void setAutoCommit(boolean autoCommit) throws SQLException
	{
		if (jdbcPooledConnection == null)
		{
			throw new SQLException(CONNECTION_ALREADY_CLOSED);
		}

		if (!jdbcPooledConnection.isParticipatingInActiveGlobalTransaction())
		{
			delegate.setAutoCommit(autoCommit);
		}
		else if (autoCommit)
		{
			throw new SQLException("autocommit is not allowed on a resource enlisted in a global transaction");
		}
	}

	/**
	 * Method isClosed returns the closed of this ConnectionJavaProxy object.
	 *
	 * @return the closed (type boolean) of this ConnectionJavaProxy object.
	 *
	 * @throws SQLException
	 * 		when
	 */
	public boolean isClosed() throws SQLException
	{
		if (jdbcPooledConnection == null)
		{
			return true;
		}
		return delegate.isClosed();
	}

	/**
	 * Method createStatement ...
	 *
	 * @return Statement
	 *
	 * @throws SQLException
	 * 		when
	 */
	public Statement createStatement() throws SQLException
	{
		enlistResource();

		Statement statement = delegate.createStatement();
		jdbcPooledConnection.registerUncachedStatement(statement);
		return JdbcProxyFactory.INSTANCE.getProxyStatement(jdbcPooledConnection, statement);
	}

	/**
	 * Enlist this connection into the current transaction if automaticEnlistingEnabled = true for this resource.
	 * If no transaction is running then this method does nothing.
	 *
	 * @throws SQLException
	 * 		thrown when an error occurs during enlistment.
	 */
	private void enlistResource() throws SQLException
	{
		if (jdbcPooledConnection == null)
		{
			throw new SQLException(CONNECTION_ALREADY_CLOSED);
		}

		if (jdbcPooledConnection.getPoolingDataSource()
		                        .getAutomaticEnlistingEnabled())
		{
			try
			{
				TransactionContextHelper.enlistInCurrentTransaction(jdbcPooledConnection);
			}
			catch (SystemException | RollbackException ex)
			{
				throw new SQLException("error enlisting " + this, ex);
			}
		} // if getAutomaticEnlistingEnabled
	}

	/**
	 * Method createStatement ...
	 *
	 * @param resultSetType
	 * 		of type int
	 * @param resultSetConcurrency
	 * 		of type int
	 *
	 * @return Statement
	 *
	 * @throws SQLException
	 * 		when
	 */
	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException
	{
		enlistResource();

		Statement statement = delegate.createStatement(resultSetType, resultSetConcurrency);
		jdbcPooledConnection.registerUncachedStatement(statement);
		return JdbcProxyFactory.INSTANCE.getProxyStatement(jdbcPooledConnection, statement);
	}

	/**
	 * Method createStatement ...
	 *
	 * @param resultSetType
	 * 		of type int
	 * @param resultSetConcurrency
	 * 		of type int
	 * @param resultSetHoldability
	 * 		of type int
	 *
	 * @return Statement
	 *
	 * @throws SQLException
	 * 		when
	 */
	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		enlistResource();

		Statement statement = delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
		jdbcPooledConnection.registerUncachedStatement(statement);
		return JdbcProxyFactory.INSTANCE.getProxyStatement(jdbcPooledConnection, statement);
	}

	/**
	 * Method prepareCall ...
	 *
	 * @param sql
	 * 		of type String
	 *
	 * @return CallableStatement
	 *
	 * @throws SQLException
	 * 		when
	 */
	public CallableStatement prepareCall(String sql) throws SQLException
	{
		enlistResource();

		CallableStatement statement = delegate.prepareCall(sql);
		jdbcPooledConnection.registerUncachedStatement(statement);
		return JdbcProxyFactory.INSTANCE.getProxyCallableStatement(jdbcPooledConnection, statement);
	}

	/**
	 * Method prepareCall ...
	 *
	 * @param sql
	 * 		of type String
	 * @param resultSetType
	 * 		of type int
	 * @param resultSetConcurrency
	 * 		of type int
	 *
	 * @return CallableStatement
	 *
	 * @throws SQLException
	 * 		when
	 */
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
	{
		enlistResource();

		CallableStatement statement = delegate.prepareCall(sql, resultSetType, resultSetConcurrency);
		jdbcPooledConnection.registerUncachedStatement(statement);
		return JdbcProxyFactory.INSTANCE.getProxyCallableStatement(jdbcPooledConnection, statement);
	}

	/* PreparedStatement cache aware methods */

	/**
	 * Method prepareCall ...
	 *
	 * @param sql
	 * 		of type String
	 * @param resultSetType
	 * 		of type int
	 * @param resultSetConcurrency
	 * 		of type int
	 * @param resultSetHoldability
	 * 		of type int
	 *
	 * @return CallableStatement
	 *
	 * @throws SQLException
	 * 		when
	 */
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		enlistResource();

		CallableStatement statement = delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		jdbcPooledConnection.registerUncachedStatement(statement);
		return JdbcProxyFactory.INSTANCE.getProxyCallableStatement(jdbcPooledConnection, statement);
	}

	/**
	 * Method prepareStatement ...
	 *
	 * @param sql
	 * 		of type String
	 *
	 * @return PreparedStatement
	 *
	 * @throws SQLException
	 * 		when
	 */
	public PreparedStatement prepareStatement(String sql) throws SQLException
	{
		enlistResource();

		if (useStatementCache)
		{
			CacheKey cacheKey = new CacheKey(sql);
			PreparedStatement cachedStmt = jdbcPooledConnection.getCachedStatement(cacheKey);
			if (cachedStmt == null)
			{
				cachedStmt = delegate.prepareStatement(sql);
				jdbcPooledConnection.putCachedStatement(cacheKey, cachedStmt);
			}

			return JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, cachedStmt, cacheKey);
		}
		else
		{
			PreparedStatement stmt = delegate.prepareStatement(sql);
			jdbcPooledConnection.registerUncachedStatement(stmt);
			return JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, stmt, null);
		}
	}

	/**
	 * Method prepareStatement ...
	 *
	 * @param sql
	 * 		of type String
	 * @param autoGeneratedKeys
	 * 		of type int
	 *
	 * @return PreparedStatement
	 *
	 * @throws SQLException
	 * 		when
	 */

	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException
	{
		enlistResource();

		if (useStatementCache)
		{
			CacheKey cacheKey = new CacheKey(sql, autoGeneratedKeys);
			PreparedStatement cachedStmt = jdbcPooledConnection.getCachedStatement(cacheKey);
			if (cachedStmt == null)
			{
				cachedStmt = delegate.prepareStatement(sql, autoGeneratedKeys);
				jdbcPooledConnection.putCachedStatement(cacheKey, cachedStmt);
			}

			return JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, cachedStmt, cacheKey);
		}
		else
		{
			PreparedStatement stmt = delegate.prepareStatement(sql, autoGeneratedKeys);
			jdbcPooledConnection.registerUncachedStatement(stmt);
			return JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, stmt, null);
		}
	}

	/**
	 * Method prepareStatement ...
	 *
	 * @param sql
	 * 		of type String
	 * @param resultSetType
	 * 		of type int
	 * @param resultSetConcurrency
	 * 		of type int
	 *
	 * @return PreparedStatement
	 *
	 * @throws SQLException
	 * 		when
	 */
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException
	{
		enlistResource();

		if (useStatementCache)
		{
			CacheKey cacheKey = new CacheKey(sql, resultSetType, resultSetConcurrency);
			PreparedStatement cachedStmt = jdbcPooledConnection.getCachedStatement(cacheKey);
			if (cachedStmt == null)
			{
				cachedStmt = delegate.prepareStatement(sql, resultSetType, resultSetConcurrency);
				jdbcPooledConnection.putCachedStatement(cacheKey, cachedStmt);
			}

			return JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, cachedStmt, cacheKey);
		}
		else
		{
			PreparedStatement stmt = delegate.prepareStatement(sql, resultSetType, resultSetConcurrency);
			jdbcPooledConnection.registerUncachedStatement(stmt);
			return JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, stmt, null);
		}
	}

	/**
	 * Method prepareStatement ...
	 *
	 * @param sql
	 * 		of type String
	 * @param resultSetType
	 * 		of type int
	 * @param resultSetConcurrency
	 * 		of type int
	 * @param resultSetHoldability
	 * 		of type int
	 *
	 * @return PreparedStatement
	 *
	 * @throws SQLException
	 * 		when
	 */
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
	{
		enlistResource();

		if (useStatementCache)
		{
			CacheKey cacheKey = new CacheKey(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
			PreparedStatement cachedStmt = jdbcPooledConnection.getCachedStatement(cacheKey);
			if (cachedStmt == null)
			{
				cachedStmt = delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
				jdbcPooledConnection.putCachedStatement(cacheKey, cachedStmt);
			}

			return JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, cachedStmt, cacheKey);
		}
		else
		{
			PreparedStatement stmt = delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
			jdbcPooledConnection.registerUncachedStatement(stmt);
			return JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, stmt, null);
		}
	}

	/**
	 * Method prepareStatement ...
	 *
	 * @param sql
	 * 		of type String
	 * @param columnIndexes
	 * 		of type int[]
	 *
	 * @return PreparedStatement
	 *
	 * @throws SQLException
	 * 		when
	 */
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException
	{
		enlistResource();

		if (useStatementCache)
		{
			CacheKey cacheKey = new CacheKey(sql, columnIndexes);
			PreparedStatement cachedStmt = jdbcPooledConnection.getCachedStatement(cacheKey);
			if (cachedStmt == null)
			{
				cachedStmt = delegate.prepareStatement(sql, columnIndexes);
				jdbcPooledConnection.putCachedStatement(cacheKey, cachedStmt);
			}

			return JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, cachedStmt, cacheKey);
		}
		else
		{
			PreparedStatement stmt = delegate.prepareStatement(sql, columnIndexes);
			jdbcPooledConnection.registerUncachedStatement(stmt);
			return JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, stmt, null);
		}
	}

	/* java.sql.Wrapper implementation */

	/**
	 * Method prepareStatement ...
	 *
	 * @param sql
	 * 		of type String
	 * @param columnNames
	 * 		of type String[]
	 *
	 * @return PreparedStatement
	 *
	 * @throws SQLException
	 * 		when
	 */
	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException
	{
		enlistResource();

		if (useStatementCache)
		{
			CacheKey cacheKey = new CacheKey(sql, columnNames);
			PreparedStatement cachedStmt = jdbcPooledConnection.getCachedStatement(cacheKey);
			if (cachedStmt == null)
			{
				cachedStmt = delegate.prepareStatement(sql, columnNames);
				jdbcPooledConnection.putCachedStatement(cacheKey, cachedStmt);
			}

			return JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, cachedStmt, cacheKey);
		}
		else
		{
			PreparedStatement stmt = delegate.prepareStatement(sql, columnNames);
			jdbcPooledConnection.registerUncachedStatement(stmt);
			return JdbcProxyFactory.INSTANCE.getProxyPreparedStatement(jdbcPooledConnection, stmt, null);
		}
	}

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

	/* Overridden methods of JavaProxyBase */

	/**
	 * Method getMethodMap returns the methodMap of this ConnectionJavaProxy object.
	 *
	 * @return the methodMap (type Map<String, Method>) of this ConnectionJavaProxy object.
	 */
	@Override
	protected Map<String, Method> getMethodMap()
	{
		return selfMethodMap;
	}
}
