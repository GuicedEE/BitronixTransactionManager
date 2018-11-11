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
package bitronix.tm.resource.jms;

/**
 * @author Ludovic Orban
 */
public interface PoolingConnectionFactoryMBean
{

	/**
	 * Method getMinPoolSize returns the minPoolSize of this PoolingConnectionFactoryMBean object.
	 *
	 * @return the minPoolSize (type int) of this PoolingConnectionFactoryMBean object.
	 */
	int getMinPoolSize();

	/**
	 * Method getMaxPoolSize returns the maxPoolSize of this PoolingConnectionFactoryMBean object.
	 *
	 * @return the maxPoolSize (type int) of this PoolingConnectionFactoryMBean object.
	 */
	int getMaxPoolSize();

	/**
	 * Method getInPoolSize returns the inPoolSize of this PoolingConnectionFactoryMBean object.
	 *
	 * @return the inPoolSize (type long) of this PoolingConnectionFactoryMBean object.
	 */
	long getInPoolSize();

	/**
	 * Method getTotalPoolSize returns the totalPoolSize of this PoolingConnectionFactoryMBean object.
	 *
	 * @return the totalPoolSize (type long) of this PoolingConnectionFactoryMBean object.
	 */
	long getTotalPoolSize();

	/**
	 * Method isFailed returns the failed of this PoolingConnectionFactoryMBean object.
	 *
	 * @return the failed (type boolean) of this PoolingConnectionFactoryMBean object.
	 */
	boolean isFailed();

	/**
	 * Method reset ...
	 *
	 * @throws Exception
	 * 		when
	 */
	void reset() throws Exception;

	/**
	 * Method isDisabled returns the disabled of this PoolingConnectionFactoryMBean object.
	 *
	 * @return the disabled (type boolean) of this PoolingConnectionFactoryMBean object.
	 */
	boolean isDisabled();

	/**
	 * Method setDisabled sets the disabled of this PoolingConnectionFactoryMBean object.
	 *
	 * @param disabled
	 * 		the disabled of this PoolingConnectionFactoryMBean object.
	 */
	void setDisabled(boolean disabled);

}
