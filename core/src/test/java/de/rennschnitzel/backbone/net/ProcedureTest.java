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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.rennschnitzel.net.Owner;
import de.rennschnitzel.net.core.Node;
import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.core.Node.HomeNode;
import de.rennschnitzel.net.core.packet.BasePacketHandler;
import de.rennschnitzel.net.core.procedure.BoundProcedure;
import de.rennschnitzel.net.core.procedure.CallableProcedure;
import de.rennschnitzel.net.core.procedure.ProcedureCallResult;
import de.rennschnitzel.net.core.procedure.Procedure;
import de.rennschnitzel.net.core.procedure.CallableRegisteredProcedure;
import de.rennschnitzel.net.dummy.DummyConnection;
import de.rennschnitzel.net.dummy.DummyNetwork;
import de.rennschnitzel.net.util.concurrent.DirectScheduledExecutorService;

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
    DirectScheduledExecutorService.disableWarning();

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

    target_client = Target.to(net_client.getHome().getId());
    target_router = Target.to(net_router.getHome().getId());

    con_router.connect(con_client);

  }

  @After
  public void tearDown() {
    DirectScheduledExecutorService.enableWarning();
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
  public void testUsability() throws InterruptedException, TimeoutException, ExecutionException {

    Function<String, String> usability = (str) -> str + "success";

    // net client was last initialized, so static instance is net_client!
    BoundProcedure<String, String> proc = Procedure.of("usability", usability);
    net_router.getProcedureManager().registerProcedure(proc);
    
    Node node = net_client.getNode(net_router.getHome().getId());
    assertTrue(node.hasProcedure(proc));

    String result = Procedure.of("usability", usability)//
        .call(net_client.getNode(net_router.getHome().getId()), "test").get(1, TimeUnit.MILLISECONDS);
    assertEquals("testsuccess", result);

  }

  @Test
  public void testTypetools() throws IOException, InterruptedException, TimeoutException, ExecutionException {

    String helloWorld = "Hello World!";
    final AtomicInteger runCount = new AtomicInteger(0);

    Function<String, Integer> func = (s) -> {
      int i = runCount.incrementAndGet();
      return i;
    };
    net_client.getProcedureManager().registerProcedure("testTypetools", func);

    Procedure info1 = Procedure.of("testTypetools", String.class, Integer.class);
    Procedure info2 = Procedure.of("testTypetools", func);

    assertEquals(info1, info2);
    assertTrue(info1.equals(info2));
    assertTrue(info1.compareTo(info2) == 0);
    CallableRegisteredProcedure<?, ?> p = net_client.getProcedureManager().getRegisteredProcedure(info1);
    assertNotNull(p);
    assertTrue(p == net_client.getProcedureManager().getRegisteredProcedure(info2));
    assertTrue(net_client.getHome().hasProcedure(info1));
    assertTrue(net_client.getHome().hasProcedure(info2));

    Node client_on_router = net_router.getNode(net_client.getHome().getId());
    assertTrue(client_on_router.getProcedures().size() > 0);
    assertTrue(client_on_router.hasProcedure(info1));
    assertTrue(client_on_router.hasProcedure(info2));

    CallableProcedure<String, Integer> p1 = info1.bind(net_router, func);
    CallableProcedure<String, Integer> p2 = info1.bind(net_router, String.class, Integer.class);
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

    net_client.getProcedureManager().registerProcedure("testFunction", func);

    Procedure info1 = Procedure.of("testFunction", String.class, String.class);
    Procedure info2 = Procedure.of("testFunction", func);

    assertEquals(info1, info2);
    assertTrue(info1.equals(info2));
    assertTrue(info1.compareTo(info2) == 0);
    CallableRegisteredProcedure<?, ?> p = net_client.getProcedureManager().getRegisteredProcedure(info1);
    assertNotNull(p);
    assertTrue(p == net_client.getProcedureManager().getRegisteredProcedure(info2));
    assertTrue(net_client.getHome().hasProcedure(info1));
    assertTrue(net_client.getHome().hasProcedure(info2));

    Node client_on_router = net_router.getNode(net_client.getHome().getId());
    assertTrue(client_on_router.getProcedures().size() > 0);
    assertTrue(client_on_router.hasProcedure(info1));
    assertTrue(client_on_router.hasProcedure(info2));

    CallableProcedure<String, String> p1 = info1.bind(net_router, func);
    CallableProcedure<String, String> p2 = info1.bind(net_router, String.class, String.class);
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
    net_client.getProcedureManager().registerProcedure("testRunnable", func);

    Procedure info1 = Procedure.of("testRunnable", Void.class, Void.class);
    Procedure info2 = Procedure.of("testRunnable", func);

    assertEquals(info1, info2);
    assertTrue(info1.equals(info2));
    assertTrue(info1.compareTo(info2) == 0);
    CallableRegisteredProcedure<?, ?> p = net_client.getProcedureManager().getRegisteredProcedure(info1);
    assertNotNull(p);
    assertTrue(p == net_client.getProcedureManager().getRegisteredProcedure(info2));
    assertTrue(net_client.getHome().hasProcedure(info1));
    assertTrue(net_client.getHome().hasProcedure(info2));

    Node client_on_router = net_router.getNode(net_client.getHome().getId());
    assertTrue(client_on_router.getProcedures().size() > 0);
    assertTrue(client_on_router.hasProcedure(info1));
    assertTrue(client_on_router.hasProcedure(info2));

    CallableProcedure<Void, Void> p1 = info1.bind(net_router, func);
    CallableProcedure<Void, Void> p2 = info1.bind(net_router, Void.class, Void.class);
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
    net_client.getProcedureManager().registerProcedure("testConsumer", func);

    Procedure info1 = Procedure.of("testConsumer", String.class, Void.class);
    Procedure info2 = Procedure.of("testConsumer", func);

    assertEquals(info1, info2);
    assertTrue(info1.equals(info2));
    assertTrue(info1.compareTo(info2) == 0);
    CallableRegisteredProcedure<?, ?> p = net_client.getProcedureManager().getRegisteredProcedure(info1);
    assertNotNull(p);
    assertTrue(p == net_client.getProcedureManager().getRegisteredProcedure(info2));
    assertTrue(net_client.getHome().hasProcedure(info1));
    assertTrue(net_client.getHome().hasProcedure(info2));

    Node client_on_router = net_router.getNode(net_client.getHome().getId());
    assertTrue(client_on_router.getProcedures().size() > 0);
    assertTrue(client_on_router.hasProcedure(info1));
    assertTrue(client_on_router.hasProcedure(info2));

    CallableProcedure<String, Void> p1 = info1.bind(net_router, func);
    CallableProcedure<String, Void> p2 = info1.bind(net_router, String.class, Void.class);
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
    net_client.getProcedureManager().registerProcedure("testSupplier", func);

    Procedure info1 = Procedure.of("testSupplier", Void.class, String.class);
    Procedure info2 = Procedure.of("testSupplier", func);

    assertEquals(info1, info2);
    assertTrue(info1.equals(info2));
    assertTrue(info1.compareTo(info2) == 0);
    CallableRegisteredProcedure<?, ?> p = net_client.getProcedureManager().getRegisteredProcedure(info1);
    assertNotNull(p);
    assertTrue(p == net_client.getProcedureManager().getRegisteredProcedure(info2));
    assertTrue(net_client.getHome().hasProcedure(info1));
    assertTrue(net_client.getHome().hasProcedure(info2));

    Node client_on_router = net_router.getNode(net_client.getHome().getId());
    assertTrue(client_on_router.getProcedures().size() > 0);
    assertTrue(client_on_router.hasProcedure(info1));
    assertTrue(client_on_router.hasProcedure(info2));

    CallableProcedure<Void, String> p1 = info1.bind(net_router, func);
    CallableProcedure<Void, String> p2 = info1.bind(net_router, Void.class, String.class);
    assertEquals(p1, p2);

    assertEquals(0, runCount.get());
    ProcedureCallResult<Void, String> call = p1.call(client_on_router, null);
    assertEquals(helloWorld, call.get(1, TimeUnit.MILLISECONDS));
    assertEquals(1, runCount.get());

  }

}
