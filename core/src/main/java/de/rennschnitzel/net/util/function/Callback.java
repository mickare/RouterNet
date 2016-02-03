package de.rennschnitzel.net.util.function;

@FunctionalInterface
public interface Callback<V> {

  void call(V value);

}
