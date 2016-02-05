package de.rennschnitzel.net.client;

import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;

import de.rennschnitzel.net.client.connection.ConnectFailHandler;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class ClientSettings {

  @NonNull
  private NodeSettings home = new NodeSettings();

  @Data
  @NoArgsConstructor
  public static class NodeSettings {
    private UUID id = UUID.randomUUID();
    private Set<String> namespaces = Sets.newHashSet();
    private String name = null;
  }

  @NonNull
  private Connection connection = new Connection();

  @Data
  @NoArgsConstructor
  public static class Connection {
    private boolean testingMode = true;
    private String address = "localhost:1010";
    private String password = "Wkn2Z[uBYT]x1T/hY1Ac";

    private ConnectFailHandler failHandler = ConnectFailHandler.SHUTDOWN;


  }

}
