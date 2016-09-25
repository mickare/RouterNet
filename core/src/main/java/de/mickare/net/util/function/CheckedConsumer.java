package de.mickare.net.util.function;

@FunctionalInterface
public interface CheckedConsumer<V> {
	void accept( V value ) throws Exception;
}
