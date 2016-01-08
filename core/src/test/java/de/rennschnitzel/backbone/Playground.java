package de.rennschnitzel.backbone;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.Test;

import net.jodah.typetools.TypeResolver;

public class Playground {


  @Test
  public void play() {
    
    Consumer<Integer> c = (i) -> System.out.println(i);

    Class<?>[] res = TypeResolver.resolveRawArguments(Consumer.class, c.getClass());

    for (int i = 0; i < res.length; ++i) {
      // System.out.println("[" + i + "] " + res[i].getName());
    }

    assertTrue(true);

  }


}
