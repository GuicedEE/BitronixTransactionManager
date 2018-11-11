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

import javax.transaction.xa.Xid;

/**
 * @author Ludovic Orban
 */
public abstract class XAEvent
		extends Event
{

	private Xid xid;

	/**
	 * Constructor XAEvent creates a new XAEvent instance.
	 *
	 * @param source
	 * 		of type Object
	 * @param xid
	 * 		of type Xid
	 */
	protected XAEvent(Object source, Xid xid)
	{
		super(source, null);
		this.xid = xid;
	}

	/**
	 * Constructor XAEvent creates a new XAEvent instance.
	 *
	 * @param source
	 * 		of type Object
	 * @param ex
	 * 		of type Exception
	 * @param xid
	 * 		of type Xid
	 */
	protected XAEvent(Object source, Exception ex, Xid xid)
	{
		super(source, ex);
		this.xid = xid;
	}

	/**
	 * Method getXid returns the xid of this XAEvent object.
	 *
	 * @return the xid (type Xid) of this XAEvent object.
	 */
	public Xid getXid()
	{
		return xid;
	}

}
