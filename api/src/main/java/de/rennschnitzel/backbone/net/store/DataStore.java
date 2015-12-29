package de.rennschnitzel.backbone.net.store;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;

public interface DataStore {

  ListenableFuture<List<ByteString>> get(EntryKey desc);

  ListenableFuture<ByteString> get(EntryKey desc, int index);

  default ListenableFuture<Void> set(EntryKey desc, byte[] data) {
    return set(desc, ImmutableList.of(data));
  }

  ListenableFuture<Void> set(EntryKey desc, List<byte[]> data);

  default ListenableFuture<Void> add(EntryKey desc, byte[] data) {
    return add(desc, ImmutableList.of(data));
  }

  ListenableFuture<Void> add(EntryKey desc, List<byte[]> data);

  ListenableFuture<Void> clear(EntryKey desc);

  default ListenableFuture<Integer> remove(EntryKey desc, byte[] data) {
    return remove(desc, ImmutableList.of(data));
  }

  ListenableFuture<Integer> remove(EntryKey desc, List<byte[]> data);

  ListenableFuture<ByteString> remove(EntryKey desc, int index);

  default ListenableFuture<Void> push(EntryKey desc, byte[] data) {
    return push(desc, ImmutableList.of(data));
  }

  /**
   * Pushes an element onto the stack of the entry
   * 
   * @param desc - the entry descriptor
   * @param data - the data to push
   */
  ListenableFuture<Void> push(EntryKey desc, List<byte[]> data);


  default ListenableFuture<Optional<ByteString>> pop(EntryKey desc) {
    return Futures.transform(pop(desc, 1), DataStoreUtils.TRANSFORM_POP_TO_SINGLE);
  }

  /**
   * Pops an amount of element from the stack represented by this list. In other words, removes and
   * returns the first element of this list. This method is equivalent to remove(0).
   * 
   * @param desc - the entry descriptor
   * @param amount - the amount of elements
   * @return list of data at the front of the entry
   */
  ListenableFuture<List<ByteString>> pop(EntryKey desc, int amount);

}
