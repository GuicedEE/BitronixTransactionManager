module tm.bitronix.btm {
	exports bitronix.tm;
	exports bitronix.tm.resource.jdbc;
	exports bitronix.tm.jndi;
	exports bitronix.tm.resource;

	requires transitive java.transaction.xa;
	requires java.naming;
	requires transitive jakarta.transaction;

	requires java.management;
	requires java.management.rmi;

	//requires static jakarta.jms;
	requires static cglib;

	requires transitive java.sql;

//	requires javassist;

	opens bitronix.tm.resource.jdbc.proxy;

}
