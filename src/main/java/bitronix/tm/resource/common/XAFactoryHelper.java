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
package bitronix.tm.resource.common;

import bitronix.tm.internal.BitronixSystemException;
import bitronix.tm.internal.LogDebugCheck;
import bitronix.tm.utils.ClassLoaderUtils;
import bitronix.tm.utils.CryptoEngine;
import bitronix.tm.utils.PropertyUtils;

import java.util.Map;

/**
 * @author Brett Wooldridge
 */
final class XAFactoryHelper
{
	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(XAFactoryHelper.class.toString());

	private static final String PASSWORD_PROPERTY_NAME = "password";

	/**
	 * Constructor XAFactoryHelper creates a new XAFactoryHelper instance.
	 */
	private XAFactoryHelper()
	{
		// This class is not instantiable.
	}

	/**
	 * Method createXAFactory ...
	 *
	 * @param bean
	 * 		of type ResourceBean
	 *
	 * @return Object
	 *
	 * @throws Exception
	 * 		when
	 */
	static Object createXAFactory(ResourceBean bean) throws Exception
	{
		String className = bean.getClassName();
		if (className == null)
		{
			throw new IllegalArgumentException("className cannot be null");
		}
		Class<?> xaFactoryClass = ClassLoaderUtils.loadClass(className);
		Object xaFactory = xaFactoryClass.getDeclaredConstructor()
		                                 .newInstance();

		for (Map.Entry<Object, Object> entry : bean.getDriverProperties()
		                                           .entrySet())
		{
			String name = (String) entry.getKey();
			Object value = entry.getValue();

			if (name.endsWith(PASSWORD_PROPERTY_NAME))
			{
				value = decrypt(value.toString());
			}

			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("setting vendor property '" + name + "' to '" + value + "'");
			}
			PropertyUtils.setProperty(xaFactory, name, value);
		}
		return xaFactory;
	}

	/**
	 * Method decrypt ...
	 *
	 * @param resourcePassword
	 * 		of type String
	 *
	 * @return String
	 *
	 * @throws Exception
	 * 		when
	 */
	private static String decrypt(String resourcePassword) throws BitronixSystemException
	{
		int startIdx = resourcePassword.indexOf('{');
		int endIdx = resourcePassword.indexOf('}');

		if (startIdx != 0 || endIdx == -1)
		{
			return resourcePassword;
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("resource password is encrypted, decrypting " + resourcePassword);
		}
		String toScan = resourcePassword.substring(endIdx + 1);
		String returned;
		try
		{
			returned = CryptoEngine.decrypt(toScan);
		}
		catch (Exception e)
		{
			throw new BitronixSystemException("Unable to decrypt field", e);
		}
		if (returned.charAt(0) == '\u0000')
		{
			returned = returned.substring(8);
		}
		return returned;
	}
}
