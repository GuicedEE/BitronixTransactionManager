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

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

/**
 * @author Brett Wooldridge
 */
public class ResultSetJavaProxy
		extends JavaProxyBase<ResultSet>
{

	private static final Map<String, Method> selfMethodMap = createMethodMap(ResultSetJavaProxy.class);

	private Statement statement;

	/**
	 * Constructor ResultSetJavaProxy creates a new ResultSetJavaProxy instance.
	 *
	 * @param statement
	 * 		of type Statement
	 * @param resultSet
	 * 		of type ResultSet
	 */
	public ResultSetJavaProxy(Statement statement, ResultSet resultSet)
	{
		initialize(statement, resultSet);
	}

	/**
	 * Method initialize ...
	 *
	 * @param statement
	 * 		of type Statement
	 * @param resultSet
	 * 		of type ResultSet
	 */
	void initialize(Statement statement, ResultSet resultSet)
	{
		this.proxy = this;
		this.statement = statement;
		this.delegate = resultSet;
	}

	/**
	 * Constructor ResultSetJavaProxy creates a new ResultSetJavaProxy instance.
	 */
	public ResultSetJavaProxy()
	{
		// Default constructor
	}

	/* Overridden methods of java.sql.ResultSet */

	/**
	 * Method getStatement returns the statement of this ResultSetJavaProxy object.
	 *
	 * @return the statement (type Statement) of this ResultSetJavaProxy object.
	 *
	 * @throws SQLException
	 * 		when
	 */
	public Statement getStatement()
	{
		return statement;
	}

	/* Overridden methods of JavaProxyBase */

	/**
	 * Method getMethodMap returns the methodMap of this ResultSetJavaProxy object.
	 *
	 * @return the methodMap (type Map<String, Method>) of this ResultSetJavaProxy object.
	 */
	@Override
	protected Map<String, Method> getMethodMap()
	{
		return selfMethodMap;
	}
}
