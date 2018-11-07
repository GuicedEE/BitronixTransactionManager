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
package bitronix.tm.mock;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.LogDebugCheck;
import bitronix.tm.mock.events.*;

import javax.transaction.InvalidTransactionException;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * @author Ludovic Orban
 */
public class NewJdbcSuspendResumeMockTest
		extends AbstractMockJdbcTest
{

	private final static java.util.logging.Logger log = java.util.logging.Logger.getLogger(NewJdbcSuspendResumeMockTest.class.toString());

	public void testSimpleAssertions() throws Exception
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting TM");
		}
		BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

		assertNull(tm.suspend());

		try
		{
			tm.resume(null);
			fail("expected InvalidTransactionException");
		}
		catch (InvalidTransactionException ex)
		{
			assertEquals("resumed transaction cannot be null", ex.getMessage());
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** before begin");
		}
		tm.begin();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** after begin");
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting connection from DS1");
		}
		Connection connection1 = poolingDataSource1.getConnection();
		connection1.createStatement();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** closing connection 1");
		}
		connection1.close();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** committing");
		}
		tm.commit();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** TX is done");
		}
	}

	public void testSimpleWorkingCase() throws Exception
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting TM");
		}
		BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** before begin");
		}
		tm.begin();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** after begin");
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting connection from DS1");
		}
		Connection connection1 = poolingDataSource1.getConnection();
		connection1.createStatement();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** suspending");
		}
		Transaction t1 = tm.suspend();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** resuming");
		}
		tm.resume(t1);

		connection1.createStatement();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** closing connection 1");
		}
		connection1.close();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** committing");
		}
		tm.commit();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** TX is done");
		}

		// check flow
		List orderedEvents = EventRecorder.getOrderedEvents();
		log.info(EventRecorder.dumpToString());

		assertEquals(13, orderedEvents.size());
		int i = 0;
		assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                                 .getPoolingDataSource()
		                                                                                 .getUniqueName());
		assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(true, ((XAResourceIsSameRmEvent) orderedEvents.get(i++)).isSameRm());
		assertEquals(XAResource.TMJOIN, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(true, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
		assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                               .getPoolingDataSource()
		                                                                               .getUniqueName());
	}

	public void testNoTmJoin() throws Exception
	{
		poolingDataSource1.setUseTmJoin(false);

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting TM");
		}
		BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** before begin");
		}
		tm.begin();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** after begin");
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting connection from DS1");
		}
		Connection connection1 = poolingDataSource1.getConnection();
		connection1.createStatement();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** suspending");
		}
		Transaction t1 = tm.suspend();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** resuming");
		}
		tm.resume(t1);

		connection1.createStatement();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** closing connection 1");
		}
		connection1.close();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** committing");
		}
		tm.commit();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** TX is done");
		}

		// check flow
		List orderedEvents = EventRecorder.getOrderedEvents();
		log.info(EventRecorder.dumpToString());

		assertEquals(15, orderedEvents.size());
		int i = 0;
		assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                                 .getPoolingDataSource()
		                                                                                 .getUniqueName());
		assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(XAResource.XA_OK, ((XAResourcePrepareEvent) orderedEvents.get(i++)).getReturnCode());
		assertEquals(XAResource.XA_OK, ((XAResourcePrepareEvent) orderedEvents.get(i++)).getReturnCode());
		assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(false, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
		assertEquals(false, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
		assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                               .getPoolingDataSource()
		                                                                               .getUniqueName());
	}

	public void testReEnlistmentAfterSuspend() throws Exception
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting TM");
		}
		BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** before begin");
		}
		tm.begin();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** after begin");
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting connection from DS1");
		}
		Connection connection1 = poolingDataSource1.getConnection();
		connection1.createStatement();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** suspending");
		}
		Transaction t1 = tm.suspend();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** before begin2");
		}
		tm.begin();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** after begin2");
		}
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** reusing connection 1");
		}
		connection1.createStatement();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** marking subTX as rollback only");
		}
		tm.setRollbackOnly();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** rolling back");
		}
		tm.rollback();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** rolling back");
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** subTX is done");
		}
		tm.resume(t1);

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** closing connection 1");
		}
		connection1.close();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** committing");
		}
		tm.commit();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** TX is done");
		}

		// check flow
		List orderedEvents = EventRecorder.getOrderedEvents();
		log.info(EventRecorder.dumpToString());

		assertEquals(20, orderedEvents.size());
		int i = 0;
		assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                                 .getPoolingDataSource()
		                                                                                 .getUniqueName());
		assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());

		assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(Status.STATUS_MARKED_ROLLBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(Status.STATUS_ROLLING_BACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(XAResourceRollbackEvent.class, orderedEvents.get(i++)
		                                                         .getClass());
		assertEquals(Status.STATUS_ROLLEDBACK, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());

		assertEquals(true, ((XAResourceIsSameRmEvent) orderedEvents.get(i++)).isSameRm());
		assertEquals(XAResource.TMJOIN, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());

		assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(true, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
		assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                               .getPoolingDataSource()
		                                                                               .getUniqueName());
	}

	public void testClosingSuspendedConnections() throws Exception
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting TM");
		}
		BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** before begin");
		}
		tm.begin();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting connection from DS1");
		}
		Connection connection1 = poolingDataSource1.getConnection();
		connection1.createStatement();

		assertEquals(POOL_SIZE - 1, getPool(poolingDataSource1).inPoolSize());

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** suspending");
		}
		Transaction t1 = tm.suspend();

		assertEquals(POOL_SIZE - 1, getPool(poolingDataSource1).inPoolSize());

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** closing connection 1 too eagerly");
		}
		try
		{
			// TODO: the TM tries to 'veto' the connection close here like the old pool did.
			// Instead, close the resource immediately or defer its release.
			connection1.close();
			fail("successfully closed a connection participating in a global transaction, this should never be allowed");
		}
		catch (SQLException ex)
		{
			assertEquals("cannot close a resource when its XAResource is taking part in an unfinished global transaction", ex.getCause()
			                                                                                                                 .getMessage());
		}
		assertFalse(connection1.isClosed());

		assertEquals(POOL_SIZE - 1, getPool(poolingDataSource1).inPoolSize());

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** resuming");
		}
		tm.resume(t1);

		assertEquals(POOL_SIZE - 1, getPool(poolingDataSource1).inPoolSize());

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** committing");
		}
		tm.commit();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** TX is done");
		}

		assertEquals(POOL_SIZE - 1, getPool(poolingDataSource1).inPoolSize());

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** closing connection 1");
		}
		connection1.close();

		assertEquals(POOL_SIZE, getPool(poolingDataSource1).inPoolSize());

		// check flow
		List orderedEvents = EventRecorder.getOrderedEvents();
		log.info(EventRecorder.dumpToString());

		assertEquals(13, orderedEvents.size());
		int i = 0;
		assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                                 .getPoolingDataSource()
		                                                                                 .getUniqueName());
		assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(true, ((XAResourceIsSameRmEvent) orderedEvents.get(i++)).isSameRm());
		assertEquals(XAResource.TMJOIN, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(true, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
		assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                               .getPoolingDataSource()
		                                                                               .getUniqueName());
	}

	public void testInterleavedLocalGlobalTransactions() throws Exception
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting TM");
		}
		BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** before begin");
		}
		tm.begin();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** after begin");
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting connection from DS1");
		}
		Connection connection1 = poolingDataSource1.getConnection();
		connection1.createStatement();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** suspending");
		}
		Transaction t1 = tm.suspend();

		Connection connection2 = poolingDataSource1.getConnection();
		assertNull(tm.getTransaction());
		connection2.createStatement();
		connection2.close();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** resuming");
		}
		tm.resume(t1);

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** closing connection 1");
		}
		connection1.close();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** committing");
		}
		tm.commit();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** TX is done");
		}

		// check flow
		List orderedEvents = EventRecorder.getOrderedEvents();
		log.info(EventRecorder.dumpToString());

		assertEquals(15, orderedEvents.size());
		int i = 0;
		assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                                 .getPoolingDataSource()
		                                                                                 .getUniqueName());
		assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                                 .getPoolingDataSource()
		                                                                                 .getUniqueName());
		assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                               .getPoolingDataSource()
		                                                                               .getUniqueName());
		assertEquals(true, ((XAResourceIsSameRmEvent) orderedEvents.get(i++)).isSameRm());
		assertEquals(XAResource.TMJOIN, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(true, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
		assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i)).getStatus());
		assertEquals(1, ((JournalLogEvent) orderedEvents.get(i++)).getJndiNames()
		                                                          .size());
		assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                               .getPoolingDataSource()
		                                                                               .getUniqueName());
	}

	public void testInterleavedGlobalGlobalTransactionsWithDifferentConnections() throws Exception
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting TM");
		}
		BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** before begin");
		}
		tm.begin();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** after begin");
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting connection from DS1");
		}
		Connection connection1 = poolingDataSource1.getConnection();
		connection1.createStatement();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer(" *** suspending transaction");
		}
		Transaction t1 = tm.suspend();
		assertNull(tm.getTransaction());

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer(" *** begin interleaved transaction");
		}
		tm.begin();
		Connection connection2 = poolingDataSource1.getConnection();
		connection2.createStatement();
		connection2.close();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer(" *** commit interleaved transaction");
		}
		tm.commit();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer(" *** resuming transaction");
		}
		tm.resume(t1);

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** closing connection 1");
		}
		connection1.close();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** committing");
		}
		tm.commit();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** TX is done");
		}

		// check flow
		List orderedEvents = EventRecorder.getOrderedEvents();
		log.info(EventRecorder.dumpToString());

		assertEquals(23, orderedEvents.size());
		int i = 0;
		assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                                 .getPoolingDataSource()
		                                                                                 .getUniqueName());
		assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());

		// interleaved transaction
		assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                                 .getPoolingDataSource()
		                                                                                 .getUniqueName());
		assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(true, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
		assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                               .getPoolingDataSource()
		                                                                               .getUniqueName());

		assertEquals(true, ((XAResourceIsSameRmEvent) orderedEvents.get(i++)).isSameRm());
		assertEquals(XAResource.TMJOIN, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(true, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
		assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i)).getStatus());
		assertEquals(1, ((JournalLogEvent) orderedEvents.get(i++)).getJndiNames()
		                                                          .size());
		assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                               .getPoolingDataSource()
		                                                                               .getUniqueName());
	}

	public void testInterleavedGlobalGlobalTransactionsWithDifferentConnectionsLateSuspend() throws Exception
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting TM");
		}
		BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** before begin");
		}
		tm.begin();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** after begin");
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting connection from DS1");
		}
		Connection connection1 = poolingDataSource1.getConnection();
		connection1.createStatement();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** closing connection 1");
		}
		connection1.close();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer(" *** suspending transaction");
		}
		Transaction t1 = tm.suspend();
		assertNull(tm.getTransaction());

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer(" *** begin interleaved transaction");
		}
		tm.begin();
		Connection connection2 = poolingDataSource1.getConnection();
		assertEquals(POOL_SIZE - 2, getPool(poolingDataSource1).inPoolSize());
		connection2.createStatement();
		connection2.close();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer(" *** commit interleaved transaction");
		}
		tm.commit();
		assertEquals(POOL_SIZE - 1, getPool(poolingDataSource1).inPoolSize());

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer(" *** resuming transaction");
		}
		tm.resume(t1);

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** committing");
		}
		tm.commit();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** TX is done");
		}
		assertEquals(POOL_SIZE, getPool(poolingDataSource1).inPoolSize());

		// check flow
		List orderedEvents = EventRecorder.getOrderedEvents();
		log.info(EventRecorder.dumpToString());

		assertEquals(23, orderedEvents.size());
		int i = 0;
		assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                                 .getPoolingDataSource()
		                                                                                 .getUniqueName());
		assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());

		// interleaved transaction
		assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                                 .getPoolingDataSource()
		                                                                                 .getUniqueName());
		assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(true, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
		assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                               .getPoolingDataSource()
		                                                                               .getUniqueName());

		assertEquals(true, ((XAResourceIsSameRmEvent) orderedEvents.get(i++)).isSameRm());
		assertEquals(XAResource.TMJOIN, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(true, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
		assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i)).getStatus());
		assertEquals(1, ((JournalLogEvent) orderedEvents.get(i++)).getJndiNames()
		                                                          .size());
		assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                               .getPoolingDataSource()
		                                                                               .getUniqueName());
	}

	public void testJoinAfterSuspend() throws Exception
	{
		BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
		tm.begin();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** get C1");
		}
		Connection c1 = poolingDataSource1.getConnection();
		c1.createStatement();
		c1.close();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** get C2");
		}
		Connection c2 = poolingDataSource2.getConnection();
		c2.createStatement();
		c2.close();

		Transaction tx = tm.suspend();
		tm.resume(tx);

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** get C3");
		}
		Connection c3 = poolingDataSource2.getConnection();
		c3.createStatement();
		c3.close();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** get C4");
		}
		Connection c4 = poolingDataSource1.getConnection();
		c4.createStatement();
		c4.close();

		tm.commit();

		// check flow
		List orderedEvents = EventRecorder.getOrderedEvents();
		log.info(EventRecorder.dumpToString());

		assertEquals(25, orderedEvents.size());
		int i = 0;
		assertEquals(Status.STATUS_ACTIVE, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                                 .getPoolingDataSource()
		                                                                                 .getUniqueName());
		assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(DATASOURCE2_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                                 .getPoolingDataSource()
		                                                                                 .getUniqueName());
		assertEquals(XAResource.TMNOFLAGS, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());

		// suspend happens here
		assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
		// resume happens here
		assertEquals(true, ((XAResourceIsSameRmEvent) orderedEvents.get(i++)).isSameRm());
		assertEquals(XAResource.TMJOIN, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());

		XAResourceIsSameRmEvent evt = (XAResourceIsSameRmEvent) orderedEvents.get(i++);
		XAResource src = (XAResource) evt.getSource();
		XAResource comp = evt.getXAResource();
		assertTrue(poolingDataSource2.findXAResourceHolder(src) != null);
		assertTrue(poolingDataSource2.findXAResourceHolder(comp) != null);

		assertEquals(XAResource.TMJOIN, ((XAResourceStartEvent) orderedEvents.get(i++)).getFlag());

		assertEquals(DATASOURCE2_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                                 .getPoolingDataSource()
		                                                                                 .getUniqueName());
		assertEquals(DATASOURCE1_NAME, ((ConnectionDequeuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                                 .getPoolingDataSource()
		                                                                                 .getUniqueName());

		assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());
		assertEquals(XAResource.TMSUCCESS, ((XAResourceEndEvent) orderedEvents.get(i++)).getFlag());

		assertEquals(Status.STATUS_PREPARING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(XAResource.XA_OK, ((XAResourcePrepareEvent) orderedEvents.get(i++)).getReturnCode());
		assertEquals(XAResource.XA_OK, ((XAResourcePrepareEvent) orderedEvents.get(i++)).getReturnCode());
		assertEquals(Status.STATUS_PREPARED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(Status.STATUS_COMMITTING, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(false, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
		assertEquals(false, ((XAResourceCommitEvent) orderedEvents.get(i++)).isOnePhase());
		assertEquals(Status.STATUS_COMMITTED, ((JournalLogEvent) orderedEvents.get(i++)).getStatus());
		assertEquals(DATASOURCE2_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                               .getPoolingDataSource()
		                                                                               .getUniqueName());
		assertEquals(DATASOURCE1_NAME, ((ConnectionQueuedEvent) orderedEvents.get(i++)).getPooledConnectionImpl()
		                                                                               .getPoolingDataSource()
		                                                                               .getUniqueName());
	}

	public void testReusePreparedStatementAfterSuspendResume() throws Exception
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting TM");
		}
		BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** before begin");
		}
		tm.begin();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** after begin");
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting connection from DS1");
		}
		Connection connection1 = poolingDataSource1.getConnection();

		Transaction tx = tm.suspend();
		tm.resume(tx);

		connection1.prepareStatement("some sql");

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** closing connection 1");
		}
		connection1.close();

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** committing");
		}
		tm.commit();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** TX is done");
		}

		// check flow
		List orderedEvents = EventRecorder.getOrderedEvents();
		log.info(EventRecorder.dumpToString());
	}

	public void testSuspendResumeSeparateThreads() throws Exception
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** getting TM");
		}
		BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** before begin");
		}
		tm.begin();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** after begin");
		}

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** suspending transaction");
		}
		Transaction suspended = tm.suspend();

		assertNull(tm.getCurrentTransaction());

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** before 2nd begin");
		}
		tm.begin();
		assertNotNull(tm.getCurrentTransaction());

		Thread thread = new Thread(() ->
		                           {
			                           if (LogDebugCheck.isDebugEnabled())
			                           {
				                           log.finer("*** getting TM");
			                           }

			                           try
			                           {
				                           if (LogDebugCheck.isDebugEnabled())
				                           {
					                           log.finer("*** resuming transaction in new thread");
				                           }
				                           tm.resume(suspended);
				                           if (LogDebugCheck.isDebugEnabled())
				                           {
					                           log.finer("*** committing transaction in new thread");
				                           }
				                           tm.commit();
				                           if (LogDebugCheck.isDebugEnabled())
				                           {
					                           log.finer("*** new thread commit complete, exiting");
				                           }
				                           assertNull(tm.getCurrentTransaction());
			                           }
			                           catch (Exception e)
			                           {
				                           fail(e.getMessage());
			                           }
		                           });
		thread.start();
		thread.join();

		assertNotNull(tm.getCurrentTransaction());
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** committing transaction in main thread");
		}
		tm.commit();
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("*** main thread complete");
		}
		assertNull(tm.getCurrentTransaction());
	}
}
