# GedMarc Update

* JRE 11 Full JPMS
* SonarLinted to JRE8
    - Removed SL4J for JDK
    - Min Compat JRE8
    - Try with resources on files and streams
    - Removed OSGI for JDK OSGi and Modules
* Dependency updates for compatibility
* Removed links to desktop (gui package)
* Cant deploy to that maven group, so the artifact is located under com.jwebmp.jre11
[Here]()
* Module name is <code>tm.bitronix.btm</code> 

Module is defined as 
```
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
	requires static cglib;

	requires java.sql;
}
```

# Details below for original project

#### General Information ####
* [Overview](https://github.com/bitronix/btm/wiki/Overview)
* [FAQ](https://github.com/bitronix/btm/wiki/FAQ)

#### Configuration ####
* [Transaction manager configuration](https://github.com/bitronix/btm/wiki/Transaction-manager-configuration)
* [Resource loader configuration](https://github.com/bitronix/btm/wiki/Resource-loader-configuration)
