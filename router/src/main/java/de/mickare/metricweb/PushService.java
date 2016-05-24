package de.mickare.metricweb;

import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import de.mickare.metricweb.protocol.WebProtocol.PacketData;
import de.mickare.metricweb.protocol.WebProtocol.PacketMessage;
import de.mickare.metricweb.websocket.WebConnection;
import lombok.Getter;

public abstract class PushService {

  private @Getter final String name;
  private final Set<WebConnection> subscribed = Sets.newConcurrentHashSet();

  public PushService(String name) {
    Preconditions.checkArgument(!name.isEmpty());
    this.name = name;
  }

  public void subscribe(WebConnection con) throws Exception {
    subscribed.add(con);
    con.getChannel().closeFuture().addListener(f -> unsubscribe(con));
    onSubscribe(con);
  }

  protected abstract void onSubscribe(WebConnection con) throws Exception;

  public void unsubscribe(WebConnection con) throws Exception {
    subscribed.remove(con);
    onUnsubscribe(con);
  }

  protected abstract void onUnsubscribe(WebConnection con) throws Exception;

  public void push(PacketData data) {
    push(data.createMessage());
  }

  public void push(PacketMessage msg) {
    subscribed.forEach(c -> c.sendFast(msg));
  }
  
  public void pushAndFlush(PacketData data) {
    pushAndFlush(data.createMessage());
  }

  public void pushAndFlush(PacketMessage msg) {
    subscribed.forEach(c -> c.sendFastAndFlush(msg));
  }
}
