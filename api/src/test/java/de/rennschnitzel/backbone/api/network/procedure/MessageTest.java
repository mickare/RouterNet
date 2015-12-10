package de.rennschnitzel.backbone.api.network.procedure;

import java.util.UUID;

import org.junit.Test;

import de.rennschnitzel.backbone.api.network.Network;

public class MessageTest {

  @Test
  public void testRemoteProcedureCall() {
    
    UUID serverId = UUID.randomUUID();
    
    Network.getInstance().getServers().getServer(serverId);
    
    
  }
  
  
  
}
