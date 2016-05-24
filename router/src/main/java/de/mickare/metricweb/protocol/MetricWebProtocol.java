package de.mickare.metricweb.protocol;

import lombok.Data;

public class MetricWebProtocol extends WebProtocol {

  public MetricWebProtocol() {
    init();
  }

  @Override
  protected void init() {
    register(Subscribe.class);
  }
    
  // *************************

  @Packet(name = "subscribe")
  public static @Data class Subscribe implements PacketData {
    private String service;
  }

}
