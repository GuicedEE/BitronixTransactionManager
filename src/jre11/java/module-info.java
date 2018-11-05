module tm.bitronix.btm {
	exports bitronix.tm;
	exports bitronix.tm.utils;
	exports bitronix.tm.resource.jdbc;
	exports bitronix.tm.jndi;

	requires java.transaction.xa;
	requires java.naming;
	requires java.transaction;

	requires java.management.rmi;

	requires jms;
	requires org.slf4j;

	requires java.sql;
	requires java.management;

	//requires javassist;
}