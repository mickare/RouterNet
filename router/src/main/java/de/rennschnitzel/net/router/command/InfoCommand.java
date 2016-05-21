package de.rennschnitzel.net.router.command;

import java.util.UUID;

import de.rennschnitzel.net.RouterNetwork;
import de.rennschnitzel.net.core.Connection;
import de.rennschnitzel.net.core.Node;
import de.rennschnitzel.net.router.Router;

public class InfoCommand extends AbstractCommand {
	
	public InfoCommand() {
		super( "info", "info <node>", "Shows info about a node." );
	}
	
	@Override
	public void execute( String[] args ) {
		
		if ( args.length == 0 ) {
			getLogger().info( "Missing argument!" );
			return;
		}
		
		String arg = args[0];
		
		if ( arg.length() == 36 ) {
			try {
				UUID id = UUID.fromString( arg );
				showInfo( id );
				return;
			} catch ( IllegalArgumentException iae ) {
			}
		}
		
		showInfo( String.join( " ", args ) );
		
	}
	
	public RouterNetwork getNetwork() {
		return Router.getInstance().getNetwork();
	}
	
	public void showInfo( Node node ) {
		if ( node == null ) {
			getLogger().info( "Node not found!" );
		} else {
			StringBuilder sb = new StringBuilder();
			
			sb.append( "Info on " ).append( node );
			sb.append( "\nNamespaces:" );
			if ( node.getNamespaces().isEmpty() ) {
				sb.append( " none" );
			} else {
				node.getNamespaces().forEach( n -> sb.append( "\n - " ).append( n ) );
			}
			
			sb.append( "\nProcedures:" );
			if ( node.getProcedures().isEmpty() ) {
				sb.append( " none" );
			} else {
				node.getProcedures().forEach( p -> sb.append( "\n - " ).append( p.toString() ) );
			}
			
			Connection con = getNetwork().getConnection( node.getId() );
			if ( con == null ) {
				sb.append( "\nConnected: false" );
			} else {
				sb.append( "\nConnected: " + con.isActive() );
				
				sb.append( "\nRemote Tunnels:" );
				if ( con.hasRemoteTunnels() ) {
					sb.append( " none" );
				} else {
					con.getRemoteTunnels().values().stream().forEach( t -> sb.append( "\n - " ).append( t ) );
				}
				
			}
			
			getLogger().info( sb.toString() );
		}
	}
	
	public void showInfo( UUID id ) {
		showInfo( getNetwork().getNode( id ) );
	}
	
	public void showInfo( String name ) {
		showInfo( getNetwork().getNode( name ) );
	}
	
}
