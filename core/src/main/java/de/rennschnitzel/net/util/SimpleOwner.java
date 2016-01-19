package de.rennschnitzel.net.util;

import java.util.logging.Logger;

import de.rennschnitzel.net.Owner;
import lombok.Data;
import lombok.NonNull;

@Data
public class SimpleOwner implements Owner {

  @NonNull
  private final String name;
  @NonNull
  private final Logger logger;
  
}
