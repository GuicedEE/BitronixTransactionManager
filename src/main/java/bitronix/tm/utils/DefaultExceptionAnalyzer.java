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
package bitronix.tm.utils;


import javax.transaction.xa.XAException;
import java.util.logging.Level;

/**
 * Default implementation of {@link ExceptionAnalyzer}.
 *
 * @author Ludovic Orban
 */
public class DefaultExceptionAnalyzer
		implements ExceptionAnalyzer
{
	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(DefaultExceptionAnalyzer.class.toString());

	/**
	 * Extract information from a vendor's XAException that isn't available through standard APIs.
	 *
	 * @param ex
	 * 		the {@link javax.transaction.xa.XAException} to analyze.
	 *
	 * @return extra error details as a human-readable string, or null if nothing extra was found.
	 */
	@Override
	public String extractExtraXAExceptionDetails(XAException ex)
	{
		if ("oracle.jdbc.xa.OracleXAException".equals(ex.getClass()
		                                                .getName()))
		{
			try
			{
				return "ORA-" + PropertyUtils.getProperty(ex, "oracleError");
			}
			catch (PropertyException e)
			{
				log.log(Level.FINEST, "Property Exception", e);
				return null;
			}
		}
		return null;
	}

	/**
	 * Shutdown the service and free all held resources.
	 */
	@Override
	public void shutdown()
	{
		//Nothing needed
	}

}
