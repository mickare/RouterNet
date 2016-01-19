package de.rennschnitzel.net;

import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.rennschnitzel.net.util.SimpleOwner;
import de.rennschnitzel.net.util.concurrent.DirectScheduledExecutorService;

public class TestNetty {

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
    
  }
  
}
