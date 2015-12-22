package de.rennschnitzel.backbone.net.store;

import java.util.List;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;

import de.rennschnitzel.backbone.net.protocol.DataStoreProtocol.DataStoreResponseMessage;

public interface DataStore {

  ListenableFuture<List<ByteString>> get(EntryKey desc);

  ListenableFuture<ByteString> get(EntryKey desc, int index);

  ListenableFuture<Void> set(EntryKey desc, List<byte[]> data);

  ListenableFuture<Void> add(EntryKey desc, List<byte[]> data);

  ListenableFuture<Void> clear(EntryKey desc);

  ListenableFuture<Integer> remove(EntryKey desc, List<byte[]> data);

  ListenableFuture<ByteString> remove(EntryKey desc, int index);

  /**
   * Pushes an element onto the stack of the entry
   * 
   * @param desc - the entry descriptor
   * @param data - the data to push
   */
  ListenableFuture<Void> push(EntryKey desc, List<byte[]> data);

  /**
   * Pops an amount of element from the stack represented by this list. In other words, removes and
   * returns the first element of this list. This method is equivalent to remove(0).
   * 
   * @param desc - the entry descriptor
   * @param amount - the amount of elements
   * @return list of data at the front of the entry
   */
  ListenableFuture<List<ByteString>> pop(EntryKey desc, int amount);

  void handle(DataStoreResponseMessage msg);

}
