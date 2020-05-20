module tm.bitronix.btm {
	exports bitronix.tm;
	exports bitronix.tm.resource.jdbc;
	exports bitronix.tm.jndi;
	exports bitronix.tm.resource;

	requires transitive java.transaction.xa;
	requires static java.naming;
	requires transitive java.transaction;

	requires static java.management;
	requires static java.management.rmi;

	requires static javax.jms;
	requires static cglib;

	requires transitive java.sql;

	requires javassist;

	opens bitronix.tm.resource.jdbc.proxy;

}
