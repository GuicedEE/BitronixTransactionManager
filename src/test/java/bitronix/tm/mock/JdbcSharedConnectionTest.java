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
package bitronix.tm.mock;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.LogDebugCheck;
import bitronix.tm.resource.jdbc.PooledConnectionProxy;

import javax.transaction.Transaction;
import java.sql.Connection;
import java.util.ArrayList;

/**
 * @author Ludovic Orban
 */
public class JdbcSharedConnectionTest
		extends AbstractMockJdbcTest
{
	private final static java.util.logging.Logger log = java.util.logging.Logger.getLogger(NewJdbcProperUsageMockTest.class.toString());

	public void testSharedConnectionMultithreaded() throws Exception
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** Starting testSharedConnectionMultithreaded: getting TM");
		}
		BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
		tm.setTransactionTimeout(120);

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** before begin");
		}
		tm.begin();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** after begin");
		}

		Transaction suspended = tm.suspend();

		ArrayList<Connection> twoConnections = new ArrayList<>();
		Thread thread1 = new Thread(() ->
		                            {
			                            try
			                            {
				                            tm.resume(suspended);
				                            if (LogDebugCheck.isDebugEnabled())
				                            {
					                            log.finer("*** getting connection from DS1");
				                            }
				                            Connection connection = poolingDataSource1.getConnection();
				                            connection.createStatement();
				                            twoConnections.add(connection);
			                            }
			                            catch (Exception e)
			                            {
				                            e.printStackTrace();
				                            fail(e.getMessage());
			                            }
		                            });
		thread1.start();
		thread1.join();

		Thread thread2 = new Thread(() ->
		                            {
			                            try
			                            {
				                            tm.resume(suspended);
				                            if (LogDebugCheck.isDebugEnabled())
				                            {
					                            log.finer("*** getting connection from DS1");
				                            }
				                            Connection connection = poolingDataSource1.getConnection();
				                            connection.createStatement();
				                            twoConnections.add(connection);
				                            tm.commit();
			                            }
			                            catch (Exception e)
			                            {
				                            e.printStackTrace();
				                            fail(e.getMessage());
			                            }
		                            });
		thread2.start();
		thread2.join();

		PooledConnectionProxy handle1 = (PooledConnectionProxy) twoConnections.get(0);
		PooledConnectionProxy handle2 = (PooledConnectionProxy) twoConnections.get(1);
		assertNotSame(handle1.getProxiedDelegate(), handle2.getProxiedDelegate());

	}

	public void testUnSharedConnection() throws Exception
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** Starting testUnSharedConnection: getting TM");
		}
		BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
		tm.setTransactionTimeout(120);

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** before begin");
		}
		tm.begin();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** after begin");
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting connection from DS2");
		}
		Connection connection1 = poolingDataSource2.getConnection();
		// createStatement causes enlistment
		connection1.createStatement();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting second connection from DS2");
		}
		Connection connection2 = poolingDataSource2.getConnection();

		PooledConnectionProxy handle1 = (PooledConnectionProxy) connection1;
		PooledConnectionProxy handle2 = (PooledConnectionProxy) connection2;
		assertNotSame(handle1.getProxiedDelegate(), handle2.getProxiedDelegate());

		connection1.close();
		connection2.close();

		tm.commit();
	}

	public void testSharedConnectionInLocalTransaction() throws Exception
	{

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** Starting testSharedConnectionInLocalTransaction: getting connection from DS1");
		}
		Connection connection1 = poolingDataSource1.getConnection();
		// createStatement causes enlistment
		connection1.createStatement();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting second connection from DS1");
		}
		Connection connection2 = poolingDataSource1.getConnection();

		PooledConnectionProxy handle1 = (PooledConnectionProxy) connection1;
		PooledConnectionProxy handle2 = (PooledConnectionProxy) connection2;
		assertNotSame(handle1.getProxiedDelegate(), handle2.getProxiedDelegate());

		connection1.close();
		connection2.close();
	}

	public void testUnSharedConnectionInLocalTransaction() throws Exception
	{

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** Starting testUnSharedConnectionInLocalTransaction: getting connection from DS2");
		}
		Connection connection1 = poolingDataSource2.getConnection();
		// createStatement causes enlistment
		connection1.createStatement();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting second connection from DS2");
		}
		Connection connection2 = poolingDataSource2.getConnection();

		PooledConnectionProxy handle1 = (PooledConnectionProxy) connection1;
		PooledConnectionProxy handle2 = (PooledConnectionProxy) connection2;
		assertNotSame(handle1.getProxiedDelegate(), handle2.getProxiedDelegate());

		connection1.close();
		connection2.close();
	}

	public void testSharedConnectionInGlobal() throws Exception
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** testSharedConnectionInGlobal: Starting getting TM");
		}
		BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
		tm.setTransactionTimeout(120);

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** before begin");
		}
		tm.begin();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** after begin");
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting connection from DS1");
		}
		Connection connection1 = poolingDataSource1.getConnection();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting second connection from DS1");
		}
		Connection connection2 = poolingDataSource1.getConnection();

		PooledConnectionProxy handle1 = (PooledConnectionProxy) connection1;
		PooledConnectionProxy handle2 = (PooledConnectionProxy) connection2;
		assertSame(handle1.getProxiedDelegate(), handle2.getProxiedDelegate());

		connection1.close();
		connection2.close();

		tm.commit();
	}
}
