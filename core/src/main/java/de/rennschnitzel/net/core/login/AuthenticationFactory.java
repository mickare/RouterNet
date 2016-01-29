package de.rennschnitzel.net.core.login;

import java.security.MessageDigest;
import java.security.SecureRandom;

import com.google.common.base.Charsets;
import com.google.protobuf.ByteString;

import lombok.Getter;

public class AuthenticationFactory {

  private static final ThreadLocal<MessageDigest> perThreadMD5 = ThreadLocal.withInitial(() -> {
    try {
      return MessageDigest.getInstance("MD5");
    } catch (Exception e) {
      throw new RuntimeException("MD5 implementation not found", e);
    }
  });

  private static SecureRandom RANDOM = new SecureRandom();

  private static void xorRepeated(byte[] target, byte[] other) {
    int c = 0;
    for (int i = 0; i < target.length; ++i) {
      target[i] = (byte) (other[c] ^ target[i]);
      c = (c + 1) % other.length;
    }
  }

  public static ClientAuthentication newPasswordForClient(String password) {
    return new PasswordClientAuthentication(password);
  }

  public static RouterAuthentication newPasswordForRouter(String password) {
    return new PasswordRouterAuthentication(password);
  }

  private static final class PasswordClientAuthentication implements ClientAuthentication {

    private static final int DROP_BYTE_VALUE_MAX = 64;

    private final byte[] password;

    public PasswordClientAuthentication(String password) {
      this.password = perThreadMD5.get().digest(password.getBytes(Charsets.UTF_8));
    }

    @Override
    public ByteString calculateResponse(ByteString challenge) {

      byte[] ch = challenge.toByteArray();
      xorRepeated(ch, password);
      MessageDigest response = perThreadMD5.get();
      response.update(password);
      for (int i = 0; i < ch.length; ++i) {
        byte c = ch[i];
        if (c < DROP_BYTE_VALUE_MAX) {
          response.update(c);
        }
      }

      return ByteString.copyFrom(response.digest());
    }
  }

  private static final class PasswordRouterAuthentication implements RouterAuthentication {

    @Getter
    private final ByteString challenge;
    @Getter
    private final ByteString expectedResponse;

    public PasswordRouterAuthentication(String password) {
      byte[] challengeBuffer = new byte[128];
      RANDOM.nextBytes(challengeBuffer);
      this.challenge = ByteString.copyFrom(challengeBuffer);
      this.expectedResponse =
          new PasswordClientAuthentication(password).calculateResponse(this.challenge);
    }

    @Override
    public boolean checkResponse(ByteString response) {
      return this.expectedResponse.equals(response);
    }
  }

}
