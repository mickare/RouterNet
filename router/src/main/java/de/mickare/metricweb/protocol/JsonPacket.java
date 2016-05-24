package de.mickare.metricweb.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public @NoArgsConstructor @AllArgsConstructor class JsonPacket {

  private @Getter String key;

  private @Getter Object object;

  public String toJson() {
    return JsonTools.GSON.toJson(this);
  }

}
