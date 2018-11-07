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

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.LogDebugCheck;
import bitronix.tm.resource.ResourceRegistrar;

import javax.naming.*;
import java.util.Hashtable;

/**
 * Implementation of {@link javax.naming.Context} that allows lookup of transaction manager
 * and registered resources.
 * <p>This implementation is trivial as only the <code>lookup</code> methods are implemented,
 * all the other ones will throw a {@link OperationNotSupportedException}.</p>
 *
 * @author Ludovic Orban
 * @see bitronix.tm.jndi.BitronixInitialContextFactory
 */
public class BitronixContext
		implements Context
{

	private final static java.util.logging.Logger log = java.util.logging.Logger.getLogger(BitronixContext.class.toString());
	private final String userTransactionName;
	private final String synchronizationRegistryName;
	private boolean closed = false;

	public BitronixContext()
	{
		userTransactionName = TransactionManagerServices.getConfiguration()
		                                                .getJndiUserTransactionName();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("binding transaction manager at name '" + userTransactionName + "'");
		}

		synchronizationRegistryName = TransactionManagerServices.getConfiguration()
		                                                        .getJndiTransactionSynchronizationRegistryName();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("binding synchronization registry at name '" + synchronizationRegistryName + "'");
		}
	}

	@Override
	public Object lookup(Name name) throws NamingException
	{
		return lookup(name.toString());
	}

	private void checkClosed() throws ServiceUnavailableException
	{
		if (closed)
		{
			throw new ServiceUnavailableException("context is closed");
		}
	}

	@Override
	public Object lookup(String s) throws NamingException
	{
		checkClosed();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("looking up '" + s + "'");
		}

		Object o;
		if (userTransactionName.equals(s))
		{
			o = TransactionManagerServices.getTransactionManager();
		}
		else if (synchronizationRegistryName.equals(s))
		{
			o = TransactionManagerServices.getTransactionSynchronizationRegistry();
		}
		else
		{
			o = ResourceRegistrar.get(s);
		}

		if (o == null)
		{
			throw new NameNotFoundException("unable to find a bound object at name '" + s + "'");
		}
		return o;
	}

	@Override
	public void bind(Name name, Object o) throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public void bind(String s, Object o) throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public void rebind(Name name, Object o) throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public void rebind(String s, Object o) throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public void unbind(Name name) throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public void unbind(String s) throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public void rename(Name name, Name name1) throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public void rename(String s, String s1) throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public NamingEnumeration<NameClassPair> list(Name name) throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public NamingEnumeration<NameClassPair> list(String s) throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public NamingEnumeration<Binding> listBindings(Name name) throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public NamingEnumeration<Binding> listBindings(String s) throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public void destroySubcontext(Name name) throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public void destroySubcontext(String s) throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public Context createSubcontext(Name name) throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public Context createSubcontext(String s) throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public Object lookupLink(Name name) throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public Object lookupLink(String s) throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public NameParser getNameParser(Name name)
	{
		return BitronixNameParser.INSTANCE;
	}

	@Override
	public NameParser getNameParser(String s)
	{
		return BitronixNameParser.INSTANCE;
	}

	@Override
	public Name composeName(Name name, Name name1) throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public String composeName(String s, String s1) throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public Object addToEnvironment(String s, Object o) throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public Object removeFromEnvironment(String s) throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public Hashtable<?, ?> getEnvironment() throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public void close()
	{
		closed = true;
	}

	@Override
	public String getNameInNamespace() throws NamingException
	{
		throw new OperationNotSupportedException();
	}

	@Override
	public String toString()
	{
		return "a BitronixContext with userTransactionName='" + userTransactionName + "' and synchronizationRegistryName='" + synchronizationRegistryName + "'";
	}

	private final static class BitronixNameParser
			implements NameParser
	{
		private static final BitronixNameParser INSTANCE = new BitronixNameParser();

		@Override
		public Name parse(String name) throws NamingException
		{
			return new CompositeName(name);
		}
	}

}
