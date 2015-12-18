package de.rennschnitzel.backbone.net.channel;

import java.io.IOException;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.Owner;
import de.rennschnitzel.backbone.net.node.HomeNode;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ChannelRegister.Type;
import lombok.Getter;

public abstract class AbstractSubChannel<SELF extends AbstractSubChannel<SELF, D>, D extends AbstractSubChannelDescriptor<D, SELF>>
    implements ChannelHandler, SubChannel {

  @Getter
  protected final Owner owner;
  @Getter
  protected final Channel parentChannel;
  @Getter
  protected final D descriptor;

  public AbstractSubChannel(Owner owner, Channel parentChannel, D descriptor) {
    Preconditions.checkNotNull(owner);
    Preconditions.checkNotNull(parentChannel);
    Preconditions.checkNotNull(descriptor);
    this.owner = owner;
    this.parentChannel = parentChannel;
    this.descriptor = descriptor;
    this.parentChannel.registerHandler(this);
  }

  @Override
  public boolean isClosed() {
    return this.parentChannel.isClosed();
  }

  @Override
  public void close() {
    this.parentChannel.close();
  }

  @Override
  public int getChannelId() {
    return this.parentChannel.getChannelId();
  }

  @Override
  public String getName() {
    return this.parentChannel.getName();
  }

  @Override
  public HomeNode getHome() {
    return this.parentChannel.getHome();
  }

  @Override
  public Type getType() {
    return this.descriptor.getType();
  }

  public abstract void receive(ChannelMessage cmsg) throws IOException;


}
