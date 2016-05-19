package de.rennschnitzel.net.router.command;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * 
 * @author Michael
 *
 *         Shows Help
 *
 */
public class HelpCommand extends AbstractCommand {
	
	private final CommandManager manager;
	
	public HelpCommand( CommandManager manager ) {
		super( "help", "help", "shows help" );
		this.manager = manager;
	}
	
	@Override
	public void execute( String[] args ) {
		
		List<Command> commands = Lists.newLinkedList( Sets.newHashSet( manager.getCommands().values() ) );
		Collections.sort( commands, new Comparator<Command>() {
			@Override
			public int compare( Command o1, Command o2 ) {
				return o1.getLabel().compareTo( o2.getLabel() );
			}
		} );
		
		StringBuilder sb = new StringBuilder();
		sb.append( "Available commands:" );
		
		for ( Command cmd : commands ) {
			sb.append( '\n' );
			sb.append( ' ' );
			sb.append( Strings.padEnd( cmd.getUsage(), 15, ' ' ) );
			sb.append( " - " ).append( cmd.getDescription() );
		}
		
		getLogger().info( sb.toString() );
		
	}
	
}
