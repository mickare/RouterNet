package de.rennschnitzel.net.util.function;

@FunctionalInterface
public interface CheckedBiFunction<T, U, R> {
  R apply(T t, U u) throws Exception;
}
