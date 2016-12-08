RouterNet
=========
[![Build Status](https://travis-ci.org/mickare/RouterNet.svg?branch=master)](https://travis-ci.org/mickare/RouterNet)

RouterNet is a Java 8 software packet that provides a simple router network based on Netty. Messages are forwarded by the router to recipients through virtual tunnels. Besides byte streams also simple object serialization and RPC (Remote Procedure Calls) allow a free, easy and dynamic development of network based applications.

Required JDK 1.8 or higher.

Used libraries:
- lombok (https://projectlombok.org/)
- Google Guava (https://github.com/google/guava)
- Google GSON (https://github.com/google/gson)
- Protocol Buffers (https://github.com/google/protobuf)
- Netty (http://netty.io/)
- FST (https://github.com/RuedigerMoeller/fast-serialization)
- typetools (https://github.com/jhalterman/typetools)

Features
----------------
Messages are transported through virtual tunnels. That means before a client can send and receive messages it has first register it by name and type at the router.
If a message is send to a client that has not registered the used tunnel, the message will be dropped. Altough this means more overhead to register the tunnels it reduces the overall network traffic when broadcasting.

Clients are identified by a unique id (UUID). Also can clients be dynamically assigned to groups. Messages can be send to UUIDs or groups while also being able to exclude some recipients. This should give developers a maximum control on how to spread messages and informations.

Clients can be configured to automatically reconnect to the network, force a restart, shutdown or wait on custom actions.


Custom Client
-------------

Add the dependency of the client library to your maven project.
```xml
<dependency>
	<groupId>de.mickare.net</groupId>
	<artifactId>client</artifactId>
	<version>0.0.1</version>
</dependency>
```

You can see an example initialization of the client here: https://github.com/mickare/RouterNet-Minecraft/blob/master/bukkit/src/main/java/de/mickare/routernet/mc/bukkit/NetPlugin.java


Usage
-----

Broadcasting String messages:
```java
public class ExampleTunnel implements Owner {

private SubTunnelDescriptor<ObjectTunnel<String>> broadcastTunnel = TunnelDescriptors.getObjectTunnel( "broadcast", String.class );

public void register() {

    Net.getTunnel( broadcastTunnel ).registerListener( this, ( msg ) -> {
        getLogger().info("received: " + msg);
    });

}

public void broadcast(String msg) {
    Net.getTunnel( broadcastTunnel ).send( Target.toAll(), msg );
}

@Override
public String getName() {
    return "ExampleTunnel";
}

@Override
public Logger getLogger() {
    return Logger.getGlobal();
}

}

```

RPC (Remote Procedure Call) on multiple servers:
```java
public class ExampleRPC implements Owner {

private CallableRegisteredProcedure<Integer, String> myProcedure;

public void register() {

    this.myProcedure = Procedure.of( "myProcedure", this::myProcedureFunc ).register();

}

public String myProcedureFunc(Integer i) {
    return Net.getHome().getId().toString() + " - " + i;
}

public void callProcedureFunc(Integer i) {

        myProcedure.call( Target.toAll(), i ).addListener( results -> {
            
            StringBuilder sb = new StringBuilder();
            
            for ( ProcedureCallResult<Integer, String> result : results ) {
                
                if ( result.isSuccess() ) {
                    
                    sb.append( result.getNode().toString() ).append( ":\n" );
                    try {
                        sb.append( result.get() );
                        sb.append( "\n" );
                    } catch ( Exception e ) {
                    }
                    
                } else {
                    getLogger().log( Level.WARNING, "Failed rpc from: " + result.getNode().toString(), result.cause() );
                }
                
            }
            
            getLogger().info( sb.toString() );
        } );

}

@Override
public String getName() {
    return "ExampleRPC";
}

@Override
public Logger getLogger() {
    return Logger.getGlobal();
}

}

```

For more usage examples look at RouterNet-Minecraft(https://github.com/mickare/RouterNet-Minecraft/blob/master/example/bukkit-example/src/main/java/de/mickare/routernet/mc/bukkitexample/BukkitExamplePlugin.java).

TODOs
-----
- Documentation in Code
- Wiki
