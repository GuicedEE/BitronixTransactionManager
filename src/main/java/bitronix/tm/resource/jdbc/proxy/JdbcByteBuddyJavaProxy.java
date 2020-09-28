package bitronix.tm.resource.jdbc.proxy;

import bitronix.tm.resource.jdbc.JdbcPooledConnection;
import bitronix.tm.resource.jdbc.LruStatementCache;
import bitronix.tm.resource.jdbc.lrc.LrcXAResource;

import javax.sql.XAConnection;
import java.lang.reflect.Constructor;
import java.sql.*;

public class JdbcByteBuddyJavaProxy
		implements JdbcProxyFactory
{
	// For LRC we just use the standard Java Proxies
	private final JdbcJavaProxyFactory lrcProxyFactory;

	private Constructor<Connection> proxyConnectionConstructor;
	private Constructor<Statement> proxyStatementConstructor;
	private Constructor<CallableStatement> proxyCallableStatementConstructor;
	private Constructor<PreparedStatement> proxyPreparedStatementConstructor;
	private Constructor<ResultSet> proxyResultSetConstructor;

	public JdbcByteBuddyJavaProxy()
	{
		lrcProxyFactory = new JdbcJavaProxyFactory();
	}

	private void createProxyConnectionClass()
	{

	}

	@Override
	public Connection getProxyConnection(JdbcPooledConnection jdbcPooledConnection, Connection connection)
	{
		ConnectionJavaProxy proxy = new ConnectionJavaProxy(jdbcPooledConnection, connection);
		return null;
	}

	@Override
	public Statement getProxyStatement(JdbcPooledConnection jdbcPooledConnection, Statement statement)
	{
		return null;
	}

	@Override
	public CallableStatement getProxyCallableStatement(JdbcPooledConnection jdbcPooledConnection, CallableStatement statement)
	{
		return null;
	}

	@Override
	public PreparedStatement getProxyPreparedStatement(JdbcPooledConnection jdbcPooledConnection, PreparedStatement statement, LruStatementCache.CacheKey cacheKey)
	{
		return null;
	}

	@Override
	public ResultSet getProxyResultSet(Statement statement, ResultSet resultSet)
	{
		return null;
	}

	@Override
	public XAConnection getProxyXaConnection(Connection connection)
	{
		return null;
	}

	@Override
	public Connection getProxyConnection(LrcXAResource xaResource, Connection connection)
	{
		return null;
	}
}
