package de.rennschnitzel.backbone.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import de.rennschnitzel.net.Owner;
import de.rennschnitzel.net.core.handshake.AbstractHandshakeHandler.State;
import de.rennschnitzel.net.core.handshake.AuthenticationFactory;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.dummy.DummyClientHandshakeHandler;
import de.rennschnitzel.net.dummy.DummyConnection;
import de.rennschnitzel.net.dummy.DummyNetwork;
import de.rennschnitzel.net.dummy.DummyRouterHandshakeHandler;
import de.rennschnitzel.net.util.SimpleOwner;

public class LoginTest {

  Owner testingOwner;

  DummyNetwork net_router;
  DummyNetwork net_client;

  @Before
  public void setup() {

    testingOwner = new SimpleOwner("ChannelTestOwner", Logger.getLogger("ChannelTest"));

    net_router = new DummyNetwork();
    do {
      net_client = new DummyNetwork();
    } while (net_client.getHome().getId().equals(net_router.getHome().getId()));

  }

  @Test
  public void testLoginSuccessful() throws IOException, InterruptedException, ExecutionException, TimeoutException {

    final String password = "testLogin";

    DummyRouterHandshakeHandler routerHandler = new DummyRouterHandshakeHandler("routerHandshake", net_router,
        AuthenticationFactory.newPasswordForRouter(password), new BasePacketHandler<>());
    DummyClientHandshakeHandler clientHandler = new DummyClientHandshakeHandler("clientHandshake", net_client,
        AuthenticationFactory.newPasswordForClient(password), new BasePacketHandler<>());


    DummyConnection con_router = new DummyConnection(net_router, routerHandler);
    DummyConnection con_client = new DummyConnection(net_client, clientHandler);

    con_client.connect(con_router);

    assertEquals(State.SUCCESS, routerHandler.getState());
    assertEquals(State.SUCCESS, clientHandler.getState());
    assertTrue(routerHandler.isDone());
    assertTrue(clientHandler.isDone());
    assertEquals(con_router, routerHandler.get(1, TimeUnit.SECONDS));
    assertEquals(con_client, clientHandler.get(1, TimeUnit.SECONDS));
  }

  @Test
  public void testLoginFailed() throws IOException {

    final String password1 = "testLogin";
    final String password2 = "falsePassword";

    DummyRouterHandshakeHandler routerHandler = new DummyRouterHandshakeHandler("routerHandshake", net_router,
        AuthenticationFactory.newPasswordForRouter(password1), new BasePacketHandler<>());
    DummyClientHandshakeHandler clientHandler = new DummyClientHandshakeHandler("clientHandshake", net_client,
        AuthenticationFactory.newPasswordForClient(password2), new BasePacketHandler<>());


    DummyConnection con_router = new DummyConnection(net_router, routerHandler);
    DummyConnection con_client = new DummyConnection(net_client, clientHandler);

    boolean catched = false;
    try {
      con_client.connect(con_router);
    } catch (Exception e) {
      catched = true;
      assertEquals("invalid login", e.getMessage());
    }
    assertTrue(catched);

    assertEquals(State.FAILED, routerHandler.getState());
    assertEquals(State.FAILED, clientHandler.getState());
    assertTrue(routerHandler.isDone());
    assertTrue(clientHandler.isDone());

    catched = false;
    try {
      assertEquals(con_router, routerHandler.get(1, TimeUnit.MICROSECONDS));
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      catched = true;
      assertEquals("de.rennschnitzel.net.exception.HandshakeException: invalid login", e.getMessage());
    }
    assertTrue(catched);

    catched = false;
    try {
      assertEquals(con_client, clientHandler.get(1, TimeUnit.MICROSECONDS));
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      catched = true;
      assertEquals("de.rennschnitzel.net.exception.HandshakeException: invalid login", e.getMessage());
    }
    assertTrue(catched);

  }

}
