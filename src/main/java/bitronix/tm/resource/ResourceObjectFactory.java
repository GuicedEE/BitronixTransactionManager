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

import bitronix.tm.internal.LogDebugCheck;

import javax.naming.*;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

/**
 * {@link bitronix.tm.resource.common.XAResourceProducer} object factory for JNDI references.
 *
 * @author Ludovic Orban
 * @see bitronix.tm.resource.common.ResourceBean
 */
public class ResourceObjectFactory
		implements ObjectFactory
{

	private final static java.util.logging.Logger log = java.util.logging.Logger.getLogger(ResourceObjectFactory.class.toString());

	/**
	 * Method getObjectInstance ...
	 *
	 * @param obj
	 * 		of type Object
	 * @param jndiNameObject
	 * 		of type Name
	 * @param nameCtx
	 * 		of type Context
	 * @param environment
	 * 		of type Hashtable<?, ?>
	 *
	 * @return Object
	 *
	 * @throws Exception
	 * 		when
	 */
	@Override
	public Object getObjectInstance(Object obj, Name jndiNameObject, Context nameCtx, Hashtable<?, ?> environment) throws Exception
	{
		Reference ref = (Reference) obj;
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("referencing resource with reference of type " + ref.getClass());
		}

		RefAddr refAddr = ref.get("uniqueName");
		if (refAddr == null)
		{
			throw new NamingException("no 'uniqueName' RefAddr found");
		}
		Object content = refAddr.getContent();
		if (!(content instanceof String))
		{
			throw new NamingException("'uniqueName' RefAddr content is not of type java.lang.String");
		}
		String uniqueName = (String) content;

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("getting registered resource with uniqueName '" + uniqueName + "'");
		}
		Referenceable resource = ResourceRegistrar.get(uniqueName);
		if (resource == null)
		{
			throw new NamingException("no resource registered with uniqueName '" + uniqueName + "', available resources: " + ResourceRegistrar.getResourcesUniqueNames());
		}

		return resource;
	}

}
