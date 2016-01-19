package de.rennschnitzel.net.client.testing;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ForwardingListenableFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.dummy.DummyConnection;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;

public class DummyConnector extends ForwardingListenableFuture<DummyConnection>
    implements TestingConnector<DummyConnection> {

  private final ListenableFuture<DummyConnection> delegate;
  private final DummyConnection con;
  private final Connection partner;

  public DummyConnector(DummyConnection con, Connection partner) {
    Preconditions.checkNotNull(con);
    Preconditions.checkNotNull(partner);
    Preconditions.checkArgument(con.getConnected() == partner);
    this.delegate = Futures.immediateFuture(con);
    this.con = con;
    this.partner = partner;
  }

  @Override
  public boolean isOpen() {
    return !this.isCancelled() && con.getConnected() == partner;
  }

  @Override
  protected ListenableFuture<DummyConnection> delegate() {
    return delegate;
  }

  @Override
  public void disconnect(CloseMessage msg) {
    con.disconnect(msg);
  }

}
