package de.rennschnitzel.backbone.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.rennschnitzel.net.Owner;
import de.rennschnitzel.net.core.login.AuthenticationFactory;
import de.rennschnitzel.net.core.login.LoginHandler;
import de.rennschnitzel.net.core.login.LoginHandler.State;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.dummy.DummyConnection;
import de.rennschnitzel.net.dummy.DummyLoginClientHandler;
import de.rennschnitzel.net.dummy.DummyLoginRouterHandler;
import de.rennschnitzel.net.dummy.DummyNetwork;
import de.rennschnitzel.net.exception.HandshakeException;
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

  @After
  public void tearDown() {}

  @Test
  public void testLoginSuccessful() throws IOException, InterruptedException, ExecutionException, TimeoutException {

    final String password = "testLogin";

    DummyLoginRouterHandler routerHandler = new DummyLoginRouterHandler("routerHandshake", net_router,
        AuthenticationFactory.newPasswordForRouter(password), new BasePacketHandler<>());
    DummyLoginClientHandler clientHandler = new DummyLoginClientHandler("clientHandshake", net_client,
        AuthenticationFactory.newPasswordForClient(password), new BasePacketHandler<>());


    DummyConnection con_router = new DummyConnection(net_router, routerHandler);
    DummyConnection con_client = new DummyConnection(net_client, clientHandler);

    con_client.connect(con_router);

    assertTrue(routerHandler.isDone());
    assertTrue(clientHandler.isDone());
    assertEquals(State.SUCCESS, routerHandler.getState());
    assertEquals(State.SUCCESS, clientHandler.getState());
    assertEquals(net_client.getHome().getId(), routerHandler.getId());
    assertEquals(net_router.getHome().getId(), clientHandler.getId());
  }

  @Test
  public void testLoginFailed() throws IOException {

    final String password1 = "testLogin";
    final String password2 = "falsePassword";

    DummyLoginRouterHandler routerHandler = new DummyLoginRouterHandler("routerHandshake", net_router,
        AuthenticationFactory.newPasswordForRouter(password1), new BasePacketHandler<>());
    DummyLoginClientHandler clientHandler = new DummyLoginClientHandler("clientHandshake", net_client,
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

    assertTrue(routerHandler.isDone());
    assertTrue(clientHandler.isDone());
    assertEquals(State.FAILED, routerHandler.getState());
    assertEquals(State.FAILED, clientHandler.getState());

    assertEquals(LoginHandler.State.AUTH, routerHandler.getFailureState());
    assertEquals(LoginHandler.State.AUTH, clientHandler.getFailureState());
    assertEquals(HandshakeException.class, routerHandler.getFailureCause().getClass());
    assertEquals(HandshakeException.class, clientHandler.getFailureCause().getClass());
    assertEquals("invalid login", routerHandler.getFailureCause().getMessage());
    assertEquals("invalid login", clientHandler.getFailureCause().getMessage());


  }

}
