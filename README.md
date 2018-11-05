# GedMarc Update

* JRE 11 Full JPMS
* Dependency updates for compatibility
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
    
    requires java.management.rmi;
    
    requires jms;
    requires org.slf4j;
    
    requires java.sql;
    requires java.management;
}
```

# Details below for original project

#### General Information ####
* [Overview](https://github.com/bitronix/btm/wiki/Overview)
* [FAQ](https://github.com/bitronix/btm/wiki/FAQ)

#### Configuration ####
* [Transaction manager configuration](https://github.com/bitronix/btm/wiki/Transaction-manager-configuration)
* [Resource loader configuration](https://github.com/bitronix/btm/wiki/Resource-loader-configuration)
