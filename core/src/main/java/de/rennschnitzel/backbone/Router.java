package de.rennschnitzel.backbone;

import java.util.UUID;

import com.google.common.net.HostAndPort;

import de.rennschnitzel.backbone.net.node.NetworkNode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class Router {


  @Getter
  @NonNull
  private final HostAndPort address;

  @Getter
  @Setter(AccessLevel.PROTECTED)
  private NetworkNode node = null;

  /**
   * 
   * @param uuid
   * @param host {@code String} containing an IPv4 or IPv6 string literal, e.g.
   *        {@code "192.168.0.1"} or {@code "2001:db8::1"}sssssssssssssssssss
   * @param port on host
   * @throws IllegalArgumentException if the argument is not a valid IP string literal
   */
  public Router(String host, int port) throws IllegalArgumentException {
    this(HostAndPort.fromParts(host, port));
  }

  public UUID getUuid() {
    return this.node.getId();
  }

}
