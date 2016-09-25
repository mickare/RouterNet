package de.mickare.net.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BuildInfo {
	
	private final Properties props;
	
	public BuildInfo( ClassLoader loader, String filename ) throws IOException {
		this.props = new Properties();
		try ( InputStream in = loader.getResourceAsStream( filename ) ) {
			props.load( in );
		}
	}
	
	public String getName() {
		return props.getProperty( "name" );
	}
	
	public String getBuildVersion() {
		return props.getProperty( " build.version" );
	}
	
	public String getBuildUser() {
		return props.getProperty( "build.user" );
	}
	
	public String getBuildDate() {
		return props.getProperty( "build.date" );
	}
	
	public String getBuildOs() {
		return props.getProperty( "build.os" );
	}
	
	public String getJDK() {
		return props.getProperty( "build.jdk" );
	}
	
	public String toString() {
		return new StringBuilder().append( "Build {" )//
				.append( "name: " ).append( getName() ).append( ", " )//
				.append( "version: " ).append( getBuildVersion() ).append( ", " )//
				.append( "timestamp: " ).append( getBuildDate() ).append( ", " )//
				.append( "user:" ).append( getBuildUser() ).append( ", " )//
				.append( "os:" ).append( getBuildOs() ).append( ", " )//
				.append( "jdk:" ).append( getJDK() )//
				.append( "}" ).toString();
	}
	
}
