package de.rennschnitzel.net.client.connection;

import java.util.logging.Logger;

import com.google.common.util.concurrent.Service;

import de.rennschnitzel.net.core.Connection;
import io.netty.util.concurrent.Future;

public interface ConnectService extends Service {

  Logger getLogger();

  Future<Connection> getCurrentFuture();

}
