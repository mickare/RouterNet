package de.mickare.routernet.util.function;

@FunctionalInterface
public interface CheckedSupplier<T> {
	T get() throws Exception;
}
