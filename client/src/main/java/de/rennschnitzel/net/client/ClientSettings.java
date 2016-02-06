package de.rennschnitzel.net.client;

import de.rennschnitzel.net.client.connection.ConnectFailHandler;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;



public @Data @NoArgsConstructor class ClientSettings {

  private @NonNull Connection connection = new Connection();

  public static @Data @NoArgsConstructor class Connection {
    private boolean testingMode = true;
    private String address = "localhost:1010";
    private String password = "Wkn2Z[uBYT]x1T/hY1Ac";

    private ConnectFailHandler failHandler = ConnectFailHandler.SHUTDOWN;


  }

}
