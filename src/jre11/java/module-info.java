module tm.bitronix.btm {
	exports bitronix.tm;
	exports bitronix.tm.utils;
	exports bitronix.tm.resource.jdbc;
	exports bitronix.tm.jndi;

	requires java.transaction.xa;
	requires java.naming;
	requires java.transaction;

	requires java.management;
	requires java.management.rmi;

	requires static jms;
	requires static org.osgi.core;

	requires java.sql;

	requires org.slf4j;
}