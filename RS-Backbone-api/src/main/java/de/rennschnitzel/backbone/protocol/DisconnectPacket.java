package de.rennschnitzel.backbone.protocol;

import lombok.Data;
import de.rennschnitzel.backbone.Packet;

@Data
@Packet(name = "backbone.disconnect")
public class DisconnectPacket {

  private String reason;

}
