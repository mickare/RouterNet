package de.rennschnitzel.backbone.protocol;

import lombok.Data;
import de.rennschnitzel.backbone.Packet;
import de.rennschnitzel.backbone.network.Namespace;
import de.rennschnitzel.backbone.network.Server;

@Data
@Packet(name = "backbone.network.information")
public class NetworkInformationPacket {

  private Server[] servers;

  private Namespace[] namespaces;

}
