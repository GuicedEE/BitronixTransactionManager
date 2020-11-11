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
package bitronix.tm.internal;

import bitronix.tm.BitronixXid;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.utils.Decoder;
import bitronix.tm.utils.MonotonicClock;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import java.util.Date;

/**
 * {@link XAResourceHolder} state container.
 * Instances are kept in the transaction and bound to / unbound from the {@link XAResourceHolder} as the
 * resource participates in different transactions. A {@link XAResourceHolder} without {@link XAResourceHolderState}
 * is considered to be in local transaction mode.
 * <p>Objects of this class also expose resource specific configuration like the unique resource name.</p>
 * <p>The {@link XAResource} state during a transaction participation is also contained: assigned XID, transaction
 * start / end state...</p>
 * <p>There is exactly one {@link XAResourceHolderState} object per {@link XAResourceHolder} per
 * {@link jakarta.transaction.Transaction}.</p>
 *
 * @author Ludovic Orban
 * @see bitronix.tm.resource.common.ResourceBean
 */
public class XAResourceHolderState
{

	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(XAResourceHolderState.class.toString());
	private static final String WITH_STRING = " with ";

	private final ResourceBean bean;
	private final XAResourceHolder xaResourceHolder;
	private volatile BitronixXid xid;
	private volatile boolean started;
	private volatile boolean ended;
	private volatile boolean suspended;
	private volatile Date transactionTimeoutDate;
	private volatile boolean isTimeoutAlreadySet;
	private volatile boolean failed;
	private volatile int hashCode;

	/**
	 * Constructor XAResourceHolderState creates a new XAResourceHolderState instance.
	 *
	 * @param resourceHolder
	 * 		of type XAResourceHolder
	 * @param bean
	 * 		of type ResourceBean
	 */
	public XAResourceHolderState(XAResourceHolder resourceHolder, ResourceBean bean)
	{
		this.bean = bean;
		this.xaResourceHolder = resourceHolder;

		started = false;
		ended = false;
		suspended = false;
		isTimeoutAlreadySet = false;
		xid = null;
		hashCode = 17 * bean.hashCode();
	}

	/**
	 * Constructor XAResourceHolderState creates a new XAResourceHolderState instance.
	 *
	 * @param resourceHolderState
	 * 		of type XAResourceHolderState
	 */
	public XAResourceHolderState(XAResourceHolderState resourceHolderState)
	{
		this.bean = resourceHolderState.bean;
		this.xaResourceHolder = resourceHolderState.xaResourceHolder;

		started = false;
		ended = false;
		suspended = false;
		isTimeoutAlreadySet = false;
		xid = null;
		hashCode = 17 * bean.hashCode();
	}

	/**
	 * Method getXid returns the xid of this XAResourceHolderState object.
	 *
	 * @return the xid (type BitronixXid) of this XAResourceHolderState object.
	 */
	public BitronixXid getXid()
	{
		return xid;
	}

