package de.rennschnitzel.backbone.api.network;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;

import lombok.Getter;

@Getter
public class MessageChannel {

  public static AtomicInteger ID_GENERATOR = new AtomicInteger(0);

  private final int id;

  private final String name;

  public MessageChannel(String name) {
    this(ID_GENERATOR.getAndIncrement(), name);
  }

  public MessageChannel(int id, String name) {
    Preconditions.checkArgument(!name.isEmpty());
    this.id = id;
    this.name = name.toLowerCase();
  }



}
