package de.rennschnitzel.net.router.plugin;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.router.Router;
import lombok.Getter;

public abstract class Plugin {

  @Target(value = {ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface Name {
    String value();
  }
  @Target(value = {ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface Version {
    String value();
  }
  @Target(value = {ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface Author {
    String value();
  }

  private static <A extends Annotation, V> Optional<V> get(final Object o,
      final Class<A> annotation, final Function<A, V> mapper) {
    final A a = o.getClass().getAnnotation(annotation);
    if (a != null) {
      return Optional.ofNullable(mapper.apply(a));
    }
    return Optional.empty();
  }

  private @Getter Router router;

  private @Getter final String name = get(this, Name.class, Name::value)//
      .orElse(this.getClass().getSimpleName());
  private @Getter final String version = get(this, Version.class, Version::value).orElse(null);
  private @Getter final String author = get(this, Author.class, Author::value).orElse(null);

  private @Getter Logger logger;
  private @Getter boolean enabled = false;

  public Plugin() {
    String name = this.getName();
    Preconditions.checkArgument(name != null && name.length() > 0,
        "Plugin (" + this.getClass().getName() + " has an empty name.");
  }

  final void init(Router router) {
    Preconditions.checkNotNull(router);
    this.router = router;
    this.logger = new PluginLogger(this);
  }

  protected synchronized final void enable() {
    if (this.enabled) {
      return;
    }
    try {
      router.getLogger().info("Enabling \"" + getName() + "\"...");
      this.onEnable();
      this.enabled = true;
      router.getLogger().info("Plugin \"" + getName() + "\" enabled.");
    } catch (Exception e) {
      router.getLogger().log(Level.SEVERE, "Exception while enabling plugin: " + e.getMessage(), e);
    }
  }

  protected synchronized final void disable() {
    if (!this.enabled) {
      return;
    }
    try {
      router.getLogger().info("Disabling \"" + getName() + "\"...");
      this.enabled = false;
      this.onDisable();
      router.getLogger().info("Plugin \"" + getName() + "\" disabled.");
    } catch (Exception e) {
      router.getLogger().log(Level.SEVERE, "Exception while disabling plugin: " + e.getMessage(),
          e);
    }
  }

  protected abstract void onEnable() throws Exception;

  protected abstract void onDisable() throws Exception;

  protected abstract void onLoad() throws Exception;

}
