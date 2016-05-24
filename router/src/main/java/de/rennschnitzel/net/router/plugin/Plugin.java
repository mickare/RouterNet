package de.rennschnitzel.net.router.plugin;

public @interface Plugin {

  String name();

  String version() default "";

  String author() default "none";

}
