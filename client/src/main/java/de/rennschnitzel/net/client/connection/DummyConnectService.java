package de.rennschnitzel.net.client.connection;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.NetClient;
import de.rennschnitzel.net.Network;
import de.rennschnitzel.net.client.TestFramework;
import de.rennschnitzel.net.dummy.DummClientNetwork;
import de.rennschnitzel.net.dummy.DummyConnection;
import de.rennschnitzel.net.dummy.DummyLoginClientHandler;
import de.rennschnitzel.net.dummy.DummyLoginRouterHandler;
import de.rennschnitzel.net.util.FutureUtils;
import io.netty.util.concurrent.Future;
import lombok.Getter;

public class DummyConnectService
    extends AbstractConnectService<DummyLoginClientHandler, DummyConnection> {

  @Getter
  private final TestFramework testFramework;

  public DummyConnectService(NetClient client, TestFramework testFramework) {
    super(client);
    Preconditions.checkNotNull(testFramework);
    this.testFramework = testFramework;
  }

  @Override
  protected DummyLoginClientHandler createLoginHandler() {
    return new DummyLoginClientHandler(this.getClient().getNetwork(),
        this.getClient().getAuthentication(), this.createPacketHandler());
  }

  @Override
  protected Future<?> doConnect(DummyLoginClientHandler loginHandler) {

    Network net_client = getClient().getNetwork();
    Preconditions.checkArgument(loginHandler.getNetwork() == net_client);
    DummClientNetwork net_router = testFramework.getRouterNetwork();

    DummyLoginRouterHandler login_router = new DummyLoginRouterHandler(net_router,
        testFramework.getAuthenticationRouter(), this.createPacketHandler());

    DummyConnection con_client = new DummyConnection(net_client, loginHandler);
    DummyConnection con_router = new DummyConnection(net_router, login_router);

    con_client.connect(con_router);

    return FutureUtils.SUCCESS;
  }

}
