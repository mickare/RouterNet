package de.rennschnitzel.backbone.net.store;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;

import de.rennschnitzel.backbone.util.function.CheckedSupplier;
import lombok.Getter;

public class LocalDataStore implements DataStore {

  @Getter
  private final BaseDataStore base;

  public LocalDataStore(BaseDataStore base) {
    Preconditions.checkNotNull(base);
    this.base = base;
  }

  private <R> ListenableFuture<R> call(CheckedSupplier<R> sup) {
    try {
      return Futures.immediateFuture(sup.get());
    } catch (Exception e) {
      return Futures.immediateFailedFuture(e);
    }
  }


  private final ByteString transform(final byte[] value) {
    return ByteString.copyFrom(value);
  }

  private final List<ByteString> transform(final List<byte[]> list) {
    return list.stream().map(this::transform).collect(Collectors.toList());
  }

  @Override
  public ListenableFuture<List<ByteString>> get(EntryKey desc) {
    return call(() -> transform(base.get(desc)));
  }

  @Override
  public ListenableFuture<ByteString> get(EntryKey desc, int index) {
    return call(() -> transform(base.get(desc, index)));
  }

  @Override
  public ListenableFuture<Void> set(EntryKey desc, List<byte[]> data) {
    return call(() -> {
      base.set(desc, data);
      return null;
    });
  }

  @Override
  public ListenableFuture<Void> add(EntryKey desc, List<byte[]> data) {
    return call(() -> {
      base.add(desc, data);
      return null;
    });
  }

  @Override
  public ListenableFuture<Void> clear(EntryKey desc) {
    return call(() -> {
      base.clear(desc);
      return null;
    });
  }

  @Override
  public ListenableFuture<Integer> remove(EntryKey desc, List<byte[]> data) {
    return call(() -> base.remove(desc, data));
  }

  @Override
  public ListenableFuture<ByteString> remove(EntryKey desc, int index) {
    return call(() -> transform(base.remove(desc, index)));
  }

  @Override
  public ListenableFuture<Void> push(EntryKey desc, List<byte[]> data) {
    return call(() -> {
      base.push(desc, data);
      return null;
    });
  }

  @Override
  public ListenableFuture<List<ByteString>> pop(EntryKey desc, int amount) {
    return call(() -> transform(base.pop(desc, amount)));
  }


}
