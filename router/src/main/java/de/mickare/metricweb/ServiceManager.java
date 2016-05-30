package de.mickare.metricweb;

import java.util.Map;
import java.util.logging.Level;

import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;

import de.mickare.metricweb.event.OpenedWebConnectionEvent;
import de.mickare.metricweb.protocol.RouterWebProtocol;
import de.mickare.metricweb.websocket.WebConnection;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public @RequiredArgsConstructor class ServiceManager {

  private @NonNull final MetricWebPlugin plugin;
  private final Map<String, Service> services = Maps.newConcurrentMap();

  public synchronized void register(Service service) {
    if (this.services.containsKey(service.getName().toLowerCase())) {
      throw new IllegalStateException("Service " + service.getName() + " already registered!");
    }
    this.services.put(service.getName().toLowerCase(), service);
  }

  public Service getService(String name) {
    return services.get(name.toLowerCase());
  }

  private void handleSubscribeMessage(WebConnection connection, String packetName,
      RouterWebProtocol.Subscribe packetData) throws Exception {

    Service service = getService(packetData.getService());
    if (service != null) {
      service.subscribe(connection);
    }

  }

  @Subscribe
  public void on(OpenedWebConnectionEvent event) {
    try {
      event.getConnection().registerMessageHandler(RouterWebProtocol.Subscribe.class,
          this::handleSubscribeMessage);
    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Could not register message handler!", e);
      throw e;
    }
  }

}
