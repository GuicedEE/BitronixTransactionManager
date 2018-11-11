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
package bitronix.tm.twopc.executor;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.XAResourceHolderState;

import javax.transaction.xa.XAException;

/**
 * Abstract job definition executable by the 2PC thread pools.
 *
 * @author Ludovic Orban
 */
public abstract class Job
		implements Runnable
{
	private final XAResourceHolderState resourceHolder;
	protected volatile XAException xaException;
	protected volatile RuntimeException runtimeException;
	private volatile Object future;

	/**
	 * Constructor Job creates a new Job instance.
	 *
	 * @param resourceHolder
	 * 		of type XAResourceHolderState
	 */
	public Job(XAResourceHolderState resourceHolder)
	{
		this.resourceHolder = resourceHolder;
	}

	/**
	 * Method getResource returns the resource of this Job object.
	 *
	 * @return the resource (type XAResourceHolderState) of this Job object.
	 */
	public XAResourceHolderState getResource()
	{
		return resourceHolder;
	}

	/**
	 * Method getXAException returns the XAException of this Job object.
	 *
	 * @return the XAException (type XAException) of this Job object.
	 */
	public XAException getXAException()
	{
		return xaException;
	}

	/**
	 * Method getRuntimeException returns the runtimeException of this Job object.
	 *
	 * @return the runtimeException (type RuntimeException) of this Job object.
	 */
	public RuntimeException getRuntimeException()
	{
		return runtimeException;
	}

	/**
	 * Method getFuture returns the future of this Job object.
	 *
	 * @return the future (type Object) of this Job object.
	 */
	public Object getFuture()
	{
		return future;
	}

	/**
	 * Method setFuture sets the future of this Job object.
	 *
	 * @param future
	 * 		the future of this Job object.
	 */
	public void setFuture(Object future)
	{
		this.future = future;
	}

	/**
	 * Method run ...
	 */
	@Override
	public final void run()
	{
		String oldThreadName = null;
		if (TransactionManagerServices.getConfiguration()
		                              .isAsynchronous2Pc())
		{
			oldThreadName = Thread.currentThread()
			                      .getName();
			Thread.currentThread()
			      .setName("bitronix-2pc [ " +
			               resourceHolder.getXid()
			                             .toString() +
			               " ]");
		}
		execute();
		if (oldThreadName != null)
		{
			Thread.currentThread()
			      .setName(oldThreadName);
		}
	}

	/**
	 * Method execute ...
	 */
	protected abstract void execute();
}
