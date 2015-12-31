package de.rennschnitzel.backbone.net.channel.object;

import com.google.protobuf.ByteString;

public interface ObjectConverter<T> {

  ByteString asByteString(T obj) throws ConvertObjectChannelException;

  T asObject(ByteString byteData) throws ConvertObjectChannelException;

}
