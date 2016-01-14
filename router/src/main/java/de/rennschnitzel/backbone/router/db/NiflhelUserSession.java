package de.rennschnitzel.backbone.router.db;

import java.sql.Timestamp;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class NiflhelUserSession {

  private final NiflhelUser user;
  private final String token;
  private final Timestamp insert_timestamp;
  private final Timestamp used_timestamp;

}
