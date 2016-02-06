package de.rennschnitzel.net.core.tunnel;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executor;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.protocol.TransportProtocol.TunnelRegister.Type;
import lombok.Getter;

public abstract class AbstractSubTunnel<SELF extends AbstractSubTunnel<SELF, D>, D extends AbstractSubTunnelDescriptor<D, SELF>>
    implements TunnelHandler, SubTunnel {

  protected @Getter final Tunnel parentTunnel;
  protected @Getter final D descriptor;

  public AbstractSubTunnel(Tunnel parentTunnel, D descriptor) {
    Preconditions.checkNotNull(parentTunnel);
    Preconditions.checkNotNull(descriptor);
    this.parentTunnel = parentTunnel;
    this.descriptor = descriptor;
    this.parentTunnel.registerHandler(this);
  }


  @Override
  public boolean isClosed() {
    return this.parentTunnel.isClosed();
  }

  @Override
  public void close() {
    this.parentTunnel.close();
  }

  @Override
  public String getName() {
    return this.parentTunnel.getName();
  }

  @Override
  public int getId() {
    return this.parentTunnel.getId();
  }

  @Override
  public Type getType() {
    return this.descriptor.getType();
  }

  public abstract void receive(TunnelMessage cmsg) throws IOException;

  @Override
  public AbstractNetwork getNetwork() {
    return parentTunnel.getNetwork();
  }

  public void register(Connection connection, boolean flush) {
    parentTunnel.register(connection, flush);
  }


  public Optional<Executor> getExectutor() {
    return this.parentTunnel.getExecutor();
  }

  public void setExectutor(Executor executor) {
    this.parentTunnel.setExectutor(executor);
  }

}
