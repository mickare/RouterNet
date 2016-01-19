package de.rennschnitzel.backbone.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import de.rennschnitzel.net.Owner;
import de.rennschnitzel.net.core.Node;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.core.procedure.Procedure;
import de.rennschnitzel.net.core.procedure.ProcedureCallResult;
import de.rennschnitzel.net.core.procedure.ProcedureInformation;
import de.rennschnitzel.net.core.procedure.RegisteredProcedure;
import de.rennschnitzel.net.dummy.DummyConnection;
import de.rennschnitzel.net.dummy.DummyNetwork;

public class ProcedureTest {

  Owner testingOwner;

  DummyNetwork net_router;
  DummyNetwork net_client;

  DummyConnection con_router;
  DummyConnection con_client;

  Target target_client;
  Target target_router;

  @Before
  public void setup() {

    testingOwner = new Owner() {
      @Override
      public Logger getLogger() {
        return Logger.getLogger("ProcedureTest");
      }

      @Override
      public String getName() {
        return "ProcedureTestOwner";
      }
    };

    net_router = new DummyNetwork(new HomeNode(new UUID(0, 1)));
    net_client = new DummyNetwork(new HomeNode(new UUID(0, 2)));

    con_router = new DummyConnection(net_router, new BasePacketHandler<>());
    con_client = new DummyConnection(net_client, new BasePacketHandler<>());

    net_router.setConnection(con_router);
    net_client.setConnection(con_client);

    target_client = Target.to(net_client.getHome().getId());
    target_router = Target.to(net_router.getHome().getId());

    con_router.connect(con_client);

  }

  public static <T> Function<String, T> test(Class<T> c) {
    return new Function<String, T>() {
      @Override
      public T apply(String t) {
        // TODO Auto-generated method stub
        return null;
      }
    };
  }

  @Test
  public void testFunctionDivergent() throws IOException, InterruptedException, TimeoutException, ExecutionException {

    String helloWorld = "Hello World!";
    final AtomicInteger runCount = new AtomicInteger(0);

    Function<String, Integer> func = (s) -> {
      int i = runCount.incrementAndGet();
      return i;
    };
    net_client.getProcedureManager().registerProcedure("function", func);

    ProcedureInformation info1 = ProcedureInformation.of("function", String.class, Integer.class);
    ProcedureInformation info2 = ProcedureInformation.of("function", func);

    assertEquals(info1, info2);
    assertTrue(info1.equals(info2));
    assertTrue(info1.compareTo(info2) == 0);
    RegisteredProcedure<?, ?> p = net_client.getProcedureManager().getRegisteredProcedure(info1);
    assertNotNull(p);
    assertTrue(p == net_client.getProcedureManager().getRegisteredProcedure(info2));
    assertTrue(net_client.getHome().hasProcedure(info1));
    assertTrue(net_client.getHome().hasProcedure(info2));

    Node client_on_router = net_router.getNode(net_client.getHome().getId());
    assertTrue(client_on_router.getProcedures().size() > 0);
    assertTrue(client_on_router.hasProcedure(info1));
    assertTrue(client_on_router.hasProcedure(info2));

    Procedure<String, Integer> p1 = info1.getProcedure(net_router, func);
    Procedure<String, Integer> p2 = info1.getProcedure(net_router, String.class, Integer.class);
    assertEquals(p1, p2);

    assertEquals(0, runCount.get());
    ProcedureCallResult<String, Integer> call = p1.call(client_on_router, helloWorld);
    assertEquals(1, call.get(1, TimeUnit.MILLISECONDS).intValue());
    assertEquals(1, runCount.get());


  }

  @Test
  public void testFunction() throws IOException, InterruptedException, TimeoutException, ExecutionException {
    String helloWorld = "Hello World!";

    final AtomicInteger runCount = new AtomicInteger(0);
    Function<String, String> func = (t) -> {
      runCount.incrementAndGet();
      return t;
    };

    net_client.getProcedureManager().registerProcedure("function", func);

    ProcedureInformation info1 = ProcedureInformation.of("function", String.class, String.class);
    ProcedureInformation info2 = ProcedureInformation.of("function", func);

    assertEquals(info1, info2);
    assertTrue(info1.equals(info2));
    assertTrue(info1.compareTo(info2) == 0);
    RegisteredProcedure<?, ?> p = net_client.getProcedureManager().getRegisteredProcedure(info1);
    assertNotNull(p);
    assertTrue(p == net_client.getProcedureManager().getRegisteredProcedure(info2));
    assertTrue(net_client.getHome().hasProcedure(info1));
    assertTrue(net_client.getHome().hasProcedure(info2));

    Node client_on_router = net_router.getNode(net_client.getHome().getId());
    assertTrue(client_on_router.getProcedures().size() > 0);
    assertTrue(client_on_router.hasProcedure(info1));
    assertTrue(client_on_router.hasProcedure(info2));

    Procedure<String, String> p1 = info1.getProcedure(net_router, func);
    Procedure<String, String> p2 = info1.getProcedure(net_router, String.class, String.class);
    assertEquals(p1, p2);

    assertEquals(0, runCount.get());
    ProcedureCallResult<String, String> call = p1.call(client_on_router, helloWorld);
    assertEquals(helloWorld, call.get(1, TimeUnit.MILLISECONDS));
    assertEquals(1, runCount.get());


  }



