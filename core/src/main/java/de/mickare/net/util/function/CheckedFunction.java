package de.mickare.net.util.function;

@FunctionalInterface
public interface CheckedFunction<T, R> {
	R apply( T t ) throws Exception;
}
