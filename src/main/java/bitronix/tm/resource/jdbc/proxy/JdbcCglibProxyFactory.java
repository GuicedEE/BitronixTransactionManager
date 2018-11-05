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
import net.sf.cglib.proxy.*;

import javax.sql.XAConnection;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.Map;
import java.util.Set;

/**
 * This class generates JDBC proxy classes using CGLIB bytecode generated
 * implementations. This factory's proxies are more efficient than JdbcJavaProxyFactory
 * but less efficient than JdbcJavassistProxyFactory.
 *
 * @author Brett Wooldridge
 */
public class JdbcCglibProxyFactory
		implements JdbcProxyFactory
{

	private final Class<Connection> proxyConnectionClass;
	private final Class<Statement> proxyStatementClass;
	private final Class<CallableStatement> proxyCallableStatementClass;
	private final Class<PreparedStatement> proxyPreparedStatementClass;
	private final Class<ResultSet> proxyResultSetClass;

	// For LRC we just use the standard Java Proxies
	private final JdbcJavaProxyFactory lrcProxyFactory;

	JdbcCglibProxyFactory()
	{
		proxyConnectionClass = createProxyConnectionClass();
		proxyStatementClass = createProxyStatementClass();
		proxyCallableStatementClass = createProxyCallableStatementClass();
		proxyPreparedStatementClass = createProxyPreparedStatementClass();
		proxyResultSetClass = createProxyResultSetClass();
		lrcProxyFactory = new JdbcJavaProxyFactory();
	}

	@SuppressWarnings("unchecked")
	private Class<Connection> createProxyConnectionClass()
	{
		Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(Connection.class);
		interfaces.add(PooledConnectionProxy.class);

		Enhancer enhancer = new Enhancer();
		enhancer.setInterfaces(interfaces.toArray(new Class<?>[0]));
		enhancer.setCallbackTypes(new Class[]{FastDispatcher.class, Interceptor.class});
		enhancer.setCallbackFilter(new InterceptorFilter(new ConnectionJavaProxy()));
		return enhancer.createClass();
	}

	@SuppressWarnings("unchecked")
	private Class<Statement> createProxyStatementClass()
	{
		Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(Statement.class);

		Enhancer enhancer = new Enhancer();
		enhancer.setInterfaces(interfaces.toArray(new Class<?>[0]));
		enhancer.setCallbackTypes(new Class[]{FastDispatcher.class, Interceptor.class});
		enhancer.setCallbackFilter(new InterceptorFilter(new StatementJavaProxy()));
		return enhancer.createClass();
	}

	@SuppressWarnings("unchecked")
	private Class<CallableStatement> createProxyCallableStatementClass()
	{
		Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(CallableStatement.class);

		Enhancer enhancer = new Enhancer();
		enhancer.setInterfaces(interfaces.toArray(new Class<?>[0]));
		enhancer.setCallbackTypes(new Class[]{FastDispatcher.class, Interceptor.class});
		enhancer.setCallbackFilter(new InterceptorFilter(new CallableStatementJavaProxy()));
		return enhancer.createClass();
	}

	@SuppressWarnings("unchecked")
	private Class<PreparedStatement> createProxyPreparedStatementClass()
	{
		Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(PreparedStatement.class);

		Enhancer enhancer = new Enhancer();
		enhancer.setInterfaces(interfaces.toArray(new Class<?>[0]));
		enhancer.setCallbackTypes(new Class[]{FastDispatcher.class, Interceptor.class});
		enhancer.setCallbackFilter(new InterceptorFilter(new PreparedStatementJavaProxy()));
		return enhancer.createClass();
	}

	@SuppressWarnings("unchecked")
	private Class<ResultSet> createProxyResultSetClass()
	{
		Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(ResultSet.class);

		Enhancer enhancer = new Enhancer();
		enhancer.setInterfaces(interfaces.toArray(new Class<?>[0]));
		enhancer.setCallbackTypes(new Class[]{FastDispatcher.class, Interceptor.class});
		enhancer.setCallbackFilter(new InterceptorFilter(new ResultSetJavaProxy()));
		return enhancer.createClass();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Connection getProxyConnection(JdbcPooledConnection jdbcPooledConnection, Connection connection)
	{
		ConnectionJavaProxy methodInterceptor = new ConnectionJavaProxy(jdbcPooledConnection, connection);
		Interceptor interceptor = new Interceptor(methodInterceptor);
		FastDispatcher fastDispatcher = new FastDispatcher(connection);

		try
		{
			Connection connectionCglibProxy = proxyConnectionClass.getDeclaredConstructor()
			                                                      .newInstance();
			((Factory) connectionCglibProxy).setCallbacks(new Callback[]{fastDispatcher, interceptor});
			return connectionCglibProxy;
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
	public Statement getProxyStatement(JdbcPooledConnection jdbcPooledConnection, Statement statement)
	{
		StatementJavaProxy methodInterceptor = new StatementJavaProxy(jdbcPooledConnection, statement);
		Interceptor interceptor = new Interceptor(methodInterceptor);
		FastDispatcher fastDispatcher = new FastDispatcher(statement);

		try
		{
			Statement statementCglibProxy = proxyStatementClass.getDeclaredConstructor()
			                                                   .newInstance();
			((Factory) statementCglibProxy).setCallbacks(new Callback[]{fastDispatcher, interceptor});
			return statementCglibProxy;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	// ---------------------------------------------------------------
	//  Generate CGLIB Proxy Classes
	// ---------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CallableStatement getProxyCallableStatement(JdbcPooledConnection jdbcPooledConnection, CallableStatement statement)
	{
		CallableStatementJavaProxy methodInterceptor = new CallableStatementJavaProxy(jdbcPooledConnection, statement);
		Interceptor interceptor = new Interceptor(methodInterceptor);
		FastDispatcher fastDispatcher = new FastDispatcher(statement);

		try
		{
			CallableStatement statementCglibProxy = proxyCallableStatementClass.getDeclaredConstructor()
			                                                                   .newInstance();
			((Factory) statementCglibProxy).setCallbacks(new Callback[]{fastDispatcher, interceptor});
			return statementCglibProxy;
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
		PreparedStatementJavaProxy methodInterceptor = new PreparedStatementJavaProxy(jdbcPooledConnection, statement, cacheKey);
		Interceptor interceptor = new Interceptor(methodInterceptor);
		FastDispatcher fastDispatcher = new FastDispatcher(statement);

		try
		{
			PreparedStatement statementCglibProxy = proxyPreparedStatementClass.getDeclaredConstructor()
			                                                                   .newInstance();
			((Factory) statementCglibProxy).setCallbacks(new Callback[]{fastDispatcher, interceptor});
			return statementCglibProxy;
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
		ResultSetJavaProxy methodInterceptor = new ResultSetJavaProxy(statement, resultSet);
		Interceptor interceptor = new Interceptor(methodInterceptor);
		FastDispatcher fastDispatcher = new FastDispatcher(resultSet);

		try
		{
			ResultSet resultSetCglibProxy = proxyResultSetClass.getDeclaredConstructor()
			                                                   .newInstance();
			((Factory) resultSetCglibProxy).setCallbacks(new Callback[]{fastDispatcher, interceptor});
			return resultSetCglibProxy;
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
		return lrcProxyFactory.getProxyXaConnection(connection);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Connection getProxyConnection(LrcXAResource xaResource, Connection connection)
	{
		return lrcProxyFactory.getProxyConnection(xaResource, connection);
	}

	// ---------------------------------------------------------------
	//  CGLIB Classes
	// ---------------------------------------------------------------

	static class FastDispatcher
			implements LazyLoader
	{
		private final Object delegate;

		public FastDispatcher(Object delegate)
		{
			this.delegate = delegate;
		}

		@Override
		public Object loadObject()
		{
			return delegate;
		}
	}

	static class Interceptor
			implements MethodInterceptor
	{
		private final JavaProxyBase<?> interceptor;

		public Interceptor(JavaProxyBase<?> interceptor)
		{
			this.interceptor = interceptor;
		}

		@Override
		public Object intercept(Object enhanced, Method method, Object[] args, MethodProxy fastProxy) throws Throwable
		{
			interceptor.proxy = enhanced;
			return interceptor.invoke(interceptor, method, args);
		}
	}

	static class InterceptorFilter
			implements CallbackFilter
	{
		private final Map<String, Method> methodMap;

		public InterceptorFilter(JavaProxyBase<?> proxyClass)
		{
			methodMap = proxyClass.getMethodMap();
		}

		@Override
		public int accept(Method method)
		{
			if (methodMap.containsKey(JavaProxyBase.getMethodKey(method)))
			{
				// Use the Interceptor
				return 1;
			}

			// Use the FastDispatcher
			return 0;
		}
	}
}