  @Test
  public void testRunnable() throws InterruptedException, TimeoutException, ExecutionException {

    final AtomicInteger runCount = new AtomicInteger(0);
    Runnable func = () -> {
      runCount.incrementAndGet();
    };
    net_client.getProcedureManager().registerProcedure("runnable", func);

    ProcedureInformation info1 = ProcedureInformation.of("runnable", Void.class, Void.class);
    ProcedureInformation info2 = ProcedureInformation.of("runnable", func);

    assertEquals(info1, info2);
    assertTrue(info1.equals(info2));
    assertTrue(info1.compareTo(info2) == 0);
    RegisteredProcedure<?, ?> p = net_client.getProcedureManager().getRegisteredProcedure(info1);
    assertNotNull(p);
    assertTrue(p == net_client.getProcedureManager().getRegisteredProcedure(info2));
    assertTrue(net_client.getHome().hasProcedure(info1));
    assertTrue(net_client.getHome().hasProcedure(info2));

    Node client_on_router = net_router.getNode(net_client.getHome().getId());
    assertTrue(client_on_router.getProcedures().size() > 0);
    assertTrue(client_on_router.hasProcedure(info1));
    assertTrue(client_on_router.hasProcedure(info2));

    Procedure<Void, Void> p1 = info1.getProcedure(net_router, func);
    Procedure<Void, Void> p2 = info1.getProcedure(net_router, Void.class, Void.class);
    assertEquals(p1, p2);

    assertEquals(0, runCount.get());
    ProcedureCallResult<Void, Void> call = p1.call(client_on_router, null);
    assertEquals(null, call.get(1, TimeUnit.MILLISECONDS));
    assertEquals(1, runCount.get());

  }

  @Test
  public void testConsumer() throws InterruptedException, TimeoutException, ExecutionException {
    String helloWorld = "Hello World!";

    final AtomicInteger runCount = new AtomicInteger(0);
    Consumer<String> func = (t) -> {
      if (helloWorld.equals(t)) {
        runCount.incrementAndGet();
      } else {
        fail();
      }
    };
    net_client.getProcedureManager().registerProcedure("consumer", func);

    ProcedureInformation info1 = ProcedureInformation.of("consumer", String.class, Void.class);
    ProcedureInformation info2 = ProcedureInformation.of("consumer", func);

    assertEquals(info1, info2);
    assertTrue(info1.equals(info2));
    assertTrue(info1.compareTo(info2) == 0);
    RegisteredProcedure<?, ?> p = net_client.getProcedureManager().getRegisteredProcedure(info1);
    assertNotNull(p);
    assertTrue(p == net_client.getProcedureManager().getRegisteredProcedure(info2));
    assertTrue(net_client.getHome().hasProcedure(info1));
    assertTrue(net_client.getHome().hasProcedure(info2));

    Node client_on_router = net_router.getNode(net_client.getHome().getId());
    assertTrue(client_on_router.getProcedures().size() > 0);
    assertTrue(client_on_router.hasProcedure(info1));
    assertTrue(client_on_router.hasProcedure(info2));

    Procedure<String, Void> p1 = info1.getProcedure(net_router, func);
    Procedure<String, Void> p2 = info1.getProcedure(net_router, String.class, Void.class);
    assertEquals(p1, p2);

    assertEquals(0, runCount.get());
    ProcedureCallResult<String, Void> call = p1.call(client_on_router, helloWorld);
    assertEquals(null, call.get(1, TimeUnit.MILLISECONDS));
    assertEquals(1, runCount.get());

  }


  @Test
  public void testSupplier() throws InterruptedException, TimeoutException, ExecutionException {
    String helloWorld = "Hello World!";

    final AtomicInteger runCount = new AtomicInteger(0);
    Supplier<String> func = () -> {
      runCount.incrementAndGet();
      return helloWorld;
    };
    net_client.getProcedureManager().registerProcedure("supplier", func);

    ProcedureInformation info1 = ProcedureInformation.of("supplier", Void.class, String.class);
    ProcedureInformation info2 = ProcedureInformation.of("supplier", func);

    assertEquals(info1, info2);
    assertTrue(info1.equals(info2));
    assertTrue(info1.compareTo(info2) == 0);
    RegisteredProcedure<?, ?> p = net_client.getProcedureManager().getRegisteredProcedure(info1);
    assertNotNull(p);
    assertTrue(p == net_client.getProcedureManager().getRegisteredProcedure(info2));
    assertTrue(net_client.getHome().hasProcedure(info1));
    assertTrue(net_client.getHome().hasProcedure(info2));

    Node client_on_router = net_router.getNode(net_client.getHome().getId());
    assertTrue(client_on_router.getProcedures().size() > 0);
    assertTrue(client_on_router.hasProcedure(info1));
    assertTrue(client_on_router.hasProcedure(info2));

    Procedure<Void, String> p1 = info1.getProcedure(net_router, func);
    Procedure<Void, String> p2 = info1.getProcedure(net_router, Void.class, String.class);
    assertEquals(p1, p2);

    assertEquals(0, runCount.get());
    ProcedureCallResult<Void, String> call = p1.call(client_on_router, null);
    assertEquals(helloWorld, call.get(1, TimeUnit.MILLISECONDS));
    assertEquals(1, runCount.get());

  }

}
