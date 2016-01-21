package de.rennschnitzel.net.client.netty;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.rennschnitzel.net.netty.login.AbstractHandshakeHandler;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginChallengeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginHandshakeMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginResponseMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginSuccessMessage;
import de.rennschnitzel.net.protocol.LoginProtocol.LoginUpgradeMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.CloseMessage;
import io.netty.channel.ChannelHandlerContext;

public class ClientHandshakeHandler extends AbstractHandshakeHandler<ClientConnection> {

  public ClientHandshakeHandler() {
    super("ClientHandshake");
  }

  @Override
  public boolean isOpen() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void addListener(Runnable arg0, Executor arg1) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isCancelled() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isDone() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public ClientConnection get() throws InterruptedException, ExecutionException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ClientConnection get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    // TODO Auto-generated method stub
    return null;
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
