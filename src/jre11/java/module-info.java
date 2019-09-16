module tm.bitronix.btm {
	exports bitronix.tm;
	exports bitronix.tm.resource.jdbc;
	exports bitronix.tm.jndi;
	exports bitronix.tm.resource;

	requires java.transaction.xa;
	requires java.naming;
	requires java.transaction;

	requires java.management;
	requires java.management.rmi;

	requires static javax.jms;
	requires static cglib;

	requires java.sql;

	requires javassist;

	opens bitronix.tm.resource.jdbc.proxy;

}
