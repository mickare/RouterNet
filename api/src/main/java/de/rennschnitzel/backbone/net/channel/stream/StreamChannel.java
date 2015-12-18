package de.rennschnitzel.backbone.net.channel.stream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import com.Ostermiller.util.CircularByteBuffer;
import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import de.rennschnitzel.backbone.Owner;
import de.rennschnitzel.backbone.net.Target;
import de.rennschnitzel.backbone.net.channel.AbstractSubChannel;
import de.rennschnitzel.backbone.net.channel.AbstractSubChannelDescriptor;
import de.rennschnitzel.backbone.net.channel.Channel;
import de.rennschnitzel.backbone.net.channel.ChannelHandler;
import de.rennschnitzel.backbone.net.channel.ChannelMessage;
import de.rennschnitzel.backbone.net.channel.SubChannel;
import de.rennschnitzel.backbone.net.channel.SubChannelDescriptor;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class StreamChannel extends AbstractSubChannel<StreamChannel, StreamChannel.Descriptor>implements ChannelHandler, SubChannel {

  public static class Descriptor extends AbstractSubChannelDescriptor<Descriptor, StreamChannel>
      implements SubChannelDescriptor<StreamChannel> {

    @Getter
    private final int bufferSize;

    public Descriptor(String name) {
      this(name, 1 * (1 << 20)); // 1 MiB Buffer
    }

    public Descriptor(String name, int bufferSize) {
      super(name, TransportProtocol.ChannelRegister.Type.STREAM);
      Preconditions.checkNotNull(bufferSize > 32);
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
    public int hashCode() {
      return Objects.hash(name, type);
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


  private final CircularByteBuffer inputBuffer;
  @Getter
  private final OutputStream outputStream;

  @Getter
  @Setter()
  @NonNull
  private volatile Target target = Target.toAll();

  public StreamChannel(Owner owner, Channel parentChannel, Descriptor descriptor) throws IllegalStateException {
    super(owner, parentChannel, descriptor);
    this.inputBuffer = new CircularByteBuffer(CircularByteBuffer.INFINITE_SIZE);
    this.outputStream = new BufferedOutputStream(new ChannelOutput(), this.descriptor.bufferSize);
  }

  public InputStream getInputStream() {
    return this.inputBuffer.getInputStream();
  }

  @Override
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

}
