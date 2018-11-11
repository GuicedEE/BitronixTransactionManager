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

import bitronix.tm.BitronixXid;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.utils.Uid;

import javax.transaction.xa.XAResource;
import java.util.Date;
import java.util.List;

/**
 * Dummy XASResourceHolder class, to get the generics right.
 *
 * @author Chris Rankin
 */
public class DummyResourceHolder
		implements XAResourceHolder<DummyResourceHolder>
{

	/**
	 * Get the vendor's {@link javax.transaction.xa.XAResource} implementation of the wrapped resource.
	 *
	 * @return the vendor's XAResource implementation.
	 */
	@Override
	public XAResource getXAResource()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * This method implements a standard Visitor Pattern.  For the specified GTRID, the
	 * provided {@link bitronix.tm.resource.common.XAResourceHolderStateVisitor}'s visit() method is called for each matching
	 * {@link bitronix.tm.internal.XAResourceHolderState} in the order they were added.  This method was introduced
	 * as a replacement for the old getXAResourceHolderStatesForGtrid(Uid) method.  The old
	 * getXAResourceHolderStatesForGtrid method exported an internal collection which was unsynchronized
	 * yet was iterated over by the callers.  Using the Visitor Pattern allows us to perform the same
	 * iteration within the context of a lock, and avoids exposing internal state and implementation
	 * details to callers.
	 *
	 * @param gtrid
	 * 		the GTRID of the transaction state to visit {@link bitronix.tm.internal.XAResourceHolderState}s for
	 * @param visitor
	 * 		a {@link bitronix.tm.resource.common.XAResourceHolderStateVisitor} instance
	 */
	@Override
	public void acceptVisitorForXAResourceHolderStates(Uid gtrid, XAResourceHolderStateVisitor visitor)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Checks whether there are {@link bitronix.tm.internal.XAResourceHolderState}s for the specified GTRID.
	 *
	 * @param gtrid
	 * 		the GTRID of the transaction state to check existence for
	 *
	 * @return true if there are {@link bitronix.tm.internal.XAResourceHolderState}s, false otherwise
	 */
	@Override
	public boolean isExistXAResourceHolderStatesForGtrid(Uid gtrid)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Get a count of {@link bitronix.tm.internal.XAResourceHolderState}s for the specified GTRID.
	 *
	 * @param gtrid
	 * 		the GTRID to get a {@link bitronix.tm.internal.XAResourceHolderState} count for
	 *
	 * @return the count of {@link bitronix.tm.internal.XAResourceHolderState}s, or 0 if there are no states for the
	 * 		specified GTRID
	 */
	@Override
	public int getXAResourceHolderStateCountForGtrid(Uid gtrid)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Add a {@link bitronix.tm.internal.XAResourceHolderState} of this wrapped resource.
	 *
	 * @param xid
	 * 		the Xid of the transaction state to add.
	 * @param xaResourceHolderState
	 * 		the {@link bitronix.tm.internal.XAResourceHolderState} to set.
	 */
	@Override
	public void putXAResourceHolderState(BitronixXid xid, XAResourceHolderState xaResourceHolderState)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Remove all states related to a specific Xid from this wrapped resource.
	 *
	 * @param xid
	 * 		the Xid of the transaction state to remove.
	 */
	@Override
	public void removeXAResourceHolderState(BitronixXid xid)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Check if this {@link bitronix.tm.resource.common.XAResourceHolder} contains a state for a specific {@link bitronix.tm.resource.common.XAResourceHolder}.
	 * In other words: has the {@link bitronix.tm.resource.common.XAResourceHolder}'s {@link javax.transaction.xa.XAResource} been enlisted in some transaction ?
	 *
	 * @param xaResourceHolder
	 * 		the {@link bitronix.tm.resource.common.XAResourceHolder} to look for.
	 *
	 * @return true if the {@link bitronix.tm.resource.common.XAResourceHolder} is enlisted in some transaction, false otherwise.
	 */
	@Override
	public boolean hasStateForXAResource(XAResourceHolder<? extends XAResourceHolder> xaResourceHolder)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Get the ResourceBean which created this XAResourceHolder.
	 *
	 * @return the ResourceBean which created this XAResourceHolder.
	 */
	@Override
	public ResourceBean getResourceBean()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

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
	 * 		of type StateChangeListener<DummyResourceHolder>
	 */
	@Override
	public void addStateChangeEventListener(StateChangeListener<DummyResourceHolder> listener)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Method removeStateChangeEventListener ...
	 *
	 * @param listener
	 * 		of type StateChangeListener<DummyResourceHolder>
	 */
	@Override
	public void removeStateChangeEventListener(StateChangeListener<DummyResourceHolder> listener)
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
