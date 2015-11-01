package de.rennschnitzel.backbone.protocol;

import java.util.List;

import lombok.Data;
import de.rennschnitzel.backbone.Packet;

@Data
@Packet(name = "backbone.login")
public class LoginPacket {

  private String name;

  private List<String> namespaces;

}
