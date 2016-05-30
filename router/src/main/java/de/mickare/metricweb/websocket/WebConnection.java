package de.mickare.metricweb.websocket;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Preconditions;

import de.mickare.metricweb.protocol.WebProtocol.PacketData;
import de.mickare.metricweb.protocol.WebProtocol.PacketMessage;
import de.mickare.metricweb.protocol.WebProtocol.RegisteredPacket;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public @RequiredArgsConstructor class WebConnection {

  private static AtomicLong ID_COUNTER = new AtomicLong(0);

  private @Getter final long id = ID_COUNTER.incrementAndGet();
  private @Getter @NonNull final WebSocketServer server;
  private @Getter @NonNull final Channel channel;

  private final ConcurrentMap<Class<?>, PacketHandler<?>> packetHandlers =
      new ConcurrentHashMap<>();

  public <T extends PacketData> void registerMessageHandler(Class<T> packetClass,
      PacketHandler<T> handler) {
    registerMessageHandler(server.getProtocol().getRegisteredPacket(packetClass), handler);
  }

  public <T extends PacketData> void registerMessageHandler(String name, Class<T> packetClass,
      PacketHandler<T> handler) {
    registerMessageHandler(server.getProtocol().getRegisteredPacket(name, packetClass), handler);
  }

  public <T extends PacketData> void registerMessageHandler(RegisteredPacket<T> reg,
      PacketHandler<T> handler) {
    Preconditions.checkNotNull(reg);
    Preconditions.checkNotNull(handler);
    packetHandlers.put(reg.getPacketClass(), handler);
  }

  @SuppressWarnings("unchecked")
  private <T extends PacketData> PacketHandler<T> getPacketHandler(Class<T> packetClass) {
    return (PacketHandler<T>) packetHandlers.get(packetClass);
  }

  @FunctionalInterface
  public static interface PacketHandler<T extends PacketData> {
    public void handle(WebConnection connection, String packetName, T packetData) throws Exception;
  }

  @SuppressWarnings("unchecked")
  private <T extends PacketData> void callHandler(final String name, final T data)
      throws Exception {
    final PacketHandler<T> handler = (PacketHandler<T>) getPacketHandler(data.getClass());
    if (handler != null) {     
      handler.handle(this, name, data);
    }
  }

  public void handle(PacketMessage msg) throws Exception {
    callHandler(msg.getName(), msg.getData());
  }

  public ChannelFuture close() {
    return this.close(new CloseWebSocketFrame());
  }

  public ChannelFuture close(int statusCode, String reason) {
    return this.close(new CloseWebSocketFrame(statusCode, reason));
  }

  private ChannelFuture close(CloseWebSocketFrame closeFrame) {
    this.channel.write(closeFrame).addListener(ChannelFutureListener.CLOSE);
    return this.channel.closeFuture();
  }

  public ChannelFuture send(PacketData data) {
    return send(data.createMessage());
  }

  public void sendFast(PacketData data) {
    sendFast(data.createMessage());
  }

  public ChannelFuture send(PacketMessage message) {
    return this.channel.write(message);
  }

  public void sendFast(PacketMessage message) {
    this.channel.write(message, this.channel.voidPromise());
  }

  public void sendFastAndFlush(PacketMessage message) {
    this.channel.writeAndFlush(message, this.channel.voidPromise());
  }

  public ChannelFuture send(String json) {
    return this.channel.write(new TextWebSocketFrame(json));
  }

  public void sendFast(String json) {
    this.channel.write(new TextWebSocketFrame(json), this.channel.voidPromise());
  }

  public void flush() {
    this.channel.flush();
  }

  public boolean isConnected() {
    return this.channel.isActive();
  }


}
