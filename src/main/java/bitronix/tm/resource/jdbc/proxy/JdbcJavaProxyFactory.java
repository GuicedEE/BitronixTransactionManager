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
import bitronix.tm.resource.jdbc.PooledConnectionProxy;
import bitronix.tm.resource.jdbc.lrc.LrcXAResource;
import bitronix.tm.utils.ClassLoaderUtils;

import javax.sql.XAConnection;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.Set;

/**
 * This class generates JDBC proxy classes using stardard java.lang.reflect.Proxy
 * implementations.
 *
 * @author Brett Wooldridge
 */
public class JdbcJavaProxyFactory
		implements JdbcProxyFactory
{

	private final ProxyFactory<Connection> proxyConnectionFactory;
	private final ProxyFactory<XAConnection> proxyXAConnectionFactory;
	private final ProxyFactory<Statement> proxyStatementFactory;
	private final ProxyFactory<CallableStatement> proxyCallableStatementFactory;
	private final ProxyFactory<PreparedStatement> proxyPreparedStatementFactory;
	private final ProxyFactory<ResultSet> proxyResultSetFactory;

	/**
	 * Constructor JdbcJavaProxyFactory creates a new JdbcJavaProxyFactory instance.
	 */
	JdbcJavaProxyFactory()
	{
		proxyConnectionFactory = createProxyConnectionFactory();
		proxyXAConnectionFactory = createProxyXAConnectionFactory();
		proxyStatementFactory = createProxyStatementFactory();
		proxyCallableStatementFactory = createProxyCallableStatementFactory();
		proxyPreparedStatementFactory = createProxyPreparedStatementFactory();
		proxyResultSetFactory = createProxyResultSetFactory();
	}

	/**
	 * Method createProxyConnectionFactory ...
	 *
	 * @return ProxyFactory<Connection>
	 */
	private ProxyFactory<Connection> createProxyConnectionFactory()
	{

		Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(Connection.class);
		interfaces.add(PooledConnectionProxy.class);

		return new ProxyFactory<>(interfaces.toArray(new Class<?>[0]));
	}

	/**
	 * Method createProxyXAConnectionFactory ...
	 *
	 * @return ProxyFactory<XAConnection>
	 */
	private ProxyFactory<XAConnection> createProxyXAConnectionFactory()
	{

		Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(Connection.class);
		interfaces.add(XAConnection.class);

		return new ProxyFactory<>(interfaces.toArray(new Class<?>[0]));
	}

	/**
	 * Method createProxyStatementFactory ...
	 *
	 * @return ProxyFactory<Statement>
	 */
	private ProxyFactory<Statement> createProxyStatementFactory()
	{

		Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(Statement.class);

		return new ProxyFactory<>(interfaces.toArray(new Class<?>[0]));
	}

	/**
	 * Method createProxyCallableStatementFactory ...
	 *
	 * @return ProxyFactory<CallableStatement>
	 */
	private ProxyFactory<CallableStatement> createProxyCallableStatementFactory()
	{

		Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(CallableStatement.class);

		return new ProxyFactory<>(interfaces.toArray(new Class<?>[0]));
	}

	/**
	 * Method createProxyPreparedStatementFactory ...
	 *
	 * @return ProxyFactory<PreparedStatement>
	 */
	private ProxyFactory<PreparedStatement> createProxyPreparedStatementFactory()
	{

		Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(PreparedStatement.class);

		return new ProxyFactory<>(interfaces.toArray(new Class<?>[0]));
	}

	/**
	 * Method createProxyResultSetFactory ...
	 *
	 * @return ProxyFactory<ResultSet>
	 */
	private ProxyFactory<ResultSet> createProxyResultSetFactory()
	{
		Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(ResultSet.class);

		return new ProxyFactory<>(interfaces.toArray(new Class<?>[0]));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Connection getProxyConnection(JdbcPooledConnection jdbcPooledConnection, Connection connection)
	{
		try
		{
			ConnectionJavaProxy jdbcConnectionProxy = new ConnectionJavaProxy(jdbcPooledConnection, connection);
			return proxyConnectionFactory.getConstructor()
			                             .newInstance(jdbcConnectionProxy);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	// ---------------------------------------------------------------
	//  Generate high-efficiency Java Proxy Classes
	// ---------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Statement getProxyStatement(JdbcPooledConnection jdbcPooledConnection, Statement statement)
	{
		try
		{
			StatementJavaProxy jdbcStatementProxy = new StatementJavaProxy(jdbcPooledConnection, statement);
			return proxyStatementFactory.getConstructor()
			                            .newInstance(jdbcStatementProxy);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CallableStatement getProxyCallableStatement(JdbcPooledConnection jdbcPooledConnection, CallableStatement statement)
	{
		try
		{
			CallableStatementJavaProxy jdbcStatementProxy = new CallableStatementJavaProxy(jdbcPooledConnection, statement);
			return proxyCallableStatementFactory.getConstructor()
			                                    .newInstance(jdbcStatementProxy);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PreparedStatement getProxyPreparedStatement(JdbcPooledConnection jdbcPooledConnection, PreparedStatement statement, CacheKey cacheKey)
	{
		try
		{
			PreparedStatementJavaProxy jdbcStatementProxy = new PreparedStatementJavaProxy(jdbcPooledConnection, statement, cacheKey);
			return proxyPreparedStatementFactory.getConstructor()
			                                    .newInstance(jdbcStatementProxy);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ResultSet getProxyResultSet(Statement statement, ResultSet resultSet)
	{
		try
		{
			ResultSetJavaProxy jdbcResultSetProxy = new ResultSetJavaProxy(statement, resultSet);
			return proxyResultSetFactory.getConstructor()
			                            .newInstance(jdbcResultSetProxy);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public XAConnection getProxyXaConnection(Connection connection)
	{
		try
		{
			LrcXAConnectionJavaProxy jdbcLrcXaConnectionProxy = new LrcXAConnectionJavaProxy(connection);
			return proxyXAConnectionFactory.getConstructor()
			                               .newInstance(jdbcLrcXaConnectionProxy);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Connection getProxyConnection(LrcXAResource xaResource, Connection connection)
	{
		try
		{
			LrcConnectionJavaProxy lrcConnectionJavaProxy = new LrcConnectionJavaProxy(xaResource, connection);
			return proxyConnectionFactory.getConstructor()
			                             .newInstance(lrcConnectionJavaProxy);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public static class ProxyFactory<T>
	{
		private final Class<?>[] interfaces;
		private Reference<Constructor<T>> ctorRef;

		/**
		 * Constructor ProxyFactory creates a new ProxyFactory instance.
		 *
		 * @param interfaces
		 * 		of type Class<?>[]
		 */
		public ProxyFactory(Class<?>[] interfaces)
		{
			this.interfaces = interfaces;
		}

		/**
		 * Method newInstance ...
		 *
		 * @param handler
		 * 		of type InvocationHandler
		 *
		 * @return T
		 */
		public T newInstance(InvocationHandler handler)
		{
			if (handler == null)
			{
				throw new NullPointerException();
			}

			try
			{
				return getConstructor().newInstance(new Object[]{handler});
			}
			catch (Exception e)
			{
				throw new InternalError(e.toString());
			}
		}

		/**
		 * Method getConstructor returns the constructor of this ProxyFactory object.
		 *
		 * @return the constructor (type Constructor<T>) of this ProxyFactory object.
		 */
		@SuppressWarnings("unchecked")
		private synchronized Constructor<T> getConstructor()
		{
			Constructor<T> ctor = ctorRef == null ? null : ctorRef.get();

			if (ctor == null)
			{
				try
				{
					ctor = (Constructor<T>) Proxy.getProxyClass(getClass().getClassLoader(), interfaces)
					                             .getConstructor(new Class[]{InvocationHandler.class});
				}
				catch (NoSuchMethodException e)
				{
					throw new InternalError(e.toString());
				}

				ctorRef = new SoftReference<>(ctor);
			}

			return ctor;
		}
	}
}
