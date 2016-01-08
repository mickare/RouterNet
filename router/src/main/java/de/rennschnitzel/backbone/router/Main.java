package de.rennschnitzel.backbone.router;

import java.util.logging.Logger;

import de.rennschnitzel.backbone.Router;

public class Main {

  public static void main(String[] args) {


    Settings settings = new Settings();

    Router router = new Router(settings.getAddress().getHostAndPort());

    Logger logger = Logger.getLogger("Router");

    RouterServer server = new RouterServer(router, null);


  }

}
