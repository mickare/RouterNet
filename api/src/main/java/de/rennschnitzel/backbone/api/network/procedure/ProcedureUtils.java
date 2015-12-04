package de.rennschnitzel.backbone.api.network.procedure;

import java.util.function.BiConsumer;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import de.rennschnitzel.backbone.api.network.Network;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureCall;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureResponse;
import de.rennschnitzel.backbone.util.CheckedFunction;

public class ProcedureUtils {

  @SuppressWarnings("unchecked")
  public static <R> CheckedFunction<ProcedureResponse, R> compileResponseReader(Class<R> resultClass) throws IllegalArgumentException {

    if (Void.class.equals(resultClass)) {
      return (p) -> null;
    } else if (byte[].class.equals(resultClass)) {
      return (p) -> (R) p.getBytes().toByteArray();
    } else if (Any.class.equals(resultClass)) {
      return (p) -> (R) p.getProtoValue();
    } else if (com.google.protobuf.Message.class.isAssignableFrom(resultClass)) {
      return (p) -> (R) p.getProtoValue().unpack((Class<? extends com.google.protobuf.Message>) resultClass);
    } else if (Object.class.isAssignableFrom(resultClass)) {
      return (p) -> (R) Network.FST.getObjectInput(p.getObjectValue().toByteArray()).readObject();
    } else {
      throw new IllegalArgumentException("Unsupported response type!");
    }

  }


  public static <R> BiConsumer<ProcedureResponse.Builder, R> compileResponseWriter(Class<R> resultClass) throws IllegalArgumentException {

    if (Void.class.equals(resultClass)) {
      return (p, r) -> p.clearData();
    } else if (byte[].class.equals(resultClass)) {
      return (p, r) -> p.setBytes(ByteString.copyFrom((byte[]) r));
    } else if (Any.class.equals(resultClass)) {
      return (p, r) -> p.setProtoValue((Any) r);
    } else if (com.google.protobuf.Message.class.isAssignableFrom(resultClass)) {
      return (p, r) -> p.setProtoValue(Any.pack((Message) r));
    } else if (Object.class.isAssignableFrom(resultClass)) {
      return (p, r) -> p.setObjectValue(ByteString.copyFrom(Network.FST.asByteArray(r)));
    } else {
      throw new IllegalArgumentException("Unsupported response type!");
    }

  }


  @SuppressWarnings("unchecked")
  public static <A> CheckedFunction<ProcedureCall, A> compileCallReader(Class<A> argClass) throws IllegalArgumentException {

    if (Void.class.equals(argClass)) {
      return (p) -> null;
    } else if (byte[].class.equals(argClass)) {
      return (p) -> (A) p.getBytes().toByteArray();
    } else if (Any.class.equals(argClass)) {
      return (p) -> (A) p.getProtoValue();
    } else if (com.google.protobuf.Message.class.isAssignableFrom(argClass)) {
      return (p) -> (A) p.getProtoValue().unpack((Class<? extends com.google.protobuf.Message>) argClass);
    } else if (Object.class.isAssignableFrom(argClass)) {
      return (p) -> (A) Network.FST.getObjectInput(p.getObjectValue().toByteArray()).readObject();
    } else {
      throw new IllegalArgumentException("Unsupported call type!");
    }

  }

  public static <A> BiConsumer<ProcedureCall.Builder, A> compileCallWriter(Class<A> argClass) throws IllegalArgumentException {

    if (Void.class.equals(argClass)) {
      return (p, r) -> p.clearData();
    } else if (byte[].class.equals(argClass)) {
      return (p, r) -> p.setBytes(ByteString.copyFrom((byte[]) r));
    } else if (Any.class.equals(argClass)) {
      return (p, r) -> p.setProtoValue((Any) r);
    } else if (com.google.protobuf.Message.class.isAssignableFrom(argClass)) {
      return (p, r) -> p.setProtoValue(Any.pack((Message) r));
    } else if (Object.class.isAssignableFrom(argClass)) {
      return (p, r) -> p.setObjectValue(ByteString.copyFrom(Network.FST.asByteArray(r)));
    } else {
      throw new IllegalArgumentException("Unsupported call type!");
    }

  }

}
