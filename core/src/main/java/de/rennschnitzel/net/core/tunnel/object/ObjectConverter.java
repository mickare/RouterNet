package de.rennschnitzel.net.core.tunnel.object;

import com.google.protobuf.ByteString;

public interface ObjectConverter<T> {

  ByteString asByteString(T obj) throws ConvertObjectTunnelException;

  T asObject(ByteString byteData) throws ConvertObjectTunnelException;

}
