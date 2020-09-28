module tm.bitronix.btm {
	exports bitronix.tm;
	exports bitronix.tm.resource.jdbc;
	exports bitronix.tm.jndi;
	exports bitronix.tm.resource;

	requires transitive java.transaction.xa;
	requires java.naming;
	requires transitive java.transaction;

	requires java.management;
	requires java.management.rmi;

	requires static javax.jms;
	requires static cglib;
	requires static javassist;

	requires transitive java.sql;

	opens bitronix.tm.resource.jdbc.proxy;

}
