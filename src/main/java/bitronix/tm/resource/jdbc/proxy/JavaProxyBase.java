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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Brett Wooldridge
 */
public abstract class JavaProxyBase<T>
		implements InvocationHandler
{

	private static final Map<Method, String> methodKeyMap = new ConcurrentHashMap<>();

	protected Object proxy;

	protected T delegate;

	/**
	 * Method createMethodMap ...
	 *
	 * @param clazz
	 * 		of type Class ?
	 *
	 * @return Map String, Method
	 */
	protected static Map<String, Method> createMethodMap(Class<?> clazz)
	{
		HashMap<String, Method> selfMethodMap = new HashMap<>();
		for (Method method : clazz.getDeclaredMethods())
		{
			selfMethodMap.put(getMethodKey(method), method);
		}
		return selfMethodMap;
	}

	/**
	 * Method getMethodKey ...
	 *
	 * @param method
	 * 		of type Method
	 *
	 * @return String
	 */
	protected static String getMethodKey(Method method)
	{
		String key = methodKeyMap.get(method);
		if (key != null)
		{
			return key;
		}

		StringBuilder sb = new StringBuilder();
		sb.append(method.getReturnType()
		                .getName())
		  .append(method.getName());
		for (Class<?> type : method.getParameterTypes())
		{
			sb.append(type.getName());
		}
		key = sb.toString();
		methodKeyMap.put(method, key);
		return key;
	}

	/**
	 * Method isWrapperFor ...
	 *
	 * @param obj
	 * 		of type Object
	 * @param param
	 * 		of type Class ?
	 *
	 * @return boolean
	 */
	protected static boolean isWrapperFor(Object obj, Class<?> param)
	{
		try
		{
			Method isWrapperForMethod = obj.getClass()
			                               .getMethod("isWrapperFor", Class.class);
			return (Boolean) isWrapperForMethod.invoke(obj, param);
		}
		catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex)
		{
			throw new UnsupportedOperationException("isWrapperFor is not supported", ex);
		}
	}

	/**
	 * Method unwrap ...
	 *
	 * @param obj
	 * 		of type Object
	 * @param param
	 * 		of type Class T
	 *
	 * @return T
	 */
	@SuppressWarnings("unchecked")
	protected static <T> T unwrap(Object obj, Class<T> param)
	{
		try
		{
			Method unwrapMethod = obj.getClass()
			                         .getMethod("unwrap", Class.class);
			return (T) unwrapMethod.invoke(obj, param);
		}
		catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex)
		{
			throw new UnsupportedOperationException("unwrap is not supported", ex);
		}
	}

	/**
	 * Method getProxy returns the proxy of this JavaProxyBase object.
	 *
	 * @return the proxy (type T) of this JavaProxyBase object.
	 */
	@SuppressWarnings("unchecked")
	protected T getProxy()
	{
		return (T) proxy;
	}

	/**
	 * Method invoke ...
	 *
	 * @param proxy
	 * 		of type Object
	 * @param method
	 * 		of type Method
	 * @param args
	 * 		of type Object[]
	 *
	 * @return Object
	 *
	 * @throws Throwable
	 * 		when
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
	{
		if (Proxy.isProxyClass(proxy.getClass()))
		{
			this.proxy = proxy;
		}

		try
		{
			Method ourMethod = getMethodMap().get(getMethodKey(method));
			if (ourMethod != null)
			{
				return ourMethod.invoke(this, args);
			}

			return method.invoke(delegate, args);
		}
		catch (InvocationTargetException ite)
		{
			throw ite.getTargetException();
		}
	}

	/**
	 * Method getMethodMap returns the methodMap of this JavaProxyBase object.
	 *
	 * @return the methodMap (type Map String, Method ) of this JavaProxyBase object.
	 */
	protected abstract Map<String, Method> getMethodMap();

}
