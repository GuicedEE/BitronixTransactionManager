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

import javax.transaction.xa.XAResource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * {@link XAResourceHolder} created by an {@link bitronix.tm.resource.common.XAResourceProducer} that is
 * used to perform recovery. Objects of this class cannot be used outside recovery scope.
 *
 * @author Ludovic Orban
 */
public class RecoveryXAResourceHolder
		extends AbstractXAResourceHolder<RecoveryXAResourceHolder>
{

	private final XAResourceHolder<? extends XAResourceHolder> xaResourceHolder;

	/**
	 * Constructor RecoveryXAResourceHolder creates a new RecoveryXAResourceHolder instance.
	 *
	 * @param xaResourceHolder
	 * 		of type XAResourceHolder ? extends XAResourceHolder
	 */
	public RecoveryXAResourceHolder(XAResourceHolder<? extends XAResourceHolder> xaResourceHolder)
	{
		this.xaResourceHolder = xaResourceHolder;
	}

	/**
	 * Get the vendor's {@link javax.transaction.xa.XAResource} implementation of the wrapped resource.
	 *
	 * @return the vendor's XAResource implementation.
	 */
	@Override
	public XAResource getXAResource()
	{
		return xaResourceHolder.getXAResource();
	}

	/**
	 * Get the ResourceBean which created this XAResourceHolder.
	 *
	 * @return the ResourceBean which created this XAResourceHolder.
	 */
	@Override
	public ResourceBean getResourceBean()
	{
		return null;
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
	public List<RecoveryXAResourceHolder> getXAResourceHolders()
	{
		return new ArrayList<>();
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
	public Object getConnectionHandle() throws Exception
	{
		throw new UnsupportedOperationException("illegal connection creation attempt out of " + this);
	}

	/**
	 * Close the physical connection that this {@link XAStatefulHolder} represents.
	 *
	 * @throws Exception
	 * 		a resource-specific exception thrown when there is an error closing the physical connection.
	 */
	@Override
	public void close() throws Exception
	{
		xaResourceHolder.setState(State.IN_POOL);
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
		return null;
	}
}
