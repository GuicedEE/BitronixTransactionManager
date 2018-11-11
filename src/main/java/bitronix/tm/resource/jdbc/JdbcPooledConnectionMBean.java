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
package bitronix.tm.resource.jdbc;

import java.util.Collection;
import java.util.Date;

/**
 * {@link JdbcPooledConnection} Management interface.
 *
 * @author Ludovic Orban
 */
public interface JdbcPooledConnectionMBean
{

	/**
	 * Method getStateDescription returns the stateDescription of this JdbcPooledConnectionMBean object.
	 *
	 * @return the stateDescription (type String) of this JdbcPooledConnectionMBean object.
	 */
	String getStateDescription();

	/**
	 * Method getAcquisitionDate returns the acquisitionDate of this JdbcPooledConnectionMBean object.
	 *
	 * @return the acquisitionDate (type Date) of this JdbcPooledConnectionMBean object.
	 */
	Date getAcquisitionDate();

	/**
	 * Method getTransactionGtridsCurrentlyHoldingThis returns the transactionGtridsCurrentlyHoldingThis of this JdbcPooledConnectionMBean object.
	 *
	 * @return the transactionGtridsCurrentlyHoldingThis (type Collection<String>) of this JdbcPooledConnectionMBean object.
	 */
	Collection<String> getTransactionGtridsCurrentlyHoldingThis();

}
