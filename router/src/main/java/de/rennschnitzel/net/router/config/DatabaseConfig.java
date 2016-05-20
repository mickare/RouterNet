package de.rennschnitzel.net.router.config;

import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariDataSource;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;


// UNUSED, if used again undeprecate it.
@Deprecated
public @NoArgsConstructor @Data class DatabaseConfig {
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
    Preconditions.checkNotNull(this.serverName);
    Preconditions.checkNotNull(this.databaseName);
    Preconditions.checkNotNull(this.user);
    Preconditions.checkNotNull(this.password);

    HikariDataSource s = new HikariDataSource();
    s.setMaximumPoolSize(this.maximumPoolSize);
    s.setDataSourceClassName(this.dataSourceClassName);
    s.addDataSourceProperty("serverName", this.serverName);
    s.addDataSourceProperty("port", this.port);
    s.addDataSourceProperty("databaseName", this.databaseName);
    s.addDataSourceProperty("user", this.user);
    s.addDataSourceProperty("password", this.password);

    s.addDataSourceProperty("cachePrepStmts", this.cachePrepStmts);
    s.addDataSourceProperty("prepStmtCacheSize", this.prepStmtCacheSize);
    s.addDataSourceProperty("prepStmtCacheSqlLimit", this.prepStmtCacheSqlLimit);
    s.addDataSourceProperty("useServerPrepStmts", this.useServerPrepStmts);

    return s;
  }
}
