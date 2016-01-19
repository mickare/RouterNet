package de.rennschnitzel.net.netty.handshake;

import com.google.protobuf.ByteString;

public interface RouterAuthentication {
  public ByteString getChallenge();

  public boolean checkResponse(ByteString response);
}
