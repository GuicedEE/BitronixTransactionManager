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
package bitronix.tm.mock.resource.jdbc;

import java.sql.*;
import java.util.Properties;

/**
 * @author Ludovic Orban
 */
public class MockDriver
		implements Driver
{

	/**
	 * Method connect ...
	 *
	 * @param url
	 * 		of type String
	 * @param info
	 * 		of type Properties
	 *
	 * @return Connection
	 *
	 * @throws SQLException
	 * 		when
	 */
	@Override
	public Connection connect(String url, Properties info) throws SQLException
	{
		return MockitoXADataSource.createMockConnection();
	}

	/**
	 * Method acceptsURL ...
	 *
	 * @param url
	 * 		of type String
	 *
	 * @return boolean
	 */
	@Override
	public boolean acceptsURL(String url)
	{
		return false;
	}

	/**
	 * Method getPropertyInfo ...
	 *
	 * @param url
	 * 		of type String
	 * @param info
	 * 		of type Properties
	 *
	 * @return DriverPropertyInfo[]
	 */
	@Override
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
	{
		return new DriverPropertyInfo[0];
	}

	/**
	 * Method getMajorVersion returns the majorVersion of this MockDriver object.
	 *
	 * @return the majorVersion (type int) of this MockDriver object.
	 */
	@Override
	public int getMajorVersion()
	{
		return 0;
	}

	/**
	 * Method getMinorVersion returns the minorVersion of this MockDriver object.
	 *
	 * @return the minorVersion (type int) of this MockDriver object.
	 */
	@Override
	public int getMinorVersion()
	{
		return 0;
	}

	/**
	 * Method jdbcCompliant ...
	 *
	 * @return boolean
	 */
	@Override
	public boolean jdbcCompliant()
	{
		return false;
	}

	/**
	 * Method getParentLogger returns the parentLogger of this MockDriver object.
	 *
	 * @return the parentLogger (type Logger) of this MockDriver object.
	 *
	 * @throws SQLFeatureNotSupportedException
	 * 		when
	 */
	@Override
	public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException
	{
		throw new SQLFeatureNotSupportedException();
	}
}