	/**
	 * Method setXid sets the xid of this XAResourceHolderState object.
	 *
	 * @param xid
	 * 		the xid of this XAResourceHolderState object.
	 *
	 * @throws BitronixSystemException
	 * 		when
	 */
	public void setXid(BitronixXid xid) throws BitronixSystemException
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("assigning <" + xid + "> to <" + this + ">");
		}
		if (this.xid != null && !xid.equals(this.xid))
		{
			throw new BitronixSystemException("a XID has already been assigned to " + this);
		}
		this.xid = xid;
		hashCode = 17 * (bean.hashCode() + (xid != null ? xid.hashCode() : 0));
	}

	/**
	 * Method getXAResourceHolder returns the XAResourceHolder of this XAResourceHolderState object.
	 *
	 * @return the XAResourceHolder (type XAResourceHolder) of this XAResourceHolderState object.
	 */
	public XAResourceHolder getXAResourceHolder()
	{
		return xaResourceHolder;
	}

	/**
	 * Method getTransactionTimeoutDate returns the transactionTimeoutDate of this XAResourceHolderState object.
	 *
	 * @return the transactionTimeoutDate (type Date) of this XAResourceHolderState object.
	 */
	public Date getTransactionTimeoutDate()
	{
		return transactionTimeoutDate;
	}

	/**
	 * Method setTransactionTimeoutDate sets the transactionTimeoutDate of this XAResourceHolderState object.
	 *
	 * @param transactionTimeoutDate
	 * 		the transactionTimeoutDate of this XAResourceHolderState object.
	 */
	public void setTransactionTimeoutDate(Date transactionTimeoutDate)
	{
		this.transactionTimeoutDate = transactionTimeoutDate;
	}

	/**
	 * Method getUniqueName returns the uniqueName of this XAResourceHolderState object.
	 *
	 * @return the uniqueName (type String) of this XAResourceHolderState object.
	 */
	public String getUniqueName()
	{
		return bean.getUniqueName();
	}

	/**
	 * Method getUseTmJoin returns the useTmJoin of this XAResourceHolderState object.
	 *
	 * @return the useTmJoin (type boolean) of this XAResourceHolderState object.
	 */
	public boolean getUseTmJoin()
	{
		return bean.getUseTmJoin();
	}

	/**
	 * Method getTwoPcOrderingPosition returns the twoPcOrderingPosition of this XAResourceHolderState object.
	 *
	 * @return the twoPcOrderingPosition (type int) of this XAResourceHolderState object.
	 */
	public int getTwoPcOrderingPosition()
	{
		return bean.getTwoPcOrderingPosition();
	}

	/**
	 * Method getIgnoreRecoveryFailures returns the ignoreRecoveryFailures of this XAResourceHolderState object.
	 *
	 * @return the ignoreRecoveryFailures (type boolean) of this XAResourceHolderState object.
	 */
	public boolean getIgnoreRecoveryFailures()
	{
		return bean.getIgnoreRecoveryFailures();
	}

	/**
	 * Method isEnded returns the ended of this XAResourceHolderState object.
	 *
	 * @return the ended (type boolean) of this XAResourceHolderState object.
	 */
	public boolean isEnded()
	{
		return ended;
	}

	/**
	 * Method isStarted returns the started of this XAResourceHolderState object.
	 *
	 * @return the started (type boolean) of this XAResourceHolderState object.
	 */
	public boolean isStarted()
	{
		return started;
	}

	/**
	 * Method isSuspended returns the suspended of this XAResourceHolderState object.
	 *
	 * @return the suspended (type boolean) of this XAResourceHolderState object.
	 */
	public boolean isSuspended()
	{
		return suspended;
	}

	/**
	 * Method isFailed returns the failed of this XAResourceHolderState object.
	 *
	 * @return the failed (type boolean) of this XAResourceHolderState object.
	 */
	public boolean isFailed()
	{
		return failed;
	}

	/**
	 * Method end ...
	 *
	 * @param flags
	 * 		of type int
	 *
	 * @throws XAException
	 * 		when
	 */
	public void end(int flags) throws XAException
	{
		boolean hasEnded = this.ended;
		boolean isSuspended = this.suspended;

		if (this.ended && (flags == XAResource.TMSUSPEND))
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("resource already ended, changing state to suspended: " + this);
			}
			this.suspended = true;
			return;
		}

		if (this.ended)
		{
			throw new BitronixXAException("resource already ended: " + this, XAException.XAER_PROTO);
		}

		if (flags == XAResource.TMSUSPEND)
		{
			if (!this.started)
			{
				throw new BitronixXAException("resource hasn't been started, cannot suspend it: " + this, XAException.XAER_PROTO);
			}
			if (this.suspended)
			{
				throw new BitronixXAException("resource already suspended: " + this, XAException.XAER_PROTO);
			}

			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("suspending " + this + WITH_STRING + Decoder.decodeXAResourceFlag(flags));
			}
			isSuspended = true;
		}
		else
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("ending " + this + WITH_STRING + Decoder.decodeXAResourceFlag(flags));
			}
			hasEnded = true;
		}

		try
		{
			getXAResource().end(xid, flags);
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("ended " + this + WITH_STRING + Decoder.decodeXAResourceFlag(flags));
			}
		}
		catch (XAException ex)
		{
			// could mean failed or unilaterally rolled back
			failed = true;
			throw ex;
		}
		finally
		{
			this.suspended = isSuspended;
			this.ended = hasEnded;
			this.started = false;
		}
	}

	/**
	 * Method getXAResource returns the XAResource of this XAResourceHolderState object.
	 *
	 * @return the XAResource (type XAResource) of this XAResourceHolderState object.
	 */
	public XAResource getXAResource()
	{
		return xaResourceHolder.getXAResource();
	}

	/**
	 * Method start ...
	 *
	 * @param flags
	 * 		of type int
	 *
	 * @throws XAException
	 * 		when
	 */
	public void start(int flags) throws XAException
	{
		boolean isSuspended = this.suspended;
		boolean hasStarted = this.started;

		if (this.ended && (flags == XAResource.TMRESUME))
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("resource already ended, changing state to resumed: " + this);
			}
			this.suspended = false;
			return;
		}

		if (flags == XAResource.TMRESUME)
		{
			if (!this.suspended)
			{
				throw new BitronixXAException("resource hasn't been suspended, cannot resume it: " + this, XAException.XAER_PROTO);
			}
			if (!this.started)
			{
				throw new BitronixXAException("resource hasn't been started, cannot resume it: " + this, XAException.XAER_PROTO);
			}

			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("resuming " + this + WITH_STRING + Decoder.decodeXAResourceFlag(flags));
			}
			isSuspended = false;
		}
		else
		{
			if (this.started)
			{
				throw new BitronixXAException("resource already started: " + this, XAException.XAER_PROTO);
			}

			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("starting " + this + WITH_STRING + Decoder.decodeXAResourceFlag(flags));
			}
			hasStarted = true;
		}

		if (!isTimeoutAlreadySet && transactionTimeoutDate != null && bean.getApplyTransactionTimeout())
		{
			int timeoutInSeconds = (int) ((transactionTimeoutDate.getTime() - MonotonicClock.currentTimeMillis() + 999L) / 1000L);
			timeoutInSeconds = Math.max(1, timeoutInSeconds); // setting a timeout of 0 means resetting -> set it to at least 1
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("applying resource timeout of " + timeoutInSeconds + "s on " + this);
			}
			getXAResource().setTransactionTimeout(timeoutInSeconds);
			isTimeoutAlreadySet = true;
		}

		getXAResource().start(xid, flags);
		this.suspended = isSuspended;
		this.started = hasStarted;
		this.ended = false;
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("started " + this + WITH_STRING + Decoder.decodeXAResourceFlag(flags));
		}
	}

	/**
	 * Method hashCode ...
	 *
	 * @return int
	 */
	@Override
	public int hashCode()
	{
		return hashCode;
	}

	/**
	 * Method equals ...
	 *
	 * @param obj
	 * 		of type Object
	 *
	 * @return boolean
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof XAResourceHolderState) || this.hashCode != obj.hashCode())
		{
			return false;
		}

		XAResourceHolderState other = (XAResourceHolderState) obj;
		return equals(other.bean, bean) && equals(other.xid, xid);
	}

	/**
	 * Method equals ...
	 *
	 * @param obj1
	 * 		of type Object
	 * @param obj2
	 * 		of type Object
	 *
	 * @return boolean
	 */
	private boolean equals(Object obj1, Object obj2)
	{
		if (obj1 == obj2)
		{
			return true;
		}
		if (obj1 == null || obj2 == null)
		{
			return false;
		}

		return obj1.equals(obj2);
	}

	/**
	 * Method toString ...
	 *
	 * @return String
	 */
	@Override
	public String toString()
	{
		return "an XAResourceHolderState with uniqueName=" + bean.getUniqueName() +
		       " XAResource=" + getXAResource() +
		       (started ? " (started)" : "") +
		       (ended ? " (ended)" : "") +
		       (suspended ? " (suspended)" : "") +
		       " with XID " + xid;
	}
}
