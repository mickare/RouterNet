package de.rennschnitzel.backbone.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;

import net.jodah.typetools.TypeResolver;

public class TypeUtils {

  private TypeUtils() {
    // TODO Auto-generated constructor stub
  }


  public static <T, R> Class<?>[] resolveArgumentClass(Function<T, R> function) {
    Preconditions.checkNotNull(function);
    return TypeResolver.resolveRawArguments(Function.class, function.getClass());
  }

  public static <T> Class<?> resolveArgumentClass(Consumer<T> function) {
    Preconditions.checkNotNull(function);
    return TypeResolver.resolveRawArgument(Consumer.class, function.getClass());
  }

  public static <R> Class<?> resolveArgumentClass(Supplier<R> function) {
    Preconditions.checkNotNull(function);
    return TypeResolver.resolveRawArgument(Supplier.class, function.getClass());
  }

}
