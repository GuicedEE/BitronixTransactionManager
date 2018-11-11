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

import java.util.*;

/**
 * @author Ludovic Orban
 */
public class EventRecorder
{

	private static final Map<Object, EventRecorder> eventRecorders = new HashMap<>();
	private final List<Event> events = new ArrayList<>();

	/**
	 * Constructor EventRecorder creates a new EventRecorder instance.
	 */
	private EventRecorder()
	{
	}

	/**
	 * Method getEventRecorder ...
	 *
	 * @param key
	 * 		of type Object
	 *
	 * @return EventRecorder
	 */
	public synchronized static EventRecorder getEventRecorder(Object key)
	{
		EventRecorder er = eventRecorders.get(key);
		if (er == null)
		{
			er = new EventRecorder();
			eventRecorders.put(key, er);
		}
		return er;
	}

	/**
	 * Method getEventRecorders returns the eventRecorders of this EventRecorder object.
	 *
	 * @return the eventRecorders (type Map<Object, EventRecorder>) of this EventRecorder object.
	 */
	public static Map<Object, EventRecorder> getEventRecorders()
	{
		return eventRecorders;
	}

	/**
	 * Method getOrderedEvents returns the orderedEvents of this EventRecorder object.
	 *
	 * @return the orderedEvents (type List<? extends Event>) of this EventRecorder object.
	 */
	public static List<? extends Event> getOrderedEvents()
	{
		Iterator<? extends Event> iterator = iterateEvents();
		List<Event> orderedEvents = new ArrayList<>();
		while (iterator.hasNext())
		{
			Event ev = iterator.next();
			orderedEvents.add(ev);
		}
		return orderedEvents;
	}

	/**
	 * Method iterateEvents ...
	 *
	 * @return Iterator<?
                                               *               	       	       extends
                                               *               	       	       Event>
	 */
	public static Iterator<? extends Event> iterateEvents()
	{
		return new EventsIterator(eventRecorders);
	}

	/**
	 * Method dumpToString ...
	 *
	 * @return String
	 */
	public static String dumpToString()
	{
		StringBuilder sb = new StringBuilder();

		int i = 0;
		Iterator<? extends Event> it = iterateEvents();
		while (it.hasNext())
		{
			Event event = it.next();
			sb.append(i++);
			sb.append(" - ");
			sb.append(event.toString());
			sb.append("\n");
		}

		return sb.toString();
	}

	/**
	 * Method clear ...
	 */
	public static void clear()
	{
		eventRecorders.clear();
	}

	/**
	 * Method addEvent ...
	 *
	 * @param evt
	 * 		of type Event
	 */
	public void addEvent(Event evt)
	{
		events.add(evt);
	}

	/**
	 * Method getEvents returns the events of this EventRecorder object.
	 *
	 * @return the events (type List<Event>) of this EventRecorder object.
	 */
	public List<Event> getEvents()
	{
		return events;
	}

}
