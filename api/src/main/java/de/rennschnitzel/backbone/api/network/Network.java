package de.rennschnitzel.backbone.api.network;

import org.nustaq.serialization.FSTConfiguration;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public abstract class Network implements NetworkInterface {

  public static FSTConfiguration FST = FSTConfiguration.createDefaultConfiguration();

  @Getter
  @Setter(AccessLevel.PROTECTED)
  private static Network instance = null;


}
