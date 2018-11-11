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
package bitronix.tm.recovery;

/**
 * {@link Recoverer} Management interface.
 *
 * @author Ludovic Orban
 */
public interface RecovererMBean
{

	/**
	 * Method run ...
	 */
	void run();

	/**
	 * Method getCommittedCount returns the committedCount of this RecovererMBean object.
	 *
	 * @return the committedCount (type int) of this RecovererMBean object.
	 */
	int getCommittedCount();

	/**
	 * Method getRolledbackCount returns the rolledbackCount of this RecovererMBean object.
	 *
	 * @return the rolledbackCount (type int) of this RecovererMBean object.
	 */
	int getRolledbackCount();

	/**
	 * Method getCompletionException returns the completionException of this RecovererMBean object.
	 *
	 * @return the completionException (type Exception) of this RecovererMBean object.
	 */
	Exception getCompletionException();

	/**
	 * Method getExecutionsCount returns the executionsCount of this RecovererMBean object.
	 *
	 * @return the executionsCount (type int) of this RecovererMBean object.
	 */
	int getExecutionsCount();

	/**
	 * Method isRunning returns the running of this RecovererMBean object.
	 *
	 * @return the running (type boolean) of this RecovererMBean object.
	 */
	boolean isRunning();

}
