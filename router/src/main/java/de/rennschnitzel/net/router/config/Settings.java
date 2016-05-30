package de.rennschnitzel.net.router.config;

import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class Settings {

  public static @Data @NoArgsConstructor class RouterConfig {
    private NodeSettings home = new NodeSettings();
    private String address = "localhost:1010";
    private String password = "Wkn2Z[uBYT]x1T/hY1Ac";
  }

  public static @Data @NoArgsConstructor class NodeSettings {
    private UUID id = UUID.randomUUID();
    private Set<String> namespaces = Sets.newHashSet();
    private String name = null;
  }

  private RouterConfig routerSettings = new RouterConfig();
  // private DatabaseConfig databaseSettings = new DatabaseConfig();
  
  
  private String restartScript = "start.sh";
  private String pluginFolder = "plugins";
  
}
