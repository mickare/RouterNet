package de.rennschnitzel.backbone.router.logging;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.fusesource.jansi.Ansi;

import jline.console.ConsoleReader;

public class SimpleLogger extends Logger {
	
	private static class ConsoleWriter extends Handler {
		private final ConsoleReader console;
		
		public ConsoleWriter( ConsoleReader console ) {
			this.console = console;
		}
		
		public void print( String s ) {
			try {
				console.print( ConsoleReader.RESET_LINE + s + Ansi.ansi().reset().toString() );
				console.drawLine();
				console.flush();
			} catch ( IOException ex ) {
			}
		}
		
		@Override
		public void publish( LogRecord record ) {
			if ( isLoggable( record ) ) {
				print( getFormatter().format( record ) );
			}
		}
		
		@Override
		public void flush() {
		}
		
		@Override
		public void close() throws SecurityException {
		}
	}
	
	private final Formatter formatter = new ConciseFormatter();
	private final LogDispatcher dispatcher = new LogDispatcher( this );
	private final FileHandler fileHandler;
	
	public SimpleLogger( ConsoleReader console ) throws IOException, SecurityException {
		super( "Niflhel", null );
		setLevel( Level.ALL );
		
		new File( "logs/" ).mkdir();
		
		fileHandler = new FileHandler( "logs/log%g.log", 1 << 24, 4, true );
		fileHandler.setFormatter( formatter );
		addHandler( fileHandler );
		
		ConsoleWriter writer = new ConsoleWriter( console );
		writer.setLevel( Level.INFO );
		writer.setFormatter( this.formatter );
		addHandler( writer );
		
		dispatcher.start();
	}
	
	@Override
	public void log( LogRecord record ) {
		dispatcher.queue( record );
	}
	
	void doLog( LogRecord record ) {
		super.log( record );
	}
	
	public void close() {
		this.fileHandler.close();
	}
	
}
