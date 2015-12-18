package de.rennschnitzel.backbone.net.channel.stream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.Ostermiller.util.CircularByteBuffer;
import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import de.rennschnitzel.backbone.Owner;
import de.rennschnitzel.backbone.net.NetworkMember;
import de.rennschnitzel.backbone.net.Target;
import de.rennschnitzel.backbone.net.channel.Channel;
import de.rennschnitzel.backbone.net.channel.ChannelHandler;
import de.rennschnitzel.backbone.net.channel.ChannelMessage;
import de.rennschnitzel.backbone.net.channel.SubChannel;
import de.rennschnitzel.backbone.net.channel.SubChannelDescriptor;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ChannelRegister.Type;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class StreamChannel implements ChannelHandler<Channel, ChannelMessage>, SubChannel {

  public static class Descriptor implements SubChannelDescriptor<StreamChannel> {
    @Getter
    private final String name;
    @Getter
    private final int bufferSize;

    @Getter
    private final TransportProtocol.ChannelRegister.Type type = TransportProtocol.ChannelRegister.Type.STREAM;

    public Descriptor(String name) {
      this(name, 1 * (1 << 20)); // 1 MiB Buffer
    }

    public Descriptor(String name, int bufferSize) {
      Preconditions.checkArgument(!name.isEmpty());
      Preconditions.checkNotNull(bufferSize > 32);
      this.name = name.toLowerCase();
      this.bufferSize = bufferSize;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Descriptor)) {
        return false;
      }
      Descriptor d = (Descriptor) o;
      return this.name.equals(d.name) && this.bufferSize == d.bufferSize;
    }

    @Override
    public StreamChannel create(Owner owner, Channel parentChannel) {
      return new StreamChannel(owner, parentChannel, this);
    }

    @Override
    public StreamChannel cast(SubChannel channel) {
      Preconditions.checkArgument(channel.getDescriptor() == this);
      return (StreamChannel) channel;
    }

  }

  @Getter
  private final Owner owner;
  @Getter
  private final Channel parentChannel;
  @Getter
  private final Descriptor descriptor;

  private final CircularByteBuffer inputBuffer;

  @Getter
  private final OutputStream outputStream;

  @Getter
  @Setter()
  @NonNull
  private volatile Target target = Target.toAll();

  public StreamChannel(Owner owner, Channel parentChannel, Descriptor descriptor) throws IllegalStateException {
    Preconditions.checkNotNull(owner);
    Preconditions.checkNotNull(parentChannel);
    Preconditions.checkNotNull(descriptor);
    this.owner = owner;
    this.parentChannel = parentChannel;
    this.descriptor = descriptor;
    this.parentChannel.registerHandler(this);

    this.inputBuffer = new CircularByteBuffer(CircularByteBuffer.INFINITE_SIZE);
    this.outputStream = new BufferedOutputStream(new ChannelOutput(), this.descriptor.bufferSize);
  }

  public InputStream getInputStream() {
    return this.inputBuffer.getInputStream();
  }

  public boolean isClosed() {
    return this.parentChannel.isClosed();
  }

  public void close() {
    this.parentChannel.close();
  }

  public int getChannelId() {
    return this.parentChannel.getChannelId();
  }

  public String getName() {
    return this.parentChannel.getName();
  }

  public NetworkMember getHome() {
    return this.parentChannel.getHome();
  }

  public synchronized void receive(ChannelMessage cmsg) throws IOException {
    if (!isClosed()) {
      this.inputBuffer.getOutputStream().write(cmsg.getByteData().toByteArray());
    }
  }

  private void send(byte[] data) throws IOException {
    this.parentChannel.send(this.target, data);
  }

  private void send(ByteString data) throws IOException {
    this.parentChannel.send(this.target, data);
  }

  private class ChannelOutput extends OutputStream {

    @Override
    public void close() throws IOException {
      StreamChannel.this.close();
    }

    private void check() throws IOException {
      if (isClosed()) {
        throw new IOException("Channel is closed");
      }
    }

    @Override
    public synchronized void write(byte[] b) throws IOException {
      check();
      send(b);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
      check();
      send(ByteString.copyFrom(b, off, len));
    }

    @Override
    public synchronized void write(int b) throws IOException {
      check();
      send(ByteString.copyFrom(new byte[] {(byte) b}));
    }
  }

  @Override
  public Type getType() {
    return this.descriptor.getType();
  }

}
