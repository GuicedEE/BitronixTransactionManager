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
package bitronix.tm.mock.resource.jdbc;

import bitronix.tm.mock.events.*;
import bitronix.tm.mock.resource.MockXAResource;
import org.mockito.stubbing.Answer;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.mockito.Mockito.*;

/**
 * @author Ludovic Orban
 */
public class MockitoXADataSource
		implements XADataSource
{

	private static SQLException staticGetXAConnectionException;
	private static SQLException staticCloseXAConnectionException;
	private final List<Xid> inDoubtXids = new ArrayList<>();
	private List<XAConnection> xaConnections = new ArrayList<>();
	private String userName;
	private String password;
	private String database;
	private Object uselessThing;
	private Properties clonedProperties;
	private SQLException getXAConnectionException;

	/**
	 * Method setStaticGetXAConnectionException sets the staticGetXAConnectionException of this MockitoXADataSource object.
	 *
	 * @param ex
	 * 		the staticGetXAConnectionException of this MockitoXADataSource object.
	 */
	public static void setStaticGetXAConnectionException(SQLException ex)
	{
		staticGetXAConnectionException = ex;
	}

	/**
	 * Method setStaticCloseXAConnectionException sets the staticCloseXAConnectionException of this MockitoXADataSource object.
	 *
	 * @param ex
	 * 		the staticCloseXAConnectionException of this MockitoXADataSource object.
	 */
	public static void setStaticCloseXAConnectionException(SQLException ex)
	{
		staticCloseXAConnectionException = ex;
	}

	/**
	 * Method setXaConnections sets the xaConnections of this MockitoXADataSource object.
	 *
	 * @param xaConnections
	 * 		the xaConnections of this MockitoXADataSource object.
	 */
	public void setXaConnections(List<XAConnection> xaConnections)
	{
		this.xaConnections = xaConnections;
	}

	/**
	 * Method getUserName returns the userName of this MockitoXADataSource object.
	 *
	 * @return the userName (type String) of this MockitoXADataSource object.
	 */
	public String getUserName()
	{
		return userName;
	}

	/**
	 * Method setUserName sets the userName of this MockitoXADataSource object.
	 *
	 * @param userName
	 * 		the userName of this MockitoXADataSource object.
	 */
	public void setUserName(String userName)
	{
		this.userName = userName;
	}

	/**
	 * Method getPassword returns the password of this MockitoXADataSource object.
	 *
	 * @return the password (type String) of this MockitoXADataSource object.
	 */
	public String getPassword()
	{
		return password;
	}

	/**
	 * Method setPassword sets the password of this MockitoXADataSource object.
	 *
	 * @param password
	 * 		the password of this MockitoXADataSource object.
	 */
	public void setPassword(String password)
	{
		this.password = password;
	}

	/**
	 * Method getDatabase returns the database of this MockitoXADataSource object.
	 *
	 * @return the database (type String) of this MockitoXADataSource object.
	 */
	public String getDatabase()
	{
		return database;
	}

	/**
	 * Method setDatabase sets the database of this MockitoXADataSource object.
	 *
	 * @param database
	 * 		the database of this MockitoXADataSource object.
	 */
	public void setDatabase(String database)
	{
		this.database = database;
	}

	/**
	 * Method getClonedProperties returns the clonedProperties of this MockitoXADataSource object.
	 *
	 * @return the clonedProperties (type Properties) of this MockitoXADataSource object.
	 */
	public Properties getClonedProperties()
	{
		return (clonedProperties == null) ? null : (Properties) clonedProperties.clone();
	}

	/**
	 * Method setClonedProperties sets the clonedProperties of this MockitoXADataSource object.
	 *
	 * @param properties
	 * 		the clonedProperties of this MockitoXADataSource object.
	 */
	public void setClonedProperties(Properties properties)
	{
		clonedProperties = (properties == null) ? null : (Properties) properties.clone();
	}

	/**
	 * Method addInDoubtXid ...
	 *
	 * @param xid
	 * 		of type Xid
	 */
	public void addInDoubtXid(Xid xid)
	{
		inDoubtXids.add(xid);
	}

	/**
	 * Method removeInDoubtXid ...
	 *
	 * @param xid
	 * 		of type Xid
	 *
	 * @return boolean
	 */
	public boolean removeInDoubtXid(Xid xid)
	{
		for (int i = 0; i < inDoubtXids.size(); i++)
		{
			Xid xid1 = inDoubtXids.get(i);
			if (Arrays.equals(xid1.getGlobalTransactionId(), xid.getGlobalTransactionId()) && Arrays.equals(xid1.getBranchQualifier(), xid.getBranchQualifier()))
			{
				inDoubtXids.remove(xid1);
				return true;
			}
		}
		return false;
	}

	/**
	 * Method getInDoubtXids returns the inDoubtXids of this MockitoXADataSource object.
	 *
	 * @return the inDoubtXids (type Xid[]) of this MockitoXADataSource object.
	 */
	public Xid[] getInDoubtXids()
	{
		return inDoubtXids.toArray(new Xid[inDoubtXids.size()]);
	}

	/**
	 * Method setGetXAConnectionException sets the getXAConnectionException of this MockitoXADataSource object.
	 *
	 * @param ex
	 * 		the getXAConnectionException of this MockitoXADataSource object.
	 */
	public void setGetXAConnectionException(SQLException ex)
	{
		getXAConnectionException = ex;
	}

	/**
	 * Method getUselessThing returns the uselessThing of this MockitoXADataSource object.
	 *
	 * @return the uselessThing (type Object) of this MockitoXADataSource object.
	 */
	public Object getUselessThing()
	{
		return uselessThing;
	}

	/**
	 * Method setUselessThing sets the uselessThing of this MockitoXADataSource object.
	 *
	 * @param uselessThing
	 * 		the uselessThing of this MockitoXADataSource object.
	 */
	public void setUselessThing(Object uselessThing)
	{
		this.uselessThing = uselessThing;
	}

	/**
	 * Method getParentLogger returns the parentLogger of this MockitoXADataSource object.
	 *
	 * @return the parentLogger (type Logger) of this MockitoXADataSource object.
	 *
	 * @throws SQLFeatureNotSupportedException
	 * 		when
	 */
	@Override
	public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException
	{
		throw new SQLFeatureNotSupportedException();
	}

	/**
	 * Method getLoginTimeout returns the loginTimeout of this MockitoXADataSource object.
	 *
	 * @return the loginTimeout (type int) of this MockitoXADataSource object.
	 */
	@Override
	public int getLoginTimeout()
	{
		return 0;
	}


	/**
	 * Method setLoginTimeout sets the loginTimeout of this MockitoXADataSource object.
	 *
	 * @param seconds
	 * 		the loginTimeout of this MockitoXADataSource object.
	 */
	@Override
	public void setLoginTimeout(int seconds)
	{
	}


	/**
	 * Method getLogWriter returns the logWriter of this MockitoXADataSource object.
	 *
	 * @return the logWriter (type PrintWriter) of this MockitoXADataSource object.
	 */
	@Override
	public PrintWriter getLogWriter()
	{
		return null;
	}


	/**
	 * Method setLogWriter sets the logWriter of this MockitoXADataSource object.
	 *
	 * @param out
	 * 		the logWriter of this MockitoXADataSource object.
	 */
	@Override
	public void setLogWriter(PrintWriter out)
	{
	}

	/**
	 * Method getXAConnection returns the XAConnection of this MockitoXADataSource object.
	 *
	 * @return the XAConnection (type XAConnection) of this MockitoXADataSource object.
	 *
	 * @throws SQLException
	 * 		when
	 */
	@Override
	public XAConnection getXAConnection() throws SQLException
	{
		if (staticGetXAConnectionException != null)
		{
			throw staticGetXAConnectionException;
		}
		if (getXAConnectionException != null)
		{
			throw getXAConnectionException;
		}

		// Create an XAResource
		XAResource xaResource = new MockXAResource(this);


		// Setup mock XAConnection
		XAConnection mockXAConnection = mock(XAConnection.class);
		// Handle XAConnection.close(), first time we answer, after that we throw
		doAnswer((Answer<Object>) invocation ->
		{
			EventRecorder eventRecorder = EventRecorder.getEventRecorder(mockXAConnection);
			eventRecorder.addEvent(new XAConnectionCloseEvent(mockXAConnection));
			return null;
		}).doThrow(new SQLException("XAConnection is already closed"))
		  .when(mockXAConnection)
		  .close();

		when(mockXAConnection.getXAResource()).thenReturn(xaResource);
		//        Connection mockConnection = createMockConnection();
		//        when(mockXAConnection.getConnection()).thenReturn(mockConnection);
		doAnswer((Answer<Connection>) invocation -> createMockConnection()).when(mockXAConnection)
		                                                                   .getConnection();

		if (staticCloseXAConnectionException != null)
		{
			doThrow(staticCloseXAConnectionException).when(mockXAConnection)
			                                         .close();
		}

		xaConnections.add(mockXAConnection);
		return mockXAConnection;
	}


	/**
	 * Method getXAConnection ...
	 *
	 * @param user
	 * 		of type String
	 * @param password
	 * 		of type String
	 *
	 * @return XAConnection
	 *
	 * @throws SQLException
	 * 		when
	 */
	@Override
	public XAConnection getXAConnection(String user, String password) throws SQLException
	{
		return getXAConnection();
	}

	/**
	 * Method createMockConnection ...
	 *
	 * @return Connection
	 *
	 * @throws SQLException
	 * 		when
	 */
	public static Connection createMockConnection() throws SQLException
	{
		// Setup mock connection
		Connection mockConnection = mock(Connection.class);

		// Autocommit is always true by default
		when(mockConnection.getAutoCommit()).thenReturn(true);

		// Handle Connection.createStatement()
		when(mockConnection.createStatement()).thenAnswer(mockStatement());
		when(mockConnection.createStatement(anyInt(), anyInt())).thenAnswer(mockStatement());
		when(mockConnection.createStatement(anyInt(), anyInt(), anyInt())).thenAnswer(mockStatement());

		// Handle Connection.prepareStatement()
		when(mockConnection.prepareStatement(anyString())).thenAnswer(mockPreparedStatement());
		when(mockConnection.prepareStatement(anyString(), anyInt())).thenAnswer(mockPreparedStatement());
		when(mockConnection.prepareStatement(anyString(), (int[]) anyObject())).thenAnswer(mockPreparedStatement());
		when(mockConnection.prepareStatement(anyString(), (String[]) anyObject())).thenAnswer(mockPreparedStatement());
		when(mockConnection.prepareStatement(anyString(), anyInt(), anyInt())).thenAnswer(mockPreparedStatement());
		when(mockConnection.prepareStatement(anyString(), anyInt(), anyInt(), anyInt())).thenAnswer(mockPreparedStatement());

		// Handle Connection.prepareCall()
		when(mockConnection.prepareCall(anyString())).thenAnswer(mockCallableStatement());
		when(mockConnection.prepareCall(anyString(), anyInt(), anyInt())).thenAnswer(mockCallableStatement());
		when(mockConnection.prepareCall(anyString(), anyInt(), anyInt(), anyInt())).thenAnswer(mockCallableStatement());

		// Handle Connection.close()
		doAnswer(invocation ->
		         {
			         EventRecorder eventRecorder = EventRecorder.getEventRecorder(mockConnection);
			         eventRecorder.addEvent(new ConnectionCloseEvent(mockConnection));
			         return null;
		         }).doThrow(new SQLException("Connection is already closed"))
		           .when(mockConnection)
		           .close();

		// Handle Connection.commit()
		doAnswer(invocation ->
		         {
			         EventRecorder eventRecorder = EventRecorder.getEventRecorder(mockConnection);
			         eventRecorder.addEvent(new LocalCommitEvent(mockConnection, new Exception()));
			         return null;
		         }).doThrow(new SQLException("Transaction already commited"))
		           .when(mockConnection)
		           .commit();

		// Handle Connection.rollback()
		doAnswer(invocation ->
		         {
			         EventRecorder eventRecorder = EventRecorder.getEventRecorder(mockConnection);
			         eventRecorder.addEvent(new LocalRollbackEvent(mockConnection, new Exception()));
			         return null;
		         }).doThrow(new SQLException("Transaction already rolledback"))
		           .when(mockConnection)
		           .rollback();

		return mockConnection;
	}

	/**
	 * Method mockStatement ...
	 *
	 * @return Answer<Statement>
	 */
	private static Answer<Statement> mockStatement()
	{
		return invocation -> mock(Statement.class);
	}

	/**
	 * Method mockPreparedStatement ...
	 *
	 * @return Answer<PreparedStatement>
	 */
	private static Answer<PreparedStatement> mockPreparedStatement()
	{
		return invocation -> mock(PreparedStatement.class);
	}

	/**
	 * Method mockCallableStatement ...
	 *
	 * @return Answer<CallableStatement>
	 */
	private static Answer<CallableStatement> mockCallableStatement()
	{
		return invocation -> mock(CallableStatement.class);
	}


}
