package de.rennschnitzel.net;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.rennschnitzel.net.util.SimpleOwner;
import de.rennschnitzel.net.util.concurrent.DirectScheduledExecutorService;

public class TestNetClient {

  private Owner owner;
  private NetClient client;

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    owner = new SimpleOwner("TestNetClient", Logger.getLogger("TestNetClient"));
    client = new NetClient();

    folder.create();
    client.init(Logger.getLogger("TestNetClient"), folder.newFolder("net"),
        new DirectScheduledExecutorService());

    client.enable();
  }

  @After
  public void tearDown() throws Exception {
    client.disable();
    folder.delete();
  }


  @Test
  public void testChannel() throws Exception {

    assertNotNull(Net.getNetwork());
    assertNotNull(Net.getConnection());

    final AtomicInteger received = new AtomicInteger(0);

    client.getTest().getNetwork().getConnection().getTunnel("testChannel")
        .registerMessageListener(owner, msg -> {
          received.addAndGet(msg.getData().byteAt(0));
        });

    Net.getChannel("testChannel").broadcast(new byte[] {1});
    assertEquals(received.get(), 1);

    Net.getChannel("testChannel").broadcast(new byte[] {2});
    assertEquals(received.get(), 3);

  }


}
