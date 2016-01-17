package de.rennschnitzel.net.client;

import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class ClientSettings {

  @NonNull
  private NodeSettings node = new NodeSettings();

  @Data
  @NoArgsConstructor
  public static class NodeSettings {
    private UUID id = UUID.randomUUID();
    private Set<String> namespaces = Sets.newHashSet();
  }

  @NonNull
  private Connection connection = new Connection();

  @Data
  @NoArgsConstructor
  public static class Connection {
    private boolean testing = true;
    private String address = "localhost:1010";
    private String password = "Wkn2Z[uBYT]x1T/hY1Ac";
  }


}
