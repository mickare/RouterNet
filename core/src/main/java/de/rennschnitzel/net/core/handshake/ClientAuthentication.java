package de.rennschnitzel.net.core.handshake;

import com.google.protobuf.ByteString;

public interface ClientAuthentication {
  public ByteString calculateResponse(ByteString challenge);
}