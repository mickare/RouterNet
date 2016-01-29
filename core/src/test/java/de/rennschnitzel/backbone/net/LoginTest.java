package de.rennschnitzel.backbone.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.rennschnitzel.net.Owner;
import de.rennschnitzel.net.core.login.AuthenticationFactory;
import de.rennschnitzel.net.core.login.ClientLoginEngine;
import de.rennschnitzel.net.core.login.LoginEngine;
import de.rennschnitzel.net.core.login.LoginEngine.State;
import de.rennschnitzel.net.core.login.RouterLoginEngine;
import de.rennschnitzel.net.dummy.DummClientNetwork;
import de.rennschnitzel.net.exception.HandshakeException;
import de.rennschnitzel.net.netty.LocalCoupling;
import de.rennschnitzel.net.netty.LoginHandler;
import de.rennschnitzel.net.netty.PipelineUtils;
import de.rennschnitzel.net.util.SimpleOwner;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;

public class LoginTest {

  Owner testingOwner;
  DummClientNetwork net_router;
  DummClientNetwork net_client;
  EventLoopGroup group = new DefaultEventLoopGroup();

  @Before
  public void setup() {

    testingOwner = new SimpleOwner("ChannelTestOwner", Logger.getLogger("ChannelTest"));

    net_router = new DummClientNetwork(group);
    net_router.setName("Router");
    net_client = new DummClientNetwork(group, net_router.newNotUsedUUID());
    net_client.setName("Client");

  }

  @After
  public void tearDown() {
    group.shutdownGracefully();
  }



  @Test
  public void testLoginSuccessful() throws Throwable {

    final String password = "testLogin";

    RouterLoginEngine routerEngine = new RouterLoginEngine(net_router, AuthenticationFactory.newPasswordForRouter(password));
    ClientLoginEngine clientEngine = new ClientLoginEngine(net_client, AuthenticationFactory.newPasswordForClient(password));


    LocalCoupling con = new LocalCoupling(PipelineUtils.baseInitAnd(ch -> {
      ch.pipeline().addLast(new LoginHandler(routerEngine));
    }), PipelineUtils.baseInitAnd(ch -> {
      ch.pipeline().addLast(new LoginHandler(clientEngine));
    }), group);

    try (AutoCloseable l = con.open()) {
      con.awaitRunning();

      routerEngine.getLoginFuture().await(1000);

      if (routerEngine.getFailureCause() != null) {
        throw routerEngine.getFailureCause();
      }
      if (clientEngine.getFailureCause() != null) {
        throw clientEngine.getFailureCause();
      }

      System.out.println(routerEngine.getState());

      assertTrue(routerEngine.isDone());
      assertTrue(clientEngine.isDone());
      assertEquals(State.SUCCESS, routerEngine.getState());
      assertEquals(State.SUCCESS, clientEngine.getState());
      assertEquals(net_client.getHome().getId(), routerEngine.getLoginId());
      assertEquals(net_router.getHome().getId(), clientEngine.getLoginId());

    } finally {
      con.close();
    }

  }

  @Test
  public void testLoginFailed() throws Exception {

    final String password1 = "testLogin";
    final String password2 = "falsePassword";

    RouterLoginEngine routerEngine = new RouterLoginEngine(net_router, AuthenticationFactory.newPasswordForRouter(password1));
    ClientLoginEngine clientEngine = new ClientLoginEngine(net_client, AuthenticationFactory.newPasswordForClient(password2));


    LocalCoupling con = new LocalCoupling(PipelineUtils.baseInitAnd(ch -> {
      ch.pipeline().addLast(new LoginHandler(routerEngine));
    }), PipelineUtils.baseInitAnd(ch -> {
      ch.pipeline().addLast(new LoginHandler(clientEngine));
    }));

    try (AutoCloseable l = con.open()) {
      con.awaitRunning();

      routerEngine.getLoginFuture().await(1000);

      assertTrue(routerEngine.isDone());
      assertFalse(routerEngine.isSuccess());

      clientEngine.getLoginFuture().await(1000);

      assertTrue(clientEngine.isDone());
      assertFalse(clientEngine.isSuccess());


      assertTrue(routerEngine.isDone());
      assertTrue(clientEngine.isDone());
      assertEquals(State.FAILED, routerEngine.getState());
      assertEquals(State.FAILED, clientEngine.getState());

      assertEquals(LoginEngine.State.AUTH, routerEngine.getFailureState());
      assertEquals(LoginEngine.State.AUTH, clientEngine.getFailureState());
      assertEquals(HandshakeException.class, routerEngine.getFailureCause().getClass());
      assertEquals(HandshakeException.class, clientEngine.getFailureCause().getClass());

      assertEquals("invalid login", routerEngine.getFailureCause().getMessage());
      assertEquals("invalid login", clientEngine.getFailureCause().getMessage());

    } finally {
      con.close();
    }



  }

}
