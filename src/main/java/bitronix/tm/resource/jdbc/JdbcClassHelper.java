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
package bitronix.tm.resource.jdbc;

import bitronix.tm.internal.LogDebugCheck;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JdbcClassHelper
{

	private final static java.util.logging.Logger log = java.util.logging.Logger.getLogger(JdbcClassHelper.class.toString());

	private final static int DETECTION_TIMEOUT = 5; // seconds

	private static final Map<Class<Connection>, Integer> connectionClassVersions = new ConcurrentHashMap<>();
	private static final Map<Class<? extends Connection>, Method> isValidMethods = new ConcurrentHashMap<>();

	/**
	 * Method getIsValidMethod ...
	 *
	 * @param connection
	 * 		of type Connection
	 *
	 * @return Method
	 */
	public static Method getIsValidMethod(Connection connection)
	{
		detectJdbcVersion(connection);
		return isValidMethods.get(connection.getClass());
	}

	/**
	 * Method detectJdbcVersion ...
	 *
	 * @param connection
	 * 		of type Connection
	 *
	 * @return int
	 */
	public static int detectJdbcVersion(Connection connection)
	{
		@SuppressWarnings("unchecked")
		Class<Connection> connectionClass = (Class<Connection>) connection.getClass();

		Integer jdbcVersionDetected = connectionClassVersions.get(connectionClass);
		if (jdbcVersionDetected != null)
		{
			return jdbcVersionDetected;
		}

		try
		{
			Method isValidMethod = connectionClass.getMethod("isValid", Integer.TYPE);
			isValidMethod.invoke(connection, DETECTION_TIMEOUT); // test invoke
			jdbcVersionDetected = 4;
			isValidMethods.put(connectionClass, isValidMethod);
		}
		catch (Exception | AbstractMethodError ex)
		{
			jdbcVersionDetected = 3;
		}

		connectionClassVersions.put(connectionClass, jdbcVersionDetected);
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("detected JDBC connection class '" + connectionClass + "' is version " + jdbcVersionDetected + " type");
		}

		return jdbcVersionDetected;
	}


}
