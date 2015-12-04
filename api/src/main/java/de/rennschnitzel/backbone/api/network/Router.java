package de.rennschnitzel.backbone.api.network;

import java.util.Map;
import java.util.UUID;

public interface Router extends Server {

  public Client getClient(UUID uuid);

  public Map<UUID, Client> getClients();

}
