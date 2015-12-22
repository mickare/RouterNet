package de.rennschnitzel.backbone.net.store;

import java.util.Objects;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.net.protocol.DataStoreProtocol;
import de.rennschnitzel.backbone.net.protocol.DataStoreProtocol.DataStoreEntryKeyMessage;
import lombok.Getter;

public class EntryKey {

  @Getter
  private final String name;

  public EntryKey(String name) {
    Preconditions.checkArgument(!name.isEmpty());
    this.name = name.toLowerCase();
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof EntryKey)) {
      return false;
    }
    EntryKey o = (EntryKey) obj;
    return this.name.equals(o.name);
  }

  @Override
  public String toString() {
    return "Entry(" + name + ")";
  }

  public DataStoreEntryKeyMessage toProtocol() {
    return DataStoreEntryKeyMessage.newBuilder().setName(name).build();
  }

  public static EntryKey from(DataStoreProtocol.DataStoreEntryKeyMessage msg) {
    return new EntryKey(msg.getName());
  }

}
