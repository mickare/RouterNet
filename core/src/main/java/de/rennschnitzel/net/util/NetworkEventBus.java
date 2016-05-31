package de.rennschnitzel.net.util;

import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class NetworkEventBus extends AsyncEventBus {
	
	@RequiredArgsConstructor
	private static class ExceptionHandler implements SubscriberExceptionHandler {
		
		private @NonNull final Supplier<Logger> logger;
		
		@Override
		public void handleException( Throwable exception, SubscriberExceptionContext context ) {
			logger.get().log( Level.SEVERE, "Exception while handling event (" + context.getEvent().toString() + ")", exception );
		}
		
	}
	
	public NetworkEventBus( Executor executor, Supplier<Logger> logger ) {
		super( executor, new ExceptionHandler( logger ) );
	}
	
}
