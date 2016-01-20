package de.rennschnitzel.net.core.handshake;

import com.google.protobuf.ByteString;

public interface RouterAuthentication {
  public ByteString getChallenge();

  public boolean checkResponse(ByteString response);
}
