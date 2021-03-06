package de.mickare.routernet.util.function;

@FunctionalInterface
public interface CheckedBiFunction<T, U, R> {
	R apply( T t, U u ) throws Exception;
}
