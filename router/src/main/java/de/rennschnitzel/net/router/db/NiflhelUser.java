package de.rennschnitzel.net.router.db;

import java.sql.Timestamp;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class NiflhelUser {

  private final int id;
  private final String email;
  private final int permission;
  private final Timestamp create_timestamp;
  private final boolean active;

}
