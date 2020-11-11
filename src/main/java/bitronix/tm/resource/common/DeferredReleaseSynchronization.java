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

import bitronix.tm.internal.LogDebugCheck;
import bitronix.tm.resource.common.XAStatefulHolder.State;

import jakarta.transaction.Synchronization;

/**
 * {@link Synchronization} used to release a {@link XAStatefulHolder} object after 2PC has executed.
 *
 * @author Ludovic Orban
 */
public class DeferredReleaseSynchronization
		implements Synchronization
{

	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(DeferredReleaseSynchronization.class.toString());

	private final XAStatefulHolder xaStatefulHolder;

	/**
	 * Constructor DeferredReleaseSynchronization creates a new DeferredReleaseSynchronization instance.
	 *
	 * @param xaStatefulHolder
	 * 		of type XAStatefulHolder
	 */
	public DeferredReleaseSynchronization(XAStatefulHolder xaStatefulHolder)
	{
		this.xaStatefulHolder = xaStatefulHolder;
	}

	/**
	 * Method getXAStatefulHolder returns the XAStatefulHolder of this DeferredReleaseSynchronization object.
	 *
	 * @return the XAStatefulHolder (type XAStatefulHolder) of this DeferredReleaseSynchronization object.
	 */
	public XAStatefulHolder getXAStatefulHolder()
	{
		return xaStatefulHolder;
	}

	/**
	 * Method beforeCompletion ...
	 */
	@Override
	public void beforeCompletion()
	{
		//Nothing required
	}

	/**
	 * Method afterCompletion ...
	 *
	 * @param status
	 * 		of type int
	 */
	@Override
	public void afterCompletion(int status)
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("DeferredReleaseSynchronization requeuing " + xaStatefulHolder);
		}

		// set this connection's state back to IN_POOL
		xaStatefulHolder.setState(State.IN_POOL);

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("DeferredReleaseSynchronization requeued " + xaStatefulHolder);
		}
	}

	/**
	 * Method toString ...
	 *
	 * @return String
	 */
	@Override
	public String toString()
	{
		return "a DeferredReleaseSynchronization of " + xaStatefulHolder;
	}
}
