package de.rennschnitzel.net.netty.handshake;

import com.google.protobuf.ByteString;

public interface ClientAuthentication {
  public ByteString calculateResponse(ByteString challenge);
}