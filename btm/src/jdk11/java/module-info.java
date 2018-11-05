module tm.bitronix.btm {

	exports bitronix.tm;
	exports bitronix.tm.utils;
	exports bitronix.tm.resource.jdbc;

	requires java.transaction.xa;
	requires java.naming;
	requires java.transaction;

	requires java.management.rmi;

	requires jms;
	requires org.slf4j;
	requires java.desktop;
	requires java.sql;
	requires java.management;
	requires org.osgi.core;
	requires javassist;
	requires beta.jboss.rmi.api_1_0;
	requires cglib;
}