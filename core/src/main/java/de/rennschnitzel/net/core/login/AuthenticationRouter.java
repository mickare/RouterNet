package de.rennschnitzel.net.core.login;

import com.google.protobuf.ByteString;

public interface AuthenticationRouter {
  public ByteString getChallenge();

  public boolean checkResponse(ByteString response);
}
