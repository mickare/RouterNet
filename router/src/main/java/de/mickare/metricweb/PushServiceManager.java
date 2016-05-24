package de.mickare.metricweb;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;

import de.mickare.metricweb.event.OpenedWebConnectionEvent;
import de.mickare.metricweb.protocol.MetricWebProtocol;
import de.mickare.metricweb.websocket.WebConnection;

public class PushServiceManager {

  private final Map<String, PushService> services = Maps.newConcurrentMap();

  public synchronized void register(PushService service) {
    if (this.services.containsKey(service.getName().toLowerCase())) {
      throw new IllegalStateException("Service " + service.getName() + " already registered!");
    }
    this.services.put(service.getName().toLowerCase(), service);
  }

  public PushService getService(String name) {
    return services.get(name.toLowerCase());
  }

  private void handleSubscribeMessage(WebConnection connection, String packetName,
      MetricWebProtocol.Subscribe packetData) throws Exception {

    PushService service = getService(packetData.getService());
    if (service != null) {
      service.subscribe(connection);
    }

  }

  @Subscribe
  public void on(OpenedWebConnectionEvent event) {
    event.getConnection().registerMessageHandler(MetricWebProtocol.Subscribe.class,
        this::handleSubscribeMessage);
  }

}
