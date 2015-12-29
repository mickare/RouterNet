package de.rennschnitzel.backbone.net.store;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class BaseDataStore {

  private final ConcurrentMap<EntryKey, Entry> values = new ConcurrentHashMap<>();

  public ImmutableList<byte[]> get(EntryKey desc) {
    Entry e = getEntry(desc);
    if (e != null) {
      return e.getData();
    }
    return ImmutableList.of();
  }

  public byte[] get(EntryKey desc, int index) {
    Preconditions.checkArgument(index >= 0);
    Entry e = getEntry(desc);
    if (e != null) {
      return e.get(index);
    }
    return null;
  }


  private Entry getEntry(EntryKey desc) {
    Preconditions.checkNotNull(desc);
    return values.get(desc);
  }

  private Entry getOrCreate(EntryKey desc) {
    Preconditions.checkNotNull(desc);
    return values.computeIfAbsent(desc, Entry::new);
  }

  public void set(EntryKey desc, List<byte[]> data) {
    Preconditions.checkNotNull(data);
    if (data.isEmpty()) {
      clear(desc);
      return;
    }
    getOrCreate(desc).set(data);
  }

  public void add(EntryKey desc, List<byte[]> data) {
    Preconditions.checkNotNull(data);
    if (data.isEmpty()) {
      return;
    }
    getOrCreate(desc).data.addAll(data);
  }

  public void clear(EntryKey desc) {
    Preconditions.checkNotNull(desc);
    values.remove(desc);
  }

  public int remove(EntryKey desc, List<byte[]> data) {
    Preconditions.checkNotNull(data);
    if (data.isEmpty()) {
      return 0;
    }
    Entry e = getEntry(desc);
    if (e != null) {
      int result = e.remove(data);
      removeIfEmpty(e);
      return result;
    }
    return 0;
  }

  public byte[] remove(EntryKey desc, int index) {
    Preconditions.checkArgument(index >= 0);
    Entry e = getEntry(desc);
    if (e != null) {
      byte[] result =  e.remove(index);
      removeIfEmpty(e);
      return result;
    }
    return null;
  }

  public void push(EntryKey desc, List<byte[]> data) {
    Preconditions.checkNotNull(data);
    if (data.isEmpty()) {
      return;
    }
    getOrCreate(desc).data.addAll(0, data);
  }

  public List<byte[]> pop(EntryKey desc, int amount) {
    Preconditions.checkArgument(amount > 0);
    Entry e = getEntry(desc);
    if (e != null) {
      List<byte[]> result = e.pop(amount);
      removeIfEmpty(e);
      return result;
    }
    return ImmutableList.of();
  }

  private void removeIfEmpty(Entry e) {
    synchronized (e.data) {
      if (e.isEmpty()) {
        this.values.remove(e.descriptor, e);
      }
    }
  }

  public int size() {
    return this.values.size();
  }

  public boolean isEmpty() {
    return this.values.isEmpty();
  }

  @RequiredArgsConstructor
  public static class Entry {

    @Getter
    @NonNull
    private final EntryKey descriptor;
    @NonNull
    private final List<byte[]> data = Collections.synchronizedList(Lists.newArrayList());

    public void set(final List<byte[]> data) {
      synchronized (data) {
        this.data.clear();
        this.data.addAll(data);
      }
    }

    public boolean isEmpty() {
      return data.isEmpty();
    }

    public List<byte[]> pop(int amount) {
      ImmutableList.Builder<byte[]> b = ImmutableList.builder();
      synchronized (data) {
        for (int i = 0; i < amount && !data.isEmpty(); ++i) {
          b.add(this.data.remove(0));
        }
      }
      return b.build();
    }

    public byte[] get(int index) {
      synchronized (data) {
        if (0 <= index && index < data.size()) {
          return data.get(index);
        }
      }
      return null;
    }

    public byte[] remove(int index) {
      synchronized (data) {
        if (0 <= index && index < data.size()) {
          return data.remove(index);
        }
      }
      return null;
    }

    public int remove(final List<byte[]> data) {
      synchronized (data) {
        int count = 0;
        final ListIterator<byte[]> it = this.data.listIterator();
        while (it.hasNext()) {
          final byte[] current = it.next();
          for (byte[] rem : data) {
            if (Arrays.equals(current, rem)) {
              it.remove();
              count++;
              break;
            }
          }
        }
        return count;
      }
    }

    public ImmutableList<byte[]> getData() {
      synchronized (data) {
        return ImmutableList.copyOf(this.data);
      }
    }

  }

}
