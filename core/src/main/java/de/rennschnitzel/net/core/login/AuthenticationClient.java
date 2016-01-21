package de.rennschnitzel.net.core.login;

import com.google.protobuf.ByteString;

public interface AuthenticationClient {
  public ByteString calculateResponse(ByteString challenge);
}