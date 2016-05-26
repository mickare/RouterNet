package de.rennschnitzel.net.router.plugin;

public @interface RouterPlugin {

  String name();

  String version() default "";

  String author() default "none";

}
