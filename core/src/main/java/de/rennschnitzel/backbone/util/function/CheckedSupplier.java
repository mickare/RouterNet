package de.rennschnitzel.backbone.util.function;

@FunctionalInterface
public interface CheckedSupplier<T> {
  T get() throws Exception;
}
