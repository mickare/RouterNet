package de.rennschnitzel.backbone.api.network.procedure;

import java.util.function.BiConsumer;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import de.rennschnitzel.backbone.api.network.Network;
import de.rennschnitzel.backbone.api.network.Server;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureCall.Builder;
import lombok.Getter;

@Getter
public class RemoteProcedure<T, R> implements Procedure<T, R> {

  private final String name;
  private final Class<T> argClass;
  private final Class<R> resultClass;
  private final Server server;
  private final BiConsumer<Builder, T> argumentWriter;

  public RemoteProcedure(String name, Class<T> argClass, Class<R> resultClass, Server server)
      throws NullPointerException, IllegalArgumentException {
    Preconditions.checkArgument(!name.isEmpty());
    Preconditions.checkNotNull(argClass);
    Preconditions.checkNotNull(resultClass);
    Preconditions.checkNotNull(server);
    this.name = name;
    this.argClass = argClass;
    this.resultClass = resultClass;
    this.server = server;


    // Compile writer

    if (Void.class.equals(argClass)) {
      argumentWriter = (p, r) -> p.clearData();
    } else if (byte[].class.equals(argClass)) {
      argumentWriter = (p, r) -> p.setBytes(ByteString.copyFrom((byte[]) r));
    } else if (Any.class.equals(argClass)) {
      argumentWriter = (p, r) -> p.setProtoValue((Any) r);
    } else if (com.google.protobuf.Message.class.isAssignableFrom(argClass)) {
      argumentWriter = (p, r) -> p.setProtoValue(Any.pack((Message) r));
    } else if (Object.class.isAssignableFrom(argClass)) {
      argumentWriter = (p, r) -> p.setObjectValue(ByteString.copyFrom(Network.FST.asByteArray(r)));
    } else {
      throw new IllegalArgumentException("Unsupported argument type!");
    }

  }

  @Override
  public ListenableFuture<R> call(T arg) {
    return server.call(this, arg);
  }

  @Override
  public String getArgClassName() {
    return argClass.getName();
  }

  @Override
  public String getResultClassName() {
    return resultClass.getName();
  }


}
