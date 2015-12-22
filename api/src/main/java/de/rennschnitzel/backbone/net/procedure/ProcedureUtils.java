package de.rennschnitzel.backbone.net.procedure;

import java.io.Serializable;
import java.util.function.BiConsumer;

import com.google.protobuf.ByteString;

import de.rennschnitzel.backbone.net.Network;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureCallMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.backbone.util.function.CheckedFunction;

public class ProcedureUtils {

  @SuppressWarnings("unchecked")
  public static <R> CheckedFunction<ProcedureResponseMessage, R> compileResponseReader(final Class<R> resultClass)
      throws IllegalArgumentException {

    if (Void.class.equals(resultClass)) {
      return (p) -> null;
    } else if (byte[].class.equals(resultClass)) {
      return (p) -> (R) p.getBytes().toByteArray();
    } else if (Serializable.class.isAssignableFrom(resultClass)) {
      return (p) -> (R) Network.SERIALIZATION.getObjectInput(p.getObject().toByteArray()).readObject(resultClass);
    } else {
      throw new IllegalArgumentException("Unsupported response type!");
    }

  }


  public static <R> BiConsumer<ProcedureResponseMessage.Builder, R> compileResponseWriter(Class<R> resultClass)
      throws IllegalArgumentException {

    if (Void.class.equals(resultClass)) {
      return (p, r) -> p.clearData();
    } else if (byte[].class.equals(resultClass)) {
      return (p, r) -> p.setBytes(ByteString.copyFrom((byte[]) r));
    } else if (Serializable.class.isAssignableFrom(resultClass)) {
      return (p, r) -> p.setObject(ByteString.copyFrom(Network.SERIALIZATION.asByteArray(r)));
    } else {
      throw new IllegalArgumentException("Unsupported response type!");
    }

  }


  @SuppressWarnings("unchecked")
  public static <A> CheckedFunction<ProcedureCallMessage, A> compileCallReader(Class<A> argClass) throws IllegalArgumentException {

    if (Void.class.equals(argClass)) {
      return (p) -> null;
    } else if (byte[].class.equals(argClass)) {
      return (p) -> (A) p.getBytes().toByteArray();
    } else if (Serializable.class.isAssignableFrom(argClass)) {
      return (p) -> (A) Network.SERIALIZATION.getObjectInput(p.getObject().toByteArray()).readObject(argClass);
    } else {
      throw new IllegalArgumentException("Unsupported call type!");
    }

  }

  public static <A> BiConsumer<ProcedureCallMessage.Builder, A> compileCallWriter(Class<A> argClass) throws IllegalArgumentException {

    if (Void.class.equals(argClass)) {
      return (p, r) -> p.clearData();
    } else if (byte[].class.equals(argClass)) {
      return (p, r) -> p.setBytes(ByteString.copyFrom((byte[]) r));
    } else if (Serializable.class.isAssignableFrom(argClass)) {
      return (p, r) -> p.setObject(ByteString.copyFrom(Network.SERIALIZATION.asByteArray(r)));
    } else {
      throw new IllegalArgumentException("Unsupported call type!");
    }

  }

}
