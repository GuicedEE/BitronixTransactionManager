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

import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation of all services required by a {@link XAStatefulHolder}.
 *
 * @author Ludovic Orban
 */
public abstract class AbstractXAStatefulHolder<T extends XAStatefulHolder<T>>
		implements XAStatefulHolder<T>
{

	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(AbstractXAStatefulHolder.class.toString());
	private final List<StateChangeListener<T>> stateChangeEventListeners = new CopyOnWriteArrayList<>();
	private final Date creationDate = new Date();
	private volatile State state = State.IN_POOL;

	/**
	 * Get the current resource state.
	 * <p>This method is thread-safe.</p>
	 *
	 * @return the current resource state.
	 */
	@Override
	public State getState()
	{
		return state;
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
		State oldState = this.state;
		fireStateChanging(oldState, state);

		if (oldState == state)
		{
			throw new IllegalArgumentException("cannot switch state from " + oldState +
			                                   " to " + state);
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("state changing from " + oldState +
			          " to " + state + " in " + this);
		}

		this.state = state;

		fireStateChanged(oldState, state);
	}

	/**
	 * Method addStateChangeEventListener ...
	 *
	 * @param listener
	 * 		of type StateChangeListener<T>
	 */
	@Override
	public void addStateChangeEventListener(StateChangeListener<T> listener)
	{
		stateChangeEventListeners.add(listener);
	}

	/**
	 * Method removeStateChangeEventListener ...
	 *
	 * @param listener
	 * 		of type StateChangeListener<T>
	 */
	@Override
	public void removeStateChangeEventListener(StateChangeListener<T> listener)
	{
		stateChangeEventListeners.remove(listener);
	}

	/**
	 * Get the date at which this object was created in the pool.
	 *
	 * @return the date at which this object was created in the pool.
	 */
	@Override
	public Date getCreationDate()
	{
		return creationDate;
	}

	/**
	 * Method fireStateChanging ...
	 *
	 * @param currentState
	 * 		of type State
	 * @param futureState
	 * 		of type State
	 */
	@SuppressWarnings("unchecked")
	private void fireStateChanging(State currentState, State futureState)
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("notifying " + stateChangeEventListeners.size() +
			          " stateChangeEventListener(s) about state changing from " + currentState +
			          " to " + futureState + " in " + this);
		}

		for (StateChangeListener<T> stateChangeListener : stateChangeEventListeners)
		{
			stateChangeListener.stateChanging((T) this, currentState, futureState);
		}
	}

	/**
	 * Method fireStateChanged ...
	 *
	 * @param oldState
	 * 		of type State
	 * @param newState
	 * 		of type State
	 */
	@SuppressWarnings("unchecked")
	private void fireStateChanged(State oldState, State newState)
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("notifying " + stateChangeEventListeners.size() +
			          " stateChangeEventListener(s) about state changed from " + oldState +
			          " to " + newState + " in " + this);
		}

		for (StateChangeListener<T> stateChangeListener : stateChangeEventListeners)
		{
			stateChangeListener.stateChanged((T) this, oldState, newState);
		}
	}
}
