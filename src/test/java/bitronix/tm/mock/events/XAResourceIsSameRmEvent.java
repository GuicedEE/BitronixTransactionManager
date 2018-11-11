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
package bitronix.tm.mock.events;

import javax.transaction.xa.XAResource;

/**
 * @author Ludovic Orban
 */
public class XAResourceIsSameRmEvent
		extends XAEvent
{

	private XAResource xaResource;
	private boolean sameRm;

	/**
	 * Constructor XAResourceIsSameRmEvent creates a new XAResourceIsSameRmEvent instance.
	 *
	 * @param source
	 * 		of type Object
	 * @param xaResource
	 * 		of type XAResource
	 * @param sameRm
	 * 		of type boolean
	 */
	public XAResourceIsSameRmEvent(Object source, XAResource xaResource, boolean sameRm)
	{
		super(source, null);
		this.xaResource = xaResource;
		this.sameRm = sameRm;
	}

	/**
	 * Method getXAResource returns the XAResource of this XAResourceIsSameRmEvent object.
	 *
	 * @return the XAResource (type XAResource) of this XAResourceIsSameRmEvent object.
	 */
	public XAResource getXAResource()
	{
		return xaResource;
	}

	/**
	 * Method isSameRm returns the sameRm of this XAResourceIsSameRmEvent object.
	 *
	 * @return the sameRm (type boolean) of this XAResourceIsSameRmEvent object.
	 */
	public boolean isSameRm()
	{
		return sameRm;
	}

	/**
	 * Method toString ...
	 *
	 * @return String
	 */
	@Override
	public String toString()
	{
		return "XAResourceIsSameRmEvent at " + getTimestamp() + " with XAResource=" + xaResource;
	}

}
