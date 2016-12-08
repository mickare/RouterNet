package de.mickare.routernet.util.function;

@FunctionalInterface
public interface Callback<V> {
	
	void call( V value );
	
}
