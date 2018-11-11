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
package bitronix.tm.jndi;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.mock.resource.jdbc.MockitoXADataSource;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import junit.framework.TestCase;

import javax.naming.*;
import java.util.Hashtable;


/**
 * @author Ludovic Orban
 */
public class JndiTest
		extends TestCase
{

	private BitronixTransactionManager transactionManager;

	/**
	 * Method setUp ...
	 */
	@Override
	protected void setUp()
	{
		transactionManager = TransactionManagerServices.getTransactionManager();
	}

	/**
	 * Method tearDown ...
	 */
	@Override
	protected void tearDown()
	{
		transactionManager.shutdown();
	}

	/**
	 * Method testNameParser ...
	 *
	 * @throws Exception
	 * 		when
	 */
	public void testNameParser() throws Exception
	{
		BitronixContext bitronixContext = new BitronixContext();
		Name name = bitronixContext.getNameParser("")
		                           .parse("java:comp/UserTransaction");
		assertEquals("java:comp/UserTransaction", name.toString());
		assertSame(BitronixTransactionManager.class, bitronixContext.lookup(name)
		                                                            .getClass());

		name = bitronixContext.getNameParser(new CompositeName())
		                      .parse("java:comp/UserTransaction");
		assertEquals("java:comp/UserTransaction", name.toString());
		assertSame(BitronixTransactionManager.class, bitronixContext.lookup(name)
		                                                            .getClass());
	}

	/**
	 * Method testDefaultUserTransactionAndResources ...
	 *
	 * @throws Exception
	 * 		when
	 */
	public void testDefaultUserTransactionAndResources() throws Exception
	{
		PoolingDataSource pds = new PoolingDataSource();
		pds.setMaxPoolSize(1);
		pds.setClassName(MockitoXADataSource.class.getName());
		pds.setUniqueName("jdbc/pds");
		pds.init();

		Hashtable<String, String> env = new Hashtable<>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, BitronixInitialContextFactory.class.getName());
		Context ctx = new InitialContext(env);

		assertTrue(transactionManager == ctx.lookup("java:comp/UserTransaction"));

		try
		{
			ctx.lookup("aaa");
			fail("expected NameNotFoundException");
		}
		catch (NameNotFoundException ex)
		{
			assertEquals("unable to find a bound object at name 'aaa'", ex.getMessage());
		}

		assertTrue(pds == ctx.lookup("jdbc/pds"));

		ctx.close();

		pds.close();
	}

	/**
	 * Method testSpecialUserTransactionName ...
	 *
	 * @throws Exception
	 * 		when
	 */
	public void testSpecialUserTransactionName() throws Exception
	{
		transactionManager.shutdown();
		TransactionManagerServices.getConfiguration()
		                          .setJndiUserTransactionName("TM");
		transactionManager = TransactionManagerServices.getTransactionManager();

		Hashtable<String, String> env = new Hashtable<>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, BitronixInitialContextFactory.class.getName());
		Context ctx = new InitialContext(env);


		try
		{
			ctx.lookup("java:comp/UserTransaction");
			fail("expected NameNotFoundException");
		}
		catch (NameNotFoundException ex)
		{
			assertEquals("unable to find a bound object at name 'java:comp/UserTransaction'", ex.getMessage());
		}


		assertTrue(transactionManager == ctx.lookup("TM"));

		try
		{
			ctx.lookup("aaa");
			fail("expected NameNotFoundException");
		}
		catch (NameNotFoundException ex)
		{
			assertEquals("unable to find a bound object at name 'aaa'", ex.getMessage());
		}

		ctx.close();
	}

}
