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
package bitronix.tm.utils;

import bitronix.tm.TransactionManagerServices;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Smoke test for ManagementRegistrar.
 *
 * @author Juergen_Kellerer, 2011-08-24
 */
public class ManagementRegistrarTest
{

	final String objectName = "bitronix.somename:type=TestBean";
	final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

	/**
	 * Method assertJMXDefaultsAreAsyncAndEnabled ...
	 */
	@Before
	public void assertJMXDefaultsAreAsyncAndEnabled()
	{
		assertFalse(TransactionManagerServices.getConfiguration()
		                                      .isDisableJmx());
		assertFalse(TransactionManagerServices.getConfiguration()
		                                      .isSynchronousJmxRegistration());
	}

	/**
	 * Method tearDown ...
	 */
	@After
	public void tearDown()
	{
		ManagementRegistrar.unregister(objectName);
		ManagementRegistrar.normalizeAndRunQueuedCommands();
	}

	/**
	 * Method testCanRegister ...
	 *
	 * @throws Exception
	 * 		when
	 */
	@Test
	public void testCanRegister() throws Exception
	{
		int iterations = 100000;
		List<TestBean> beans = new ArrayList<>(iterations);

		for (int i = 0; i < iterations; i++)
		{
			if (i > 0)
			{
				ManagementRegistrar.unregister(objectName);
			}

			TestBean testBean = new TestBean("#" + i);
			beans.add(testBean); // holding a hard reference to ensure the instances are not GCed.
			ManagementRegistrar.register(objectName, testBean);
		}

		ManagementRegistrar.normalizeAndRunQueuedCommands();
		assertEquals(beans.get(beans.size() - 1)
		                  .getName(), mBeanServer.getAttribute(new ObjectName(objectName), "Name"));
	}

	/**
	 * Method testCanUnregister ...
	 *
	 * @throws Exception
	 * 		when
	 */
	@Test(expected = InstanceNotFoundException.class)
	public void testCanUnregister() throws Exception
	{
		TestBean testBean = new TestBean("1");
		ManagementRegistrar.register(objectName, testBean);
		ManagementRegistrar.unregister(objectName);
		ManagementRegistrar.normalizeAndRunQueuedCommands();

		mBeanServer.getAttribute(new ObjectName(objectName), "Name");
	}

	public interface TestBeanMBean
	{
		/**
		 * Method getName returns the name of this TestBeanMBean object.
		 *
		 * @return the name (type String) of this TestBeanMBean object.
		 */
		String getName();
	}

	public static class TestBean
			implements TestBeanMBean
	{

		String name;

		/**
		 * Constructor TestBean creates a new TestBean instance.
		 *
		 * @param name
		 * 		of type String
		 */
		public TestBean(String name)
		{
			this.name = name;
		}

		/**
		 * Method getName returns the name of this TestBean object.
		 *
		 * @return the name (type String) of this TestBean object.
		 */
		@Override
		public String getName()
		{
			return name;
		}
	}
}
