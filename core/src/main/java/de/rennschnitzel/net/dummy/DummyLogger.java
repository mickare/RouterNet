package de.rennschnitzel.net.dummy;

import java.io.PrintStream;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.util.logging.DummyConciseFormatter;

public class DummyLogger extends Logger {
	
	private final Formatter formatter = new DummyConciseFormatter();
	private final PrintStream out;
	
	public DummyLogger( String name, PrintStream out ) {
		super( name, null );
		setLevel( Level.ALL );
		Preconditions.checkNotNull( out );
		this.out = out;
	}
	
	@Override
	public void log( LogRecord record ) {
		out.println( formatter.format( record ) );
	}
	
	void doLog( LogRecord record ) {
		super.log( record );
	}
	
}
