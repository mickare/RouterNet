package de.mickare.metricweb.protocol;

import lombok.Data;

public class RouterWebProtocol extends WebProtocol {

  public RouterWebProtocol() {
    init();
  }

  @Override
  protected void init() {
    register(Subscribe.class);
    register(Unsubscribe.class);
  }
    
  // *************************

  @Packet(name = "subscribe")
  public static @Data class Subscribe implements PacketData {
    private String service;
  }

  @Packet(name = "unsubscribe")
  public static @Data class Unsubscribe implements PacketData {
    private String service;
  }
  
}
