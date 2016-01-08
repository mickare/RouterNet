package net.jodah.typetools;

import static org.junit.Assert.assertArrayEquals;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.Test;

public class LambdaFunctionTest {

  @Test
  public void testLambdaFunctionVariableInMethod() {

    final AtomicInteger a = new AtomicInteger(0);
    Function<String, Integer> func = (s) -> {
      a.incrementAndGet();
      return s.hashCode();
    };

    assertArrayEquals(new Class<?>[] {String.class, Integer.class}, TypeResolver.resolveRawArguments(Function.class, func.getClass()));

  }
  
  private final AtomicInteger b = new AtomicInteger(0);

  @Test
  public void testLambdaFunctionVariableInObject() {

    Function<String, Integer> func = (s) -> {
      b.incrementAndGet();
      return s.hashCode();
    };

    assertArrayEquals(new Class<?>[] {String.class, Integer.class}, TypeResolver.resolveRawArguments(Function.class, func.getClass()));

  }

}
