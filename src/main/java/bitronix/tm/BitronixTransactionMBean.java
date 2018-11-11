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
package bitronix.tm;

import java.util.Collection;
import java.util.Date;

/**
 * {@link BitronixTransaction} Management interface.
 *
 * @author Ludovic Orban
 */
public interface BitronixTransactionMBean
{

	/**
	 * Method getGtrid returns the gtrid of this BitronixTransactionMBean object.
	 *
	 * @return the gtrid (type String) of this BitronixTransactionMBean object.
	 */
	String getGtrid();

	/**
	 * Method getStatusDescription returns the statusDescription of this BitronixTransactionMBean object.
	 *
	 * @return the statusDescription (type String) of this BitronixTransactionMBean object.
	 */
	String getStatusDescription();

	/**
	 * Method getThreadName returns the threadName of this BitronixTransactionMBean object.
	 *
	 * @return the threadName (type String) of this BitronixTransactionMBean object.
	 */
	String getThreadName();

	/**
	 * Method getStartDate returns the startDate of this BitronixTransactionMBean object.
	 *
	 * @return the startDate (type Date) of this BitronixTransactionMBean object.
	 */
	Date getStartDate();

	/**
	 * Method getEnlistedResourcesUniqueNames returns the enlistedResourcesUniqueNames of this BitronixTransactionMBean object.
	 *
	 * @return the enlistedResourcesUniqueNames (type Collection<String>) of this BitronixTransactionMBean object.
	 */
	Collection<String> getEnlistedResourcesUniqueNames();
}
