package de.mickare.net.util.function;

@FunctionalInterface
public interface Callback<V> {
	
	void call( V value );
	
}
