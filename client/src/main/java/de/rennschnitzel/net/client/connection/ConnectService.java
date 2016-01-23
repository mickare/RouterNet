package de.rennschnitzel.net.client.connection;

import java.util.UUID;
import java.util.logging.Logger;

import com.google.common.util.concurrent.Service;

import de.rennschnitzel.net.core.Connection;
import io.netty.util.concurrent.Future;

public interface ConnectService<L> extends Service {

  Future<Connection> getConnectionPromise();

  L getLoginHandler();

  String getConnectedName();

  UUID getConnectedId();

  Logger getLogger();

}
