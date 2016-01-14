package de.rennschnitzel.backbone.net.channel.stream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

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
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ChannelRegister.Type;
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
      super(name, Type.STREAM);
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
      if (channel == null) {
        return null;
      }
      Preconditions.checkArgument(channel.getDescriptor() == this);
      return (StreamChannel) channel;
    }

  }

  private final OutputStream outputStream;

  private final Set<ChannelInputStream> inputBuffers = new CopyOnWriteArraySet<>();


  @Getter
  @Setter
  @NonNull
  private volatile Target target = Target.toAll();

  public StreamChannel(Owner owner, Channel parentChannel, Descriptor descriptor) throws IllegalStateException {
    super(owner, parentChannel, descriptor);
    this.outputStream = new BufferedOutputStream(new ChannelOutputStream(), this.descriptor.bufferSize);
  }


  public synchronized InputStream newInputBuffer() throws IOException {
    return newInputBuffer(CircularByteBuffer.INFINITE_SIZE);
  }

  public synchronized InputStream newInputBuffer(int bufferSize) throws IOException {
    checkChannel();
    ChannelInputStream in = new ChannelInputStream(bufferSize);
    inputBuffers.add(in);
    return in;
  }

  public OutputStream getOutputBuffer() throws IOException {
    checkChannel();
    return this.outputStream;
  }

  @Override
  public synchronized void close() {
    super.close();
    for (ChannelInputStream in : this.inputBuffers) {
      try {
        in.close();
      } catch (IOException e) {
      }
    }
    try {
      this.outputStream.close();
    } catch (IOException e) {
    }
  }

  @Override
  public void receive(ChannelMessage cmsg) throws IOException {
    this.receive(cmsg.getData().toByteArray());
  }

  private synchronized void receive(byte[] data) throws IOException {
    if (!isClosed()) {
      for (ChannelInputStream in : this.inputBuffers) {
        try {
          in.write(data);
        } catch (IOException e) {
          // just drop it
        }
      }
    }
  }

  private void send(byte[] data) throws IOException {
    this.parentChannel.send(this.target, data);

  }

  private void send(ByteString data) throws IOException {
    this.parentChannel.send(this.target, data);
  }

  private void checkChannel() throws IOException {
    if (isClosed()) {
      throw new IOException("Channel is closed");
    }
  }

  private class ChannelInputStream extends InputStream {
    private final CircularByteBuffer buffer;

    public ChannelInputStream(int bufferSize) {
      this.buffer = new CircularByteBuffer(bufferSize);
    }

    private void write(byte[] data) throws IOException {
      this.buffer.getOutputStream().write(data);
    }

    @Override
    public int available() throws IOException {
      return buffer.getInputStream().available();
    }

    @Override
    public synchronized void close() throws IOException {
      StreamChannel.this.inputBuffers.remove(this);
      buffer.getInputStream().close();
      buffer.getOutputStream().close();
    }

    @Override
    public void mark(int readAheadLimit) {
      buffer.getInputStream().mark(readAheadLimit);
    }

    @Override
    public boolean markSupported() {
      return buffer.getInputStream().markSupported();
    }

    @Override
    public int read() throws IOException {
      return buffer.getInputStream().read();
    }

    @Override
    public int read(byte[] cbuf) throws IOException {
      return buffer.getInputStream().read(cbuf);
    }

    @Override
    public int read(byte[] cbuf, int off, int len) throws IOException {
      return buffer.getInputStream().read(cbuf, off, len);
    }

    @Override
    public void reset() throws IOException {
      buffer.getInputStream().reset();
    }

    @Override
    public long skip(long n) throws IOException, IllegalArgumentException {
      return buffer.getInputStream().skip(n);
    }
  }

  private class ChannelOutputStream extends OutputStream {

    @Override
    public void write(byte[] b) throws IOException {
      checkChannel();
      send(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      checkChannel();
      ByteString data = ByteString.copyFrom(b, off, len);
      send(data);
    }

    @Override
    public void write(int b) throws IOException {
      checkChannel();
      ByteString data = ByteString.copyFrom(new byte[] {(byte) b});
      send(data);
    }
  }

}
