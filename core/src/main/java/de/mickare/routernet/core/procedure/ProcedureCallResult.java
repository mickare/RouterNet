package de.mickare.routernet.core.procedure;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.UncheckedExecutionException;

import de.mickare.routernet.core.Node;
import de.mickare.routernet.util.FutureUtils;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import lombok.Getter;

public class ProcedureCallResult<T, R> implements Future<R> {
	
	private @Getter final ProcedureCall<T, R> call;
	private @Getter final Node node;
	private @Getter long completionTime = -1;
	private final Promise<R> promise = FutureUtils.newPromise();
	
	protected ProcedureCallResult( ProcedureCall<T, R> call, Node node ) {
		Preconditions.checkNotNull( call );
		Preconditions.checkNotNull( node );
		this.call = call;
		this.node = node;
	}
	
	private boolean setCompletionTime( boolean doSet ) {
		if ( doSet ) {
			completionTime = System.currentTimeMillis();
		}
		return doSet;
	}
	
	public R getUnchecked() throws UncheckedExecutionException {
		try {
			return this.get();
		} catch ( InterruptedException | ExecutionException e ) {
			throw new UncheckedExecutionException( e );
		}
	}
	
	@Override
	public boolean cancel( boolean mayInterruptIfRunning ) {
		return promise.cancel( mayInterruptIfRunning );
	}
	
	protected boolean set( R value ) {
		return setCompletionTime( promise.trySuccess( value ) );
	}
	
	protected boolean setException( Throwable throwable ) {
		return setCompletionTime( promise.tryFailure( throwable ) );
	}
	
	public ProcedureCallResult<T, R> addResultListener( Consumer<ProcedureCallResult<T, R>> listener ) {
		this.promise.addListener( p -> listener.accept( this ) );
		return this;
	}
	
	@Override
	public boolean isCancelled() {
		return promise.isCancelled();
	}
	
	@Override
	public boolean isDone() {
		return promise.isDone();
	}
	
	@Override
	public R get() throws InterruptedException, ExecutionException {
		return promise.get();
	}
	
	@Override
	public R get( long timeout, TimeUnit unit ) throws InterruptedException, ExecutionException, TimeoutException {
		return promise.get( timeout, unit );
	}
	
	@Override
	public boolean isCancellable() {
		return promise.isCancellable();
	}
	
	@Override
	public Throwable cause() {
		return promise.cause();
	}
	
	@Override
	public Future<R> addListener( GenericFutureListener<? extends Future<? super R>> listener ) {
		return promise.addListener( listener );
	}
	
	@SuppressWarnings( "unchecked" )
	@Override
	public Future<R> addListeners( GenericFutureListener<? extends Future<? super R>>... listeners ) {
		return promise.addListeners( listeners );
	}
	
	@Override
	public Future<R> removeListener( GenericFutureListener<? extends Future<? super R>> listener ) {
		return promise.removeListener( listener );
	}
	
	@SuppressWarnings( "unchecked" )
	@Override
	public Future<R> removeListeners( GenericFutureListener<? extends Future<? super R>>... listeners ) {
		return promise.removeListeners( listeners );
	}
	
	@Override
	public Future<R> sync() throws InterruptedException {
		return promise.sync();
	}
	
	@Override
	public Future<R> syncUninterruptibly() {
		return promise.syncUninterruptibly();
	}
	
	@Override
	public Future<R> await() throws InterruptedException {
		return promise.await();
	}
	
	@Override
	public Future<R> awaitUninterruptibly() {
		return promise.awaitUninterruptibly();
	}
	
	@Override
	public boolean await( long timeout, TimeUnit unit ) throws InterruptedException {
		return promise.await( timeout, unit );
	}
	
	@Override
	public boolean await( long timeoutMillis ) throws InterruptedException {
		return promise.await( timeoutMillis );
	}
	
	@Override
	public boolean awaitUninterruptibly( long timeout, TimeUnit unit ) {
		return promise.awaitUninterruptibly( timeout, unit );
	}
	
	@Override
	public boolean awaitUninterruptibly( long timeoutMillis ) {
		return promise.awaitUninterruptibly( timeoutMillis );
	}
	
	@Override
	public R getNow() {
		return promise.getNow();
	}
	
	@Override
	public boolean isSuccess() {
		return promise.isSuccess();
	}
}
