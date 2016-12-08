package de.mickare.routernet.util;

import java.util.function.Function;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import de.mickare.routernet.util.function.CheckedBiFunction;
import de.mickare.routernet.util.function.CheckedConsumer;
import de.mickare.routernet.util.function.CheckedFunction;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;

public class FutureUtils {
	
	public static final Future<Void> SUCCESS = futureSuccess( null );
	
	public static <V> Future<V> futureSuccess( final V value ) {
		return ImmediateEventExecutor.INSTANCE.<V> newSucceededFuture( value );
	}
	
	public static <V> Future<V> futureFailure( final Throwable t ) {
		return ImmediateEventExecutor.INSTANCE.<V> newFailedFuture( t );
	}
	
	public static <V> Promise<V> newPromise() {
		return ImmediateEventExecutor.INSTANCE.newPromise();
	}
	
	public static <V> Future<V> transformFuture( final ListenableFuture<V> future ) {
		final Promise<V> promise = ImmediateEventExecutor.INSTANCE.newPromise();
		Futures.addCallback( future, new FutureCallback<V>() {
			@Override
			public void onSuccess( V result ) {
				promise.setSuccess( result );
			}
			
			@Override
			public void onFailure( Throwable t ) {
				promise.setFailure( t );
			}
		} );
		return promise;
	}
	
	public static <V, F extends Future<V>> void on( F future, final CheckedConsumer<Future<V>> callback ) {
		if ( future.isDone() ) {
			try {
				callback.accept( future );
			} catch ( Exception e ) {
				throw new RuntimeException( e );
			}
		} else {
			future.addListener( ( FutureListener<V> ) callback::accept );
		}
	}
	
	public static <V, F extends Future<V>> void onSuccess( F future, final CheckedConsumer<V> callback ) {
		if ( future.isSuccess() ) {
			try {
				callback.accept( future.get() );
			} catch ( Exception e ) {
				throw new RuntimeException( e );
			}
		} else {
			future.addListener( ( FutureListener<V> ) f -> {
				if ( f.isSuccess() ) {
					callback.accept( f.get() );
				}
			} );
		}
	}
	
	public static <V> Promise<V> dereference( final Future<? extends Future<? extends V>> nested ) {
		return dereference( newPromise(), nested );
	}
	
	public static <V> Promise<V> dereference( final Promise<V> promise, final Future<? extends Future<? extends V>> nested ) {
		nested.addListener( ( FutureListener<Future<? extends V>> ) future -> {
			if ( future.isSuccess() ) {
				try {
					future.get().addListener( ( FutureListener<V> ) result -> {
						try {
							if ( result.isSuccess() ) {
								promise.trySuccess( result.get() );
							} else {
								promise.tryFailure( result.cause() );
							}
						} catch ( Exception e ) {
							promise.tryFailure( e );
						}
					} );
				} catch ( Exception e ) {
					promise.tryFailure( e );
				}
			} else {
				promise.tryFailure( future.cause() );
			}
		} );
		return promise;
	}
	
	public static <P> Promise<P> propagateFailure( final Promise<P> target, final Future<?> failable ) {
		failable.addListener( f -> {
			if ( !f.isSuccess() ) {
				target.tryFailure( f.cause() );
			}
		} );
		return target;
	}
	
	public static <V> Promise<V> propagate( final Promise<V> target, final Future<V> delegate ) {
		on( delegate, f -> {
			if ( f.isSuccess() ) {
				target.trySuccess( f.get() );
			} else {
				target.tryFailure( f.cause() );
			}
		} );
		return target;
	}
	
	public static <T, U, R> Future<R> call( final Future<T> future, final CheckedBiFunction<T, U, Future<R>> func, final U value ) {
		final Promise<R> promise = ImmediateEventExecutor.INSTANCE.newPromise();
		on( future, f -> {
			if ( f.isSuccess() ) {
				try {
					propagate( promise, func.apply( f.get(), value ) );
				} catch ( Exception e ) {
					promise.tryFailure( f.cause() );
				}
			} else {
				promise.tryFailure( f.cause() );
			}
		} );
		return promise;
	}
	
	public static <T, R> Future<R> call( final Future<T> future, final CheckedFunction<T, Future<R>> func ) {
		final Promise<R> promise = ImmediateEventExecutor.INSTANCE.newPromise();
		future.addListener( ( FutureListener<T> ) f -> {
			if ( f.isSuccess() ) {
				try {
					propagate( promise, func.apply( f.get() ) );
				} catch ( Exception e ) {
					promise.setFailure( e );
				}
			} else {
				promise.setFailure( f.cause() );
			}
		} );
		
		return promise;
	}
	
	public static <T, R> Future<R> lazyTransform( final Future<T> future, final Function<T, R> convert ) {
		final Promise<R> promise = ImmediateEventExecutor.INSTANCE.newPromise();
		on( future, t -> {
			if ( t.isSuccess() ) {
				try {
					promise.setSuccess( convert.apply( t.get() ) );
				} catch ( Exception e ) {
					promise.setFailure( e );
				}
			} else {
				promise.setFailure( t.cause() );
			}
		} );
		return promise;
	}
	
}
