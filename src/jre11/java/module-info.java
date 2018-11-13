module tm.bitronix.btm {
	exports bitronix.tm;
	exports bitronix.tm.resource.jdbc;
	exports bitronix.tm.jndi;

	requires java.transaction.xa;
	requires java.naming;
	requires java.transaction;

	requires java.management;
	requires java.management.rmi;

	requires static jms;
	requires static cglib;

	requires java.sql;

	requires static javassist;
	requires static beta.jboss.rmi.api_1_0;
}
