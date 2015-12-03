package de.rennschnitzel.backbone.api;

import java.util.UUID;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RouterInfo {

  @Getter
  @NonNull
  private final UUID id;

  @Getter
  @NonNull
  private final String host;
  @Getter
  private final int port;

  @Getter
  @NonNull
  private final String name;

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof RouterInfo)) {
      return false;
    }
    final RouterInfo i = (RouterInfo) o;
    return i.id.equals(this.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public String toString() {
    return "Router(" + id.toString() + "," + host + ":" + port + ")";
  }

}
