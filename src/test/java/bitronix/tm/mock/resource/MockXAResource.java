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
package bitronix.tm.mock.resource;

import bitronix.tm.internal.BitronixXAException;
import bitronix.tm.mock.events.*;
import bitronix.tm.mock.resource.jdbc.MockitoXADataSource;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * @author Ludovic Orban
 */
public class MockXAResource
		implements XAResource
{

	private int prepareRc = XAResource.XA_OK;
	private int transactiontimeout;
	private MockitoXADataSource xads;

	private XAException endException;
	private XAException prepareException;
	private XAException commitException;
	private XAException rollbackException;
	private RuntimeException prepareRuntimeException;
	private XAException recoverException;
	private long recoveryDelay;

	/**
	 * Constructor MockXAResource creates a new MockXAResource instance.
	 *
	 * @param xads
	 * 		of type MockitoXADataSource
	 */
	public MockXAResource(MockitoXADataSource xads)
	{
		this.xads = xads;
	}

	/**
	 * Method setRecoveryDelay sets the recoveryDelay of this MockXAResource object.
	 *
	 * @param recoveryDelay
	 * 		the recoveryDelay of this MockXAResource object.
	 */
	public void setRecoveryDelay(long recoveryDelay)
	{
		this.recoveryDelay = recoveryDelay;
	}

	/**
	 * Method setPrepareRc sets the prepareRc of this MockXAResource object.
	 *
	 * @param prepareRc
	 * 		the prepareRc of this MockXAResource object.
	 */
	public void setPrepareRc(int prepareRc)
	{
		this.prepareRc = prepareRc;
	}

	/**
	 * Method addInDoubtXid ...
	 *
	 * @param xid
	 * 		of type Xid
	 */
	public void addInDoubtXid(Xid xid)
	{
		xads.addInDoubtXid(xid);
	}

	/**
	 * Method commit ...
	 *
	 * @param xid
	 * 		of type Xid
	 * @param b
	 * 		of type boolean
	 *
	 * @throws XAException
	 * 		when
	 */
	@Override
	public void commit(Xid xid, boolean b) throws XAException
	{
		getEventRecorder().addEvent(new XAResourceCommitEvent(this, commitException, xid, b));
		if (commitException != null)
		{
			throw commitException;
		}
		if (xads != null)
		{
			xads.removeInDoubtXid(xid);
		}
	}

    /*
    Interface implementation
    */

	/**
	 * Method end ...
	 *
	 * @param xid
	 * 		of type Xid
	 * @param flag
	 * 		of type int
	 *
	 * @throws XAException
	 * 		when
	 */
	@Override
	public void end(Xid xid, int flag) throws XAException
	{
		getEventRecorder().addEvent(new XAResourceEndEvent(this, xid, flag));
		if (endException != null)
		{
			throw endException;
		}
	}

	/**
	 * Method forget ...
	 *
	 * @param xid
	 * 		of type Xid
	 *
	 * @throws XAException
	 * 		when
	 */
	@Override
	public void forget(Xid xid) throws XAException
	{
		getEventRecorder().addEvent(new XAResourceForgetEvent(this, xid));
		boolean found = xads.removeInDoubtXid(xid);
		if (!found)
		{
			throw new BitronixXAException("unknown XID: " + xid, XAException.XAER_INVAL);
		}
	}

	/**
	 * Method getTransactionTimeout returns the transactionTimeout of this MockXAResource object.
	 *
	 * @return the transactionTimeout (type int) of this MockXAResource object.
	 */
	@Override
	public int getTransactionTimeout()
	{
		return transactiontimeout;
	}

	/**
	 * Method isSameRM ...
	 *
	 * @param xaResource
	 * 		of type XAResource
	 *
	 * @return boolean
	 */
	@Override
	public boolean isSameRM(XAResource xaResource)
	{
		boolean result = xaResource == this;
		getEventRecorder().addEvent(new XAResourceIsSameRmEvent(this, xaResource, result));
		return result;
	}

	/**
	 * Method prepare ...
	 *
	 * @param xid
	 * 		of type Xid
	 *
	 * @return int
	 *
	 * @throws XAException
	 * 		when
	 */
	@Override
	public int prepare(Xid xid) throws XAException
	{
		if (prepareException != null)
		{
			getEventRecorder().addEvent(new XAResourcePrepareEvent(this, prepareException, xid, -1));
			prepareException.fillInStackTrace();
			throw prepareException;
		}
		if (prepareRuntimeException != null)
		{
			prepareRuntimeException.fillInStackTrace();
			getEventRecorder().addEvent(new XAResourcePrepareEvent(this, prepareRuntimeException, xid, -1));
			throw prepareRuntimeException;
		}
		getEventRecorder().addEvent(new XAResourcePrepareEvent(this, xid, prepareRc));
		return prepareRc;
	}

	/**
	 * Method recover ...
	 *
	 * @param flag
	 * 		of type int
	 *
	 * @return Xid[]
	 *
	 * @throws XAException
	 * 		when
	 */
	@Override
	public Xid[] recover(int flag) throws XAException
	{
		if (recoveryDelay > 0)
		{
			try
			{
				Thread.sleep(recoveryDelay);
			}
			catch (InterruptedException e)
			{
				// ignore
			}
		}

		if (recoverException != null)
		{
			throw recoverException;
		}
		if (xads == null)
		{
			return new Xid[0];
		}
		return xads.getInDoubtXids();
	}

	/**
	 * Method rollback ...
	 *
	 * @param xid
	 * 		of type Xid
	 *
	 * @throws XAException
	 * 		when
	 */
	@Override
	public void rollback(Xid xid) throws XAException
	{
		getEventRecorder().addEvent(new XAResourceRollbackEvent(this, rollbackException, xid));
		if (rollbackException != null)
		{
			throw rollbackException;
		}
		if (xads != null)
		{
			xads.removeInDoubtXid(xid);
		}
	}

	/**
	 * Method setTransactionTimeout ...
	 *
	 * @param i
	 * 		of type int
	 *
	 * @return boolean
	 */
	@Override
	public boolean setTransactionTimeout(int i)
	{
		transactiontimeout = i;
		return true;
	}

	/**
	 * Method start ...
	 *
	 * @param xid
	 * 		of type Xid
	 * @param flag
	 * 		of type int
	 */
	@Override
	public void start(Xid xid, int flag)
	{
		getEventRecorder().addEvent(new XAResourceStartEvent(this, xid, flag));
	}

	/**
	 * Method getEventRecorder returns the eventRecorder of this MockXAResource object.
	 *
	 * @return the eventRecorder (type EventRecorder) of this MockXAResource object.
	 */
	private EventRecorder getEventRecorder()
	{
		return EventRecorder.getEventRecorder(this);
	}

	/**
	 * Method setEndException sets the endException of this MockXAResource object.
	 *
	 * @param endException
	 * 		the endException of this MockXAResource object.
	 */
	public void setEndException(XAException endException)
	{
		this.endException = endException;
	}

	/**
	 * Method setPrepareException sets the prepareException of this MockXAResource object.
	 *
	 * @param prepareException
	 * 		the prepareException of this MockXAResource object.
	 */
	public void setPrepareException(XAException prepareException)
	{
		this.prepareException = prepareException;
	}

	/**
	 * Method setPrepareException sets the prepareException of this MockXAResource object.
	 *
	 * @param prepareException
	 * 		the prepareException of this MockXAResource object.
	 */
	public void setPrepareException(RuntimeException prepareException)
	{
		prepareRuntimeException = prepareException;
	}

	/**
	 * Method setCommitException sets the commitException of this MockXAResource object.
	 *
	 * @param commitException
	 * 		the commitException of this MockXAResource object.
	 */
	public void setCommitException(XAException commitException)
	{
		this.commitException = commitException;
	}

	/**
	 * Method setRollbackException sets the rollbackException of this MockXAResource object.
	 *
	 * @param rollbackException
	 * 		the rollbackException of this MockXAResource object.
	 */
	public void setRollbackException(XAException rollbackException)
	{
		this.rollbackException = rollbackException;
	}

	/**
	 * Method setRecoverException sets the recoverException of this MockXAResource object.
	 *
	 * @param recoverException
	 * 		the recoverException of this MockXAResource object.
	 */
	public void setRecoverException(XAException recoverException)
	{
		this.recoverException = recoverException;
	}
}
