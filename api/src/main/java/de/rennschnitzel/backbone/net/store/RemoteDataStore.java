package de.rennschnitzel.backbone.net.store;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;

import de.rennschnitzel.backbone.net.Connection;
import de.rennschnitzel.backbone.net.protocol.DataStoreProtocol.DataStoreRequestMessage;
import de.rennschnitzel.backbone.net.protocol.DataStoreProtocol.DataStoreRequestType;
import de.rennschnitzel.backbone.net.protocol.DataStoreProtocol.DataStoreResponseMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Packet;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class RemoteDataStore implements DataStore {

  public static final int MAX_TIMEOUT = 10; // in seconds

  private static final AtomicInteger REQUEST_ID_GENERATOR = new AtomicInteger(0);

  private final Connection connection;

  private final Cache<Integer, Request<?>> requests =
      CacheBuilder.newBuilder().expireAfterWrite(MAX_TIMEOUT, TimeUnit.SECONDS).removalListener(new RemovalListener<Integer, Request<?>>() {
        @Override
        public void onRemoval(RemovalNotification<Integer, Request<?>> notify) {
          if (!notify.getValue().isDone()) {
            notify.getValue().failed(new TimeoutException());
          }
        }
      }).build();

  public RemoteDataStore(Connection connection) {
    Preconditions.checkNotNull(connection);
    this.connection = connection;
  }

  public void cleanUpRequests() {
    this.requests.cleanUp();
  }

  private <T> Request<T> newRequestBytes(EntryKey desc, DataStoreRequestType type, List<byte[]> requestData,
      final Function<List<ByteString>, T> func) {
    ImmutableList.Builder<ByteString> b = ImmutableList.builder();
    for (int i = 0; i < requestData.size(); ++i) {
      b.add(ByteString.copyFrom(requestData.get(i)));
    }
    return newRequest(desc, type, b.build(), func);
  }

  private <T> Request<T> newRequest(EntryKey desc, DataStoreRequestType type, ImmutableList<ByteString> requestData,
      final Function<List<ByteString>, T> func) {
    return new Request<T>(desc, type, requestData) {
      @Override
      protected T readData(List<ByteString> data) throws Exception {
        return func.apply(data);
      }
    };
  }

  @RequiredArgsConstructor
  private abstract class Request<T> extends AbstractFuture<T> {
    @Getter
    private final int id = REQUEST_ID_GENERATOR.incrementAndGet();
    @NonNull
    @Getter
    private final EntryKey key;
    @NonNull
    @Getter
    private final DataStoreRequestType type;
    @Getter
    private final ImmutableList<ByteString> requestData;

    public String toString() {
      return key.toString() + " " + type.toString();
    }

    private boolean failed(Throwable throwable) {
      return this.setException(throwable);
    }

    private void handle(DataStoreResponseMessage response) {
      Preconditions.checkArgument(this.id == response.getId());
      requests.invalidate(this.id);
      if (this.isDone()) {
        return;
      }
      try {
        if (!response.getSuccess()) {
          this.setException(new RuntimeException("DataStore operation failed (" + this.toString() + ")"));
          return;
        }
        this.set(this.readData(response.getDataList()));
      } catch (Exception e) {
        this.setException(e);
      }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      if (super.cancel(mayInterruptIfRunning)) {
        requests.invalidate(this.id);
        return true;
      }
      return false;
    }

    protected abstract T readData(List<ByteString> data) throws Exception;

    private DataStoreRequestMessage toProtocol() {
      DataStoreRequestMessage.Builder b = DataStoreRequestMessage.newBuilder();
      b.setId(id);
      b.setKey(key.toProtocol());
      b.setType(this.type);
      b.addAllData(this.requestData);
      return b.build();
    }

  }

  public void handle(DataStoreResponseMessage response) {
    Request<?> req = requests.getIfPresent(response.getId());
    if (req != null) {
      req.handle(response);
    }
  }

  private void send(Request<?> req) {
    if (connection.isClosed()) {
      req.failed(new RuntimeException("Connection closed"));
      return;
    }
    try {
      this.requests.put(req.getId(), req);
      this.connection.send(Packet.newBuilder().setDataStoreRequest(req.toProtocol()));
    } catch (Exception e) {
      this.requests.invalidate(req.getId());
      req.failed(e);
    }
  }

  @Override
  public ListenableFuture<List<ByteString>> get(EntryKey desc) {
    Request<List<ByteString>> req = newRequest(desc, DataStoreRequestType.GET, null, (d) -> d);
    send(req);
    return req;
  }

  @Override
  public ListenableFuture<Void> set(EntryKey desc, List<byte[]> data) {
    Request<Void> req = newRequestBytes(desc, DataStoreRequestType.SET, data, (d) -> null);
    send(req);
    return req;
  }

  @Override
  public ListenableFuture<Void> add(EntryKey desc, List<byte[]> data) {
    Request<Void> req = newRequestBytes(desc, DataStoreRequestType.ADD, data, (d) -> null);
    send(req);
    return req;
  }

  @Override
  public ListenableFuture<Void> clear(EntryKey desc) {
    Request<Void> req = newRequest(desc, DataStoreRequestType.CLEAR, ImmutableList.of(), (d) -> null);
    send(req);
    return req;
  }

  @Override
  public ListenableFuture<Integer> remove(EntryKey desc, List<byte[]> data) {
    Request<Integer> req = newRequestBytes(desc, DataStoreRequestType.REMOVE, data, (d) -> {
      if (d.size() > 0) {
        return d.get(0).asReadOnlyByteBuffer().getInt();
      }
      return 0;
    });
    send(req);
    return req;
  }

  @Override
  public ListenableFuture<ByteString> remove(EntryKey desc, int index) {
    ByteString bs = ByteString.copyFrom(ByteBuffer.allocate(4).putInt(index));
    Request<ByteString> req =
        newRequest(desc, DataStoreRequestType.REMOVE_INDEX, ImmutableList.of(bs), (d) -> d.isEmpty() ? null : d.get(0));
    send(req);
    return req;
  }

  @Override
  public ListenableFuture<Void> push(EntryKey desc, List<byte[]> data) {
    Request<Void> req = newRequestBytes(desc, DataStoreRequestType.PUSH, data, (d) -> null);
    send(req);
    return req;
  }

  @Override
  public ListenableFuture<List<ByteString>> pop(EntryKey desc, int amount) {
    ByteString bs = ByteString.copyFrom(ByteBuffer.allocate(4).putInt(amount));
    Preconditions.checkArgument(amount > 0);
    Request<List<ByteString>> req = newRequest(desc, DataStoreRequestType.GET_INDEX, ImmutableList.of(bs), (d) -> d);
    send(req);
    return req;
  }


  @Override
  public ListenableFuture<ByteString> get(EntryKey desc, int index) {
    ByteString bs = ByteString.copyFrom(ByteBuffer.allocate(4).putInt(index));
    Request<ByteString> req = newRequest(desc, DataStoreRequestType.GET_INDEX, ImmutableList.of(bs), (d) -> d.isEmpty() ? null : d.get(0));
    send(req);
    return req;
  }

}
