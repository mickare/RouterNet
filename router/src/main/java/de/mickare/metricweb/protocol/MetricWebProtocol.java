package de.mickare.metricweb.protocol;

import lombok.Data;

public class MetricWebProtocol extends WebProtocol {

  public MetricWebProtocol() {
    // TODO Auto-generated constructor stub
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
