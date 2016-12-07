RouterNet
=========

RouterNet is a Java 8 software packet that provides a simple messaging router network based on Netty. Messages are forwarded by the router to recipients. Besides byte streams also simple object serialization and RPC (Remote Procedure Calls) allow a free, easy and dynamic development of network based applications.

Required JDK 1.8 or higher.
Used libraries:
- lombok (https://projectlombok.org/)
- Google Guava (https://github.com/google/guava)
- Google GSON (https://github.com/google/gson)
- Protocol Buffers (https://github.com/google/protobuf)
- Netty (http://netty.io/)
- FST (https://github.com/RuedigerMoeller/fast-serialization)
- typetools (https://github.com/jhalterman/typetools)

Client
------

Add the dependency of the client library to your maven project.
```xml
<dependency>
	<groupId>de.mickare.net</groupId>
	<artifactId>client</artifactId>
	<version>0.0.1</version>
</dependency>
```


Usage
-----
Run the router and start your client.