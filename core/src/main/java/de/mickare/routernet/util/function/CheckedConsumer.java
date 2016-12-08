package de.mickare.routernet.util.function;

@FunctionalInterface
public interface CheckedConsumer<V> {
	void accept( V value ) throws Exception;
}
