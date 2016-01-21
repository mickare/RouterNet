package de.rennschnitzel.net.core.tunnel;

import java.io.IOException;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.protocol.TransportProtocol.TunnelRegister.Type;
import lombok.Getter;

public abstract class AbstractSubTunnel<SELF extends AbstractSubTunnel<SELF, D>, D extends AbstractSubTunnelDescriptor<D, SELF>>
    implements TunnelHandler, SubTunnel {

  @Getter
  protected final Tunnel parentTunnel;
  @Getter
  protected final D descriptor;

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
  public Type getType() {
    return this.descriptor.getType();
  }

  public abstract void receive(TunnelMessage cmsg) throws IOException;

  @Override
  public AbstractNetwork getNetwork() {
    return parentTunnel.getNetwork();
  }

}