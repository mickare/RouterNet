package de.rennschnitzel.backbone.net;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import de.rennschnitzel.backbone.Owner;
import de.rennschnitzel.backbone.net.node.NetworkNode;
import de.rennschnitzel.backbone.net.packet.BasePacketHandler;
import de.rennschnitzel.backbone.net.procedure.Procedure;
import de.rennschnitzel.backbone.net.procedure.ProcedureInformation;

public class ProcedureTest {

  private final Random rand = new Random();

  Owner testingOwner;

  NetworkForTesting net_router;
  NetworkForTesting net_client;

  ConnectionForTesting con_router;
  ConnectionForTesting con_client;

  Target target_client;
  Target target_router;

  @Before
  public void setup() {

    testingOwner = new Owner() {
      @Override
      public Logger getLogger() {
        return Logger.getLogger("ChannelTest");
      }
    };

    net_router = new NetworkForTesting();
    net_client = new NetworkForTesting();

    con_router = new ConnectionForTesting(net_router, new BasePacketHandler());
    con_client = new ConnectionForTesting(net_client, new BasePacketHandler());


    target_client = Target.to(net_client.getHome().getId());
    target_router = Target.to(net_router.getHome().getId());

    con_router.connect(con_client);

  }



  @Test
  public void testProc() throws IOException, InterruptedException, TimeoutException, ExecutionException {

    Function<String, String> echo = (s) -> s;
    net_client.getProcedureManager().registerProcedure("echo", echo);

    ProcedureInformation info1 = ProcedureInformation.of("echo", String.class, String.class);
    ProcedureInformation info2 = ProcedureInformation.of("echo", echo);
    assertEquals(info1, info2);

    NetworkNode client_on_router = net_router.getNodes().get(net_client.getHome());
    assertTrue(client_on_router.hasProcedure(info1));

    Procedure<String, String> p1 = info1.getProcedure(net_router, echo);
    Procedure<String, String> p2 = info1.getProcedure(net_router, String.class, String.class);
    assertEquals(p1, p2);

    String helloWorld = "Hello World!";
    assertEquals(helloWorld, p1.call(client_on_router, helloWorld).get(1, TimeUnit.MILLISECONDS));


  }

}
