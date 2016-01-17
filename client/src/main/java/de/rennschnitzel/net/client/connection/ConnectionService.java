package de.rennschnitzel.net.client.connection;

import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.Service;

import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.exception.NotConnectedException;

public interface ConnectionService extends Service {

  Connection getConnection(long time, TimeUnit unit) throws NotConnectedException;

}
