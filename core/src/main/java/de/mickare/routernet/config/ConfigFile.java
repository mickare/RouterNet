package de.mickare.routernet.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import lombok.Getter;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ConfigFile<T> {
	
	private static final Gson GSON;
	
	static {
		GsonBuilder b = new GsonBuilder();
		b.serializeNulls();
		b.setPrettyPrinting();
		GSON = b.create();
	}
	
	public static <T> ConfigFile<T> create( final File file, final Class<T> configClass ) {
		return new ConfigFile<T>( file, configClass );
	}
	
	@Getter
	private final File file;
	@Getter
	private final Class<T> configClass;
	
	private T config = null;
	
	public ConfigFile( final File file, final Class<T> configClass ) {
		Preconditions.checkNotNull( file );
		Preconditions.checkNotNull( configClass );
		Preconditions.checkArgument( !file.isDirectory(), "file is not a file" );
		try {
			configClass.getConstructor();
		} catch ( final NoSuchMethodException | SecurityException e ) {
			throw new IllegalArgumentException( "config class has not an empty constructor", e );
		}
		this.file = file.getAbsoluteFile();
		this.configClass = configClass;
	}
	
	private T _createInstance() {
		try {
			return this.configClass.newInstance();
		} catch ( final InstantiationException | IllegalAccessException e ) {
			throw new RuntimeException( "config class has invalid constructor", e );
		}
	}
	
	public synchronized T getConfig() {
		if ( this.config == null ) {
			try {
				this.reload();
			} catch ( IOException e ) {
				throw new RuntimeException( e );
			}
		}
		return this.config;
	}
	
	public synchronized void reload() throws IOException {
		if ( !this.file.exists() ) {
			this.config = _createInstance();
			return;
		}
		try ( final BufferedReader reader = Files.newReader( this.file, Charsets.UTF_8 ) ) {
			this.config = GSON.fromJson( reader, this.configClass );
		}
	}
	
	private synchronized void _save( final T config ) throws IOException {
		File parent = this.file.getParentFile();
		if(parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		final File tmp = File.createTempFile( this.file.getName(), ".tmp", parent );
		try ( final BufferedWriter writer = Files.newWriter( tmp, Charsets.UTF_8 ) ) {
			GSON.toJson( config, writer );
		}
		Files.move( tmp, this.file );
	}
	
	public void save() throws IOException {
		this._save( this.config );
	}
	
	public synchronized void saveDefault() {
		if ( !this.file.exists() ) {
			try {
				this._save( this._createInstance() );
			} catch ( final IOException e ) {
				throw new RuntimeException( e );
			}
		}
	}
}
