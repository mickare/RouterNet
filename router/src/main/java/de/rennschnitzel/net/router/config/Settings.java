package de.rennschnitzel.net.router.config;

import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;
import com.zaxxer.hikari.HikariDataSource;

import jline.internal.Preconditions;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor
@Data
public class Settings {
	
	@NoArgsConstructor
	@Data
	public static class DatabaseConfig {
		private int maximumPoolSize = 1;
		@NonNull
		private String dataSourceClassName = "com.mysql.jdbc.jdbc2.optional.MysqlDataSource";
		@NonNull
		private String serverName = null;
		private int port = 3306;
		@NonNull
		private String databaseName = null;
		@NonNull
		private String user = null;
		@NonNull
		private String password = null;
		
		// Recommended MySQL Settings
		private boolean cachePrepStmts = true;
		private int prepStmtCacheSize = 250;
		private int prepStmtCacheSqlLimit = 2048;
		private boolean useServerPrepStmts = true;
		
		public HikariDataSource toDataSource() {
			Preconditions.checkNotNull( this.serverName );
			Preconditions.checkNotNull( this.databaseName );
			Preconditions.checkNotNull( this.user );
			Preconditions.checkNotNull( this.password );
			
			HikariDataSource s = new HikariDataSource();
			s.setMaximumPoolSize( this.maximumPoolSize );
			s.setDataSourceClassName( this.dataSourceClassName );
			s.addDataSourceProperty( "serverName", this.serverName );
			s.addDataSourceProperty( "port", this.port );
			s.addDataSourceProperty( "databaseName", this.databaseName );
			s.addDataSourceProperty( "user", this.user );
			s.addDataSourceProperty( "password", this.password );
			
			s.addDataSourceProperty( "cachePrepStmts", this.cachePrepStmts );
			s.addDataSourceProperty( "prepStmtCacheSize", this.prepStmtCacheSize );
			s.addDataSourceProperty( "prepStmtCacheSqlLimit", this.prepStmtCacheSqlLimit );
			s.addDataSourceProperty( "useServerPrepStmts", this.useServerPrepStmts );
			
			return s;
		}
	}
	
	public static @Data @NoArgsConstructor class RouterConfig {
		private NodeSettings home = new NodeSettings();
		private String address = "localhost:1010";
		private String password = "Wkn2Z[uBYT]x1T/hY1Ac";
	}
	
	public static @Data @NoArgsConstructor class NodeSettings {
		private UUID id = UUID.randomUUID();
		private Set<String> namespaces = Sets.newHashSet();
		private String name = null;
	}
	
	private RouterConfig routerSettings = new RouterConfig();
	private DatabaseConfig databaseSettings = new DatabaseConfig();
	
}
