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
package bitronix.tm.resource.jdbc.lrc;

import bitronix.tm.resource.jdbc.proxy.JdbcProxyFactory;
import bitronix.tm.utils.ClassLoaderUtils;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;

/**
 * XADataSource implementation for a non-XA JDBC resource emulating XA with Last Resource Commit.
 *
 * @author Ludovic Orban
 * @author Brett Wooldridge
 */
@SuppressWarnings("unused")
public class LrcXADataSource
		implements XADataSource
{

	private volatile int loginTimeout;
	private volatile String driverClassName;
	private volatile String url;
	private volatile String user;
	private volatile String password;

	/**
	 * Constructor LrcXADataSource creates a new LrcXADataSource instance.
	 */
	public LrcXADataSource()
	{
		//No config required
	}

	/**
	 * Method getDriverClassName returns the driverClassName of this LrcXADataSource object.
	 *
	 * @return the driverClassName (type String) of this LrcXADataSource object.
	 */
	public String getDriverClassName()
	{
		return driverClassName;
	}

	/**
	 * Method setDriverClassName sets the driverClassName of this LrcXADataSource object.
	 *
	 * @param driverClassName
	 * 		the driverClassName of this LrcXADataSource object.
	 */
	public void setDriverClassName(String driverClassName)
	{
		this.driverClassName = driverClassName;
	}

	/**
	 * Method getUrl returns the url of this LrcXADataSource object.
	 *
	 * @return the url (type String) of this LrcXADataSource object.
	 */
	public String getUrl()
	{
		return url;
	}

	/**
	 * Method setUrl sets the url of this LrcXADataSource object.
	 *
	 * @param url
	 * 		the url of this LrcXADataSource object.
	 */
	public void setUrl(String url)
	{
		this.url = url;
	}

	/**
	 * Method getUser returns the user of this LrcXADataSource object.
	 *
	 * @return the user (type String) of this LrcXADataSource object.
	 */
	public String getUser()
	{
		return user;
	}

	/**
	 * Method setUser sets the user of this LrcXADataSource object.
	 *
	 * @param user
	 * 		the user of this LrcXADataSource object.
	 */
	public void setUser(String user)
	{
		this.user = user;
	}

	/**
	 * Method getPassword returns the password of this LrcXADataSource object.
	 *
	 * @return the password (type String) of this LrcXADataSource object.
	 */
	public String getPassword()
	{
		return password;
	}

	/**
	 * Method setPassword sets the password of this LrcXADataSource object.
	 *
	 * @param password
	 * 		the password of this LrcXADataSource object.
	 */
	public void setPassword(String password)
	{
		this.password = password;
	}

	/**
	 * Method getXAConnection returns the XAConnection of this LrcXADataSource object.
	 *
	 * @return the XAConnection (type XAConnection) of this LrcXADataSource object.
	 *
	 * @throws SQLException
	 * 		when
	 */
	@Override
	public XAConnection getXAConnection() throws SQLException
	{
		try
		{
			Class<?> driverClazz = ClassLoaderUtils.loadClass(driverClassName);
			Driver driver = (Driver) driverClazz.getDeclaredConstructor()
			                                    .newInstance();
			Properties props = new Properties();
			if (user != null)
			{
				props.setProperty("user", user);
			}
			if (password != null)
			{
				props.setProperty("password", password);
			}
			Connection connection = driver.connect(url, props);
			return JdbcProxyFactory.INSTANCE.getProxyXaConnection(connection);
		}
		catch (Exception ex)
		{
			throw new SQLException("unable to connect to non-XA resource " + driverClassName, ex);
		}
	}

	/**
	 * Method getXAConnection ...
	 *
	 * @param user
	 * 		of type String
	 * @param password
	 * 		of type String
	 *
	 * @return XAConnection
	 *
	 * @throws SQLException
	 * 		when
	 */
	@Override
	public XAConnection getXAConnection(String user, String password) throws SQLException
	{
		try
		{
			Class<?> driverClazz = ClassLoaderUtils.loadClass(driverClassName);
			Driver driver = (Driver) driverClazz.getDeclaredConstructor()
			                                    .newInstance();
			Properties props = new Properties();
			props.setProperty("user", user);
			props.setProperty("password", password);
			Connection connection = driver.connect(url, props);
			return JdbcProxyFactory.INSTANCE.getProxyXaConnection(connection);
		}
		catch (Exception ex)
		{
			throw new SQLException("unable to connect to non-XA resource " + driverClassName, ex);
		}
	}

	/**
	 * Method getLogWriter returns the logWriter of this LrcXADataSource object.
	 *
	 * @return the logWriter (type PrintWriter) of this LrcXADataSource object.
	 *
	 * @throws SQLException
	 * 		when
	 */
	@Override
	public PrintWriter getLogWriter() throws SQLException
	{
		return null;
	}

	/**
	 * Method setLogWriter sets the logWriter of this LrcXADataSource object.
	 *
	 * @param out
	 * 		the logWriter of this LrcXADataSource object.
	 *
	 * @throws SQLException
	 * 		when
	 */
	@Override
	public void setLogWriter(PrintWriter out) throws SQLException
	{
		//Nothing required
	}

	/**
	 * Method toString ...
	 *
	 * @return String
	 */
	@Override
	public String toString()
	{
		return "a JDBC LrcXADataSource on " + driverClassName + " with URL " + url;
	}

	/**
	 * Method getParentLogger returns the parentLogger of this LrcXADataSource object.
	 *
	 * @return the parentLogger (type Logger) of this LrcXADataSource object.
	 *
	 * @throws SQLFeatureNotSupportedException
	 * 		when
	 */
	@Override
	public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException
	{
		throw new SQLFeatureNotSupportedException();
	}

	/**
	 * Method getLoginTimeout returns the loginTimeout of this LrcXADataSource object.
	 *
	 * @return the loginTimeout (type int) of this LrcXADataSource object.
	 *
	 * @throws SQLException
	 * 		when
	 */
	@Override
	public int getLoginTimeout() throws SQLException
	{
		return loginTimeout;
	}


	/**
	 * Method setLoginTimeout sets the loginTimeout of this LrcXADataSource object.
	 *
	 * @param seconds
	 * 		the loginTimeout of this LrcXADataSource object.
	 *
	 * @throws SQLException
	 * 		when
	 */
	@Override
	public void setLoginTimeout(int seconds) throws SQLException
	{
		this.loginTimeout = seconds;
	}


}
