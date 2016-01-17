package de.rennschnitzel.net.util.function;

@FunctionalInterface
public interface CheckedSupplier<T> {
  T get() throws Exception;
}
