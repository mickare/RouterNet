package de.rennschnitzel.net.netty.handshake;

import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;

import de.rennschnitzel.net.Protocol;
import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.exception.ConnectionException;
import de.rennschnitzel.net.exception.ProtocolException;
import de.rennschnitzel.net.netty.PacketUtil;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginChallengeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginHandshakeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginResponseMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ErrorMessage;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;

public abstract class ClientHandshakeHandler<C extends Connection>
    extends AbstractHandshakeHandler<C> {

  @Getter
  private final AbstractNetwork network;
  private ClientAuthentication authentication = null;

  public ClientHandshakeHandler(String handlerName, AbstractNetwork network,
      ClientAuthentication authentication) {
    super(handlerName);
    Preconditions.checkNotNull(network);
    Preconditions.checkNotNull(authentication);
    this.network = network;
    this.authentication = authentication;
  }

  @Override
  public void handle(ChannelHandlerContext ctx, CloseMessage msg) throws Exception {
    switch (msg.getReasonCase()) {
      case ERROR:
        this.fail(new ConnectionException(msg.getError()));
        break;
      case NORMAL:
        this.fail(new ConnectionException(ErrorMessage.Type.UNRECOGNIZED, msg.getNormal()));
        break;
      case SHUTDOWN:
        this.fail(new ConnectionException(ErrorMessage.Type.UNAVAILABLE, "shutdown"));
        break;
      default:
        fail(new ProtocolException("invalid close reason"));
    }
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    checkState(State.NEW);
    LoginHandshakeMessage.Builder msg = LoginHandshakeMessage.newBuilder();
    msg.setProtocolVersion(Protocol.VERSION);
    msg.setId(ProtocolUtils.convert(network.getHome().getId()));
    Optional<String> name = network.getHome().getName();
    if (name.isPresent()) {
      msg.setName(name.get());
    }
    PacketUtil.writeAndFlush(ctx.channel(), msg);
    setState(State.LOGIN);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, LoginHandshakeMessage msg) throws Exception {
    throw new ProtocolException("invalid packet");
  }

  @Override
  public void handle(ChannelHandlerContext ctx, LoginResponseMessage msg) throws Exception {
    throw new ProtocolException("invalid packet");
  }

  @Override
  public void handle(ChannelHandlerContext ctx, LoginChallengeMessage msg) throws Exception {
    checkState(State.LOGIN);
    ByteString response = this.authentication.calculateResponse(msg.getToken());
    PacketUtil.writeAndFlush(ctx.channel(), LoginResponseMessage.newBuilder().setToken(response));
    setState(State.AUTH);
  }

  @Override
  public void handle(ChannelHandlerContext ctx, LoginSuccessMessage msg) throws Exception {
    checkState(State.AUTH);
    upgrade(msg);
    checkState(State.SUCCESS);
  }

  protected abstract void upgrade(LoginSuccessMessage msg);

  @Override
  public void handle(ChannelHandlerContext ctx, LoginUpgradeMessage msg) throws Exception {
    throw new ProtocolException("invalid packet");
  }

  @Override
  protected void onFail(Throwable cause) {
    if (!this.getChannelContext().channel().isActive()) {
      return;
    }
    PacketUtil.writeAndFlush(this.getChannelContext().channel(),
        CloseMessage.newBuilder().setError(ErrorMessage.newBuilder()
            .setType(ErrorMessage.Type.HANDSHAKE).setMessage(cause.getMessage())));
  }

}
