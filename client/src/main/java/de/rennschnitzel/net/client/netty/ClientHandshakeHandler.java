package de.rennschnitzel.net.client.netty;

import de.rennschnitzel.net.netty.AbstractHandshakeHandler;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginChallengeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginHandshakeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginResponseMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import io.netty.channel.ChannelHandlerContext;

public class ClientHandshakeHandler extends AbstractHandshakeHandler {

  public ClientHandshakeHandler() {
    super("ClientHandshake");
  }

  @Override
  public void handle(ChannelHandlerContext ctx, CloseMessage msg) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void handle(ChannelHandlerContext ctx, LoginHandshakeMessage msg) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void handle(ChannelHandlerContext ctx, LoginResponseMessage msg) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void handle(ChannelHandlerContext ctx, LoginChallengeMessage msg) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void handle(ChannelHandlerContext ctx, LoginSuccessMessage msg) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void handle(ChannelHandlerContext ctx, LoginUpgradeMessage msg) throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  protected void onFail(Throwable cause) {
    // TODO Auto-generated method stub

  }

}
