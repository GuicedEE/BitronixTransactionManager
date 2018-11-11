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

import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.utils.Uid;
import bitronix.tm.utils.UidGenerator;
import junit.framework.TestCase;

import javax.transaction.xa.XAResource;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Ludovic Orban
 */
public class AbstractXAResourceHolderTest
		extends TestCase
{

	/**
	 * Method testStatesForGtridIterationOrder ...
	 */
	public void testStatesForGtridIterationOrder()
	{
		ResourceBean resourceBean = new ResourceBean()
		{
		};

		AbstractXAResourceHolder xaResourceHolder = new AbstractXAResourceHolder()
		{
			/**
			 * Get the vendor's {@link javax.transaction.xa.XAResource} implementation of the wrapped resource.
			 * @return the vendor's XAResource implementation.
			 */
			@Override
			public XAResource getXAResource()
			{
				return null;
			}

			/**
			 * Get the ResourceBean which created this XAResourceHolder.
			 * @return the ResourceBean which created this XAResourceHolder.
			 */
			@Override
			public ResourceBean getResourceBean()
			{
				return resourceBean;
			}

			/**
			 * Get the list of {@link XAResourceHolder}s created by this
			 * {@link XAStatefulHolder} that are still open.
			 * <p>This method is thread-safe.</p>
			 * @return the list of {@link bitronix.tm.resource.common.XAResourceHolder}s created by this
			 *         {@link XAStatefulHolder} that are still open.
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
			 * @return a resource-specific disposable connection object.
			 * @throws Exception a resource-specific exception thrown when the disposable connection cannot be created.
			 */
			@Override
			public Object getConnectionHandle()
			{
				return null;
			}

			/**
			 * Close the physical connection that this {@link XAStatefulHolder} represents.
			 * @throws Exception a resource-specific exception thrown when there is an error closing the physical connection.
			 */
			@Override
			public void close()
			{
			}

			/**
			 * Get the date at which this object was last released to the pool. This is required to check if it is eligible
			 * for discard when the containing pool needs to shrink.
			 * @return the date at which this object was last released to the pool or null if it never left the pool.
			 */
			@Override
			public Date getLastReleaseDate()
			{
				return null;
			}
		};

		Uid gtrid = UidGenerator.generateUid();

		XAResourceHolderState state1 = new XAResourceHolderState(xaResourceHolder, resourceBean);
		XAResourceHolderState state2 = new XAResourceHolderState(xaResourceHolder, resourceBean);
		XAResourceHolderState state3 = new XAResourceHolderState(xaResourceHolder, resourceBean);

		xaResourceHolder.putXAResourceHolderState(UidGenerator.generateXid(gtrid), state1);
		xaResourceHolder.putXAResourceHolderState(UidGenerator.generateXid(gtrid), state2);
		xaResourceHolder.putXAResourceHolderState(UidGenerator.generateXid(gtrid), state3);


		Map statesForGtrid = xaResourceHolder.getXAResourceHolderStatesForGtrid(gtrid);
		Iterator statesForGtridIt = statesForGtrid.values()
		                                          .iterator();


		assertTrue(statesForGtridIt.hasNext());
		assertSame(state1, statesForGtridIt.next());
		assertTrue(statesForGtridIt.hasNext());
		assertSame(state2, statesForGtridIt.next());
		assertTrue(statesForGtridIt.hasNext());
		assertSame(state3, statesForGtridIt.next());
		assertFalse(statesForGtridIt.hasNext());
	}
}
