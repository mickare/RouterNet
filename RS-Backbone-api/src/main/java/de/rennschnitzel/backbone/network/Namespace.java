package de.rennschnitzel.backbone.network;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class Namespace implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = -7192080618032217637L;

  private String name;

  private List<Server> servers;

}
