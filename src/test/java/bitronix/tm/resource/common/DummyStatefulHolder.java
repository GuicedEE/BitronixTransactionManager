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

import java.util.Date;
import java.util.List;

/**
 * Dummy XAStatefulHolder class, to get the generics right.
 *
 * @author Chris Rankin
 */
public class DummyStatefulHolder
		implements XAStatefulHolder<DummyStatefulHolder>
{

	/**
	 * Get the current resource state.
	 * <p>This method is thread-safe.</p>
	 *
	 * @return the current resource state.
	 */
	@Override
	public State getState()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Set the current resource state.
	 * <p>This method is thread-safe.</p>
	 *
	 * @param state
	 * 		the current resource state.
	 */
	@Override
	public void setState(State state)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Method addStateChangeEventListener ...
	 *
	 * @param listener
	 * 		of type StateChangeListener<DummyStatefulHolder>
	 */
	@Override
	public void addStateChangeEventListener(StateChangeListener<DummyStatefulHolder> listener)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Method removeStateChangeEventListener ...
	 *
	 * @param listener
	 * 		of type StateChangeListener<DummyStatefulHolder>
	 */
	@Override
	public void removeStateChangeEventListener(StateChangeListener<DummyStatefulHolder> listener)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Get the list of {@link XAResourceHolder}s created by this
	 * {@link XAStatefulHolder} that are still open.
	 * <p>This method is thread-safe.</p>
	 *
	 * @return the list of {@link bitronix.tm.resource.common.XAResourceHolder}s created by this
	 * 		{@link XAStatefulHolder} that are still open.
	 */
	@Override
	public List<? extends XAResourceHolder<? extends XAResourceHolder>> getXAResourceHolders()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Create a disposable handler used to drive a pooled instance of
	 * {@link XAStatefulHolder}.
	 * <p>This method is thread-safe.</p>
	 *
	 * @return a resource-specific disposable connection object.
	 *
	 * @throws Exception
	 * 		a resource-specific exception thrown when the disposable connection cannot be created.
	 */
	@Override
	public Object getConnectionHandle()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Close the physical connection that this {@link XAStatefulHolder} represents.
	 *
	 * @throws Exception
	 * 		a resource-specific exception thrown when there is an error closing the physical connection.
	 */
	@Override
	public void close()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Get the date at which this object was last released to the pool. This is required to check if it is eligible
	 * for discard when the containing pool needs to shrink.
	 *
	 * @return the date at which this object was last released to the pool or null if it never left the pool.
	 */
	@Override
	public Date getLastReleaseDate()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Get the date at which this object was created in the pool.
	 *
	 * @return the date at which this object was created in the pool.
	 */
	@Override
	public Date getCreationDate()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
