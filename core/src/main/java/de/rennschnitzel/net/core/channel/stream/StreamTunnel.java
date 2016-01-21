package de.rennschnitzel.net.core.channel.stream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.Ostermiller.util.CircularByteBuffer;
import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import de.rennschnitzel.net.core.Target;
import de.rennschnitzel.net.core.Tunnel;
import de.rennschnitzel.net.core.tunnel.AbstractSubTunnel;
import de.rennschnitzel.net.core.tunnel.AbstractSubTunnelDescriptor;
import de.rennschnitzel.net.core.tunnel.SubChannelDescriptor;
import de.rennschnitzel.net.core.tunnel.SubTunnel;
import de.rennschnitzel.net.core.tunnel.TunnelHandler;
import de.rennschnitzel.net.core.tunnel.TunnelMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.TunnelRegister;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class StreamTunnel extends AbstractSubTunnel<StreamTunnel, StreamTunnel.Descriptor>implements TunnelHandler, SubTunnel {

  public static class Descriptor extends AbstractSubTunnelDescriptor<Descriptor, StreamTunnel>
      implements SubChannelDescriptor<StreamTunnel> {

    public Descriptor(String name) {
      super(name, TunnelRegister.Type.STREAM);
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
      return this.name.equals(d.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, type);
    }

    @Override
    public StreamTunnel create(Tunnel parentChannel) {
      return new StreamTunnel(parentChannel, this);
    }

    @Override
    public StreamTunnel cast(SubTunnel channel) {
      if (channel == null) {
        return null;
      }
      Preconditions.checkArgument(channel.getDescriptor() == this);
      return (StreamTunnel) channel;
    }

  }

  private final Semaphore outputStreamSemaphore = new Semaphore(1);
  private volatile ChannelOutputStream outputStream = null;

  private final Set<ChannelInputStream> inputBuffers = new CopyOnWriteArraySet<>();

  public StreamTunnel(Tunnel parentChannel, Descriptor descriptor) throws IllegalStateException {
    super(parentChannel, descriptor);
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


  public OutputStream newOutputBuffer(Target target) throws IOException {
    return newOutputBuffer(target, 512);
  }

  public OutputStream newOutputBuffer(Target target, int bufferSize) throws IOException {
    Preconditions.checkArgument(bufferSize > 0);
    Preconditions.checkArgument(!target.isEmpty());
    checkChannel();
    try {
      if (!outputStreamSemaphore.tryAcquire(3, TimeUnit.SECONDS)) {
        if (this.outputStream != null) {
          this.getNetwork().getLogger().log(Level.WARNING, "Resource is blocked by a not-closed OutputStream!",
              this.outputStream.getThrowable());
        }
        outputStreamSemaphore.acquire();
      }
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
    try {
      checkChannel();
      return new ChannelOutputStream(target, bufferSize);
    } catch (Exception e) {
      outputStreamSemaphore.release();
      throw e;
    }
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
    if (outputStream != null) {
      try {
        outputStream.close();
      } catch (IOException e) {
      }
    }
  }

  @Override
  public void receive(TunnelMessage cmsg) throws IOException {
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
      checkChannel();
      return buffer.getInputStream().available();
    }

    @Override
    public synchronized void close() throws IOException {
      StreamTunnel.this.inputBuffers.remove(this);
      buffer.getOutputStream().close();
      buffer.getInputStream().close();
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
      checkChannel();
      return buffer.getInputStream().read();
    }

    @Override
    public int read(byte[] cbuf) throws IOException {
      checkChannel();
      return buffer.getInputStream().read(cbuf);
    }

    @Override
    public int read(byte[] cbuf, int off, int len) throws IOException {
      checkChannel();
      return buffer.getInputStream().read(cbuf, off, len);
    }

    @Override
    public void reset() throws IOException {
      checkChannel();
      buffer.getInputStream().reset();
    }

    @Override
    public long skip(long n) throws IOException, IllegalArgumentException {
      checkChannel();
      return buffer.getInputStream().skip(n);
    }
  }

  private class ChannelOutputStream extends OutputStream {

    private final BufferedOutputStream out;
    @Getter
    private final Throwable throwable;

    public ChannelOutputStream(Target target, int size) {
      Preconditions.checkNotNull(target);
      Preconditions.checkArgument(size > 0);
      this.out = new BufferedOutputStream(new SenderOutputStream(target), size);
      throwable = new Throwable(); // costly
    }

    @Override
    public void close() throws IOException {
      out.close();
    }

    @Override
    public void flush() throws IOException {
      out.flush();
    }

    @Override
    public void write(byte[] b) throws IOException {
      out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      out.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
      out.write(b);
    }


    @RequiredArgsConstructor
    private class SenderOutputStream extends OutputStream {

      @NonNull
      private final Target target;

      private boolean closed = false;

      @Override
      public synchronized void close() throws IOException {
        boolean old = closed;
        closed = true;
        if (!old) {
          outputStreamSemaphore.release();
        }
      }

      @Override
      public void write(byte[] b) throws IOException {
        checkChannel();
        StreamTunnel.this.parentTunnel.send(this.target, b);
      }

      @Override
      public void write(byte[] b, int off, int len) throws IOException {
        checkChannel();
        ByteString data = ByteString.copyFrom(b, off, len);
        StreamTunnel.this.parentTunnel.send(this.target, data);
      }

      @Override
      public void write(int b) throws IOException {
        checkChannel();
        ByteString data = ByteString.copyFrom(new byte[] {(byte) b});
        StreamTunnel.this.parentTunnel.send(this.target, data);
      }
    }

  }


}
