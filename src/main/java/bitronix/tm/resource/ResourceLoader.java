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
package bitronix.tm.resource;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.LogDebugCheck;
import bitronix.tm.resource.common.XAResourceProducer;
import bitronix.tm.utils.ClassLoaderUtils;
import bitronix.tm.utils.InitializationException;
import bitronix.tm.utils.PropertyUtils;
import bitronix.tm.utils.Service;

import javax.jms.XAConnectionFactory;
import javax.sql.XADataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Level;

/**
 * XA resources pools configurator &amp; loader.
 * <p>{@link ResourceLoader} relies on the optional <code>bitronix.tm.resource.configuration</code> propery to load the
 * JDBC datasources ({@link bitronix.tm.resource.jdbc.PoolingDataSource}) and JMS connection factories
 * ({@link bitronix.tm.resource.jms.PoolingConnectionFactory}) configuration file and create the resources.</p>
 * <p>When <code>bitronix.tm.resource.configuration</code> is not specified, ResourceLoader is disabled and resources
 * should be manually created.</p>
 *
 * @author Ludovic Orban
 */
public class ResourceLoader
		implements Service
{

	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(ResourceLoader.class.toString());

	private static final String JDBC_RESOURCE_CLASSNAME = "bitronix.tm.resource.jdbc.PoolingDataSource";
	private static final String JMS_RESOURCE_CLASSNAME = "bitronix.tm.resource.jms.PoolingConnectionFactory";

	private final Map<String, XAResourceProducer> resourcesByUniqueName = new HashMap<>();

	/**
	 * Constructor ResourceLoader creates a new ResourceLoader instance.
	 */
	public ResourceLoader()
	{
		//No config required
	}

	/**
	 * Get a Map with the configured uniqueName as key and {@link XAResourceProducer} as value.
	 *
	 * @return a Map using the uniqueName as key and {@link XAResourceProducer} as value.
	 */
	public Map<String, XAResourceProducer> getResources()
	{
		return resourcesByUniqueName;
	}

	/**
	 * Initialize the ResourceLoader and load the resources configuration file specified in
	 * <code>bitronix.tm.resource.configuration</code> property.
	 *
	 * @return the number of resources which failed to initialize.
	 */
	public int init()
	{
		String filename = TransactionManagerServices.getConfiguration()
		                                            .getResourceConfigurationFilename();
		if (filename != null)
		{
			if (!new File(filename).exists())
			{
				throw new ResourceConfigurationException(
						"cannot find resources configuration file '" + filename + "', missing or invalid value of property 'bitronix.tm.resource.configuration'");
			}
			log.info("reading resources configuration from " + filename);
			return init(filename);
		}
		else
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("no resource configuration file specified");
			}
			return 0;
		}
	}

	/**
	 * Read the resources properties file and create {@link XAResourceProducer} accordingly.
	 *
	 * @param propertiesFilename
	 * 		the name of the properties file to load.
	 *
	 * @return the number of resources which failed to initialize.
	 */
	private int init(String propertiesFilename)
	{
		try (FileInputStream fis = new FileInputStream(propertiesFilename))
		{
			Properties properties;
			properties = new Properties();
			properties.load(fis);
			return initXAResourceProducers(properties);
		}
		catch (IOException ex)
		{
			throw new InitializationException("cannot create resource loader", ex);
		}
	}

	/*
	 * Internal impl.
	 */

	/**
	 * Initialize {@link XAResourceProducer}s given a set of properties.
	 *
	 * @param properties
	 * 		the properties to use for initialization.
	 *
	 * @return the number of resources which failed to initialize.
	 */
	int initXAResourceProducers(Properties properties)
	{
		Map<String, List<PropertyPair>> entries = buildConfigurationEntriesMap(properties);
		int errorCount = 0;

		for (Map.Entry<String, List<PropertyPair>> entry : entries.entrySet())
		{
			String uniqueName = entry.getKey();
			List<PropertyPair> propertyPairs = entry.getValue();
			XAResourceProducer producer = buildXAResourceProducer(uniqueName, propertyPairs);

			if (ResourceRegistrar.get(producer.getUniqueName()) != null)
			{
				if (LogDebugCheck.isDebugEnabled())
				{
					log.finer("resource already registered, skipping it:" + producer.getUniqueName());
				}
				continue;
			}

			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("creating resource " + producer);
			}
			try
			{
				producer.init();
			}
			catch (ResourceConfigurationException ex)
			{
				log.log(Level.WARNING, "unable to create resource with unique name " + producer.getUniqueName(), ex);
				producer.close();
				errorCount++;
			}

			resourcesByUniqueName.put(producer.getUniqueName(), producer);
		}

		return errorCount;
	}

	/**
	 * Create a map using the configured resource name as the key and a List of PropertyPair objects as the value.
	 *
	 * @param properties
	 * 		object to analyze.
	 *
	 * @return the built map.
	 */
	private Map<String, List<PropertyPair>> buildConfigurationEntriesMap(Properties properties)
	{
		Map<String, List<PropertyPair>> entries = new HashMap<>();
		for (Map.Entry<Object, Object> entry : properties.entrySet())
		{
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();

			if (key.startsWith("resource."))
			{
				String[] keyParts = key.split("\\.");
				if (keyParts.length < 3)
				{
					log.warning("ignoring invalid entry in configuration file: " + key);
					continue;
				}
				String configuredName = keyParts[1];
				StringBuilder propertyName = new StringBuilder(keyParts[2]);
				if (keyParts.length > 3)
				{
					for (int i = 3; i < keyParts.length; i++)
					{
						propertyName.append(".")
						            .append(keyParts[i]);
					}
				}
				List<PropertyPair> pairs = entries.computeIfAbsent(configuredName, k -> new ArrayList<>());
				pairs.add(new PropertyPair(propertyName.toString(), value));
			}
		}
		return entries;
	}

	/**
	 * Build a populated {@link XAResourceProducer} out of a list of property pairs and the config name.
	 *
	 * @param configuredName
	 * 		index name of the config file.
	 * @param propertyPairs
	 * 		the properties attached to this index.
	 *
	 * @return a populated {@link XAResourceProducer}.
	 *
	 * @throws ResourceConfigurationException
	 * 		if the {@link XAResourceProducer} cannot be built.
	 */
	private XAResourceProducer buildXAResourceProducer(String configuredName, List<PropertyPair> propertyPairs)
	{
		String lastPropertyName = "className";
		try
		{
			XAResourceProducer producer = createBean(configuredName, propertyPairs);

			for (PropertyPair propertyPair : propertyPairs)
			{
				lastPropertyName = propertyPair.getName();
				String propertyValue = propertyPair.getValue();

				PropertyUtils.setProperty(producer, lastPropertyName, propertyValue);
			}
			if (producer.getUniqueName() == null)
			{
				throw new ResourceConfigurationException("missing mandatory property [uniqueName] of resource [" + configuredName + "] in resources configuration file");
			}

			return producer;
		}
		catch (ResourceConfigurationException ex)
		{
			throw ex;
		}
		catch (Exception ex)
		{
			throw new ResourceConfigurationException(
					"cannot configure resource for configuration entries with name [" + configuredName + "]" + " - failing property is [" + lastPropertyName + "]", ex);
		}
	}

	/**
	 * Create an unpopulated, uninitialized {@link XAResourceProducer} instance depending on the className value.
	 *
	 * @param configuredName
	 * 		the properties configured name.
	 * @param propertyPairs
	 * 		a list of {@link PropertyPair}s.
	 *
	 * @return a {@link XAResourceProducer}.
	 *
	 * @throws ClassNotFoundException
	 * 		if the {@link XAResourceProducer} cannot be instantiated.
	 * @throws IllegalAccessException
	 * 		if the {@link XAResourceProducer} cannot be instantiated.
	 * @throws InstantiationException
	 * 		if the {@link XAResourceProducer} cannot be instantiated.
	 */
	private XAResourceProducer createBean(String configuredName, List<PropertyPair> propertyPairs) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException
	{
		for (PropertyPair propertyPair : propertyPairs)
		{
			if ("className".equals(propertyPair.getName()))
			{
				String className = propertyPair.getValue();
				XAResourceProducer producer = instantiate(className);
				if (producer == null)
				{
					throw new ResourceConfigurationException("property [className] " +
					                                         "of resource [" + configuredName + "] in resources configuration file " +
					                                         "must be the name of a class implementing either javax.sql.XADataSource or javax.jms.XAConnectionFactory");
				}
				return producer;
			}
		}
		throw new ResourceConfigurationException("missing mandatory property [className] for resource [" + configuredName + "] in resources configuration file");
	}

	/**
	 * Create an unitialized {@link XAResourceProducer} implementation which depends on the XA resource class name.
	 *
	 * @param xaResourceClassName
	 * 		an XA resource class name.
	 *
	 * @return a {@link XAResourceProducer} implementation.
	 *
	 * @throws ClassNotFoundException
	 * 		if the {@link XAResourceProducer} cannot be instantiated.
	 * @throws IllegalAccessException
	 * 		if the {@link XAResourceProducer} cannot be instantiated.
	 * @throws InstantiationException
	 * 		if the {@link XAResourceProducer} cannot be instantiated.
	 */
	@SuppressWarnings("unchecked")
	private static XAResourceProducer instantiate(String xaResourceClassName) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException
	{
		Class<?> clazz = ClassLoaderUtils.loadClass(xaResourceClassName);

		// resource classes are instantiated via reflection so that there is no hard class binding between this internal
		// transaction manager service and 3rd party libraries like the JMS ones.
		// This allows using the TM with a 100% JDBC application without requiring JMS libraries.

		if (XADataSource.class.isAssignableFrom(clazz))
		{
			return (XAResourceProducer) ClassLoaderUtils.loadClass(JDBC_RESOURCE_CLASSNAME)
			                                            .getDeclaredConstructor()
			                                            .newInstance();
		}
		else if (XAConnectionFactory.class.isAssignableFrom(clazz))
		{
			return (XAResourceProducer) ClassLoaderUtils.loadClass(JMS_RESOURCE_CLASSNAME)
			                                            .getDeclaredConstructor()
			                                            .newInstance();
		}
		else
		{
			return null;
		}
	}

	/**
	 * Shutdown the service and free all held resources.
	 */
	@Override
	public synchronized void shutdown()
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("resource loader has registered " + resourcesByUniqueName.entrySet()
			                                                                   .size() + " resource(s), unregistering them now");
		}
		for (Map.Entry<String, XAResourceProducer> entry : resourcesByUniqueName.entrySet())
		{
			XAResourceProducer producer = entry.getValue();
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("closing " + producer);
			}
			try
			{
				producer.close();
			}
			catch (Exception ex)
			{
				log.log(Level.WARNING, "error closing resource " + producer, ex);
			}
		}
		resourcesByUniqueName.clear();
	}

	private final class PropertyPair
	{
		private final String name;
		private final String value;

		/**
		 * Constructor PropertyPair creates a new PropertyPair instance.
		 *
		 * @param key
		 * 		of type String
		 * @param value
		 * 		of type String
		 */
		public PropertyPair(String key, String value)
		{
			this.name = key;
			this.value = value;
		}

		/**
		 * Method getName returns the name of this PropertyPair object.
		 *
		 * @return the name (type String) of this PropertyPair object.
		 */
		public String getName()
		{
			return name;
		}

		/**
		 * Method getValue returns the value of this PropertyPair object.
		 *
		 * @return the value (type String) of this PropertyPair object.
		 */
		public String getValue()
		{
			return value;
		}

		/**
		 * Method toString ...
		 *
		 * @return String
		 */
		@Override
		public String toString()
		{
			return name + "/" + value;
		}
	}

}
