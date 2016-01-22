package de.rennschnitzel.net.core.procedure;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;

import de.rennschnitzel.net.core.AbstractNetwork;
import de.rennschnitzel.net.protocol.NetworkProtocol.ProcedureDescription;
import de.rennschnitzel.net.util.LazyCache;
import de.rennschnitzel.net.util.TypeUtils;
import lombok.Getter;

public class Procedure implements Comparable<Procedure> {

  private static final Object LOCK = new Object();

  @SuppressWarnings("unchecked")
  public static <T, R> Procedure of(String name, Function<T, R> function) {
    Class<?>[] typeArgs = TypeUtils.resolveArgumentClass(function);
    return new Procedure(name, (Class<T>) typeArgs[0], (Class<R>) typeArgs[1]);
  }

  public static Procedure of(String name, Consumer<?> function) {
    return new Procedure(name, TypeUtils.resolveArgumentClass(function), Void.class);
  }


  public static Procedure of(String name, Supplier<?> function) {
    return new Procedure(name, Void.class, TypeUtils.resolveArgumentClass(function));
  }


  public static Procedure of(String name, Runnable function) {
    return new Procedure(name, Void.class, Void.class);
  }



  public static Procedure of(String name, final Class<?> argument, final Class<?> result) {
    return new Procedure(name, argument, result);
  }

  @Getter
  private final String name, argumentType, resultType;

  private final LazyCache<Class<?>> argumentClass = LazyCache.of(() -> {
    synchronized (LOCK) {
      return Class.forName(this.argumentType);
    }
  });

  private final LazyCache<Class<?>> resultClass = LazyCache.of(() -> {
    synchronized (LOCK) {
      return Class.forName(this.resultType);
    }
  });

  public Procedure(final ProcedureDescription msg) throws IllegalArgumentException, NullPointerException {
    this(msg.getName(), msg.getArgumentType(), msg.getResultType());
  }

  /*
   * public ProcedureInformation(String name, Function<?, ?> function) { this(name,
   * TypeUtils.resolveArgumentClass(function)); }
   * 
   * private ProcedureInformation(String name, Class<?>[] typeArgs) { this(name, typeArgs[0],
   * typeArgs[1]); }
   */

  public Procedure(final String name, final Class<?> argument, final Class<?> result) {
    this(name, argument.getName(), result.getName());
    this.argumentClass.set(argument);
    this.resultClass.set(result);
  }

  public Procedure(final String name, final String argumentType, final String resultType)
      throws IllegalArgumentException, NullPointerException {
    Preconditions.checkArgument(!name.isEmpty());
    Preconditions.checkArgument(!argumentType.isEmpty());
    Preconditions.checkArgument(!resultType.isEmpty());
    this.name = name.toLowerCase();
    this.argumentType = argumentType;
    this.resultType = resultType;
  }


  public <T, R> CallableProcedure<T, R> get(Function<T, R> function) throws IllegalArgumentException {
    return get(AbstractNetwork.getInstance(), function);
  }


  public <T> CallableProcedure<T, Void> get(Consumer<T> consumer) throws IllegalArgumentException {
    return get(AbstractNetwork.getInstance(), consumer);
  }

  public <R> CallableProcedure<Void, R> get(Supplier<R> supplier) throws IllegalArgumentException {
    return get(AbstractNetwork.getInstance(), supplier);
  }

  public CallableProcedure<Void, Void> get(Runnable runnable) throws IllegalArgumentException {
    return get(AbstractNetwork.getInstance(), runnable);
  }

  public <T, R> CallableProcedure<T, R> get(final Class<T> argument, final Class<R> result) throws IllegalArgumentException {
    return get(AbstractNetwork.getInstance(), argument, result);
  }

  @SuppressWarnings("unchecked")
  public <T, R> CallableProcedure<T, R> get(AbstractNetwork network, Function<T, R> function) throws IllegalArgumentException {
    Class<?>[] typeArgs = TypeUtils.resolveArgumentClass(function);
    return get(network, (Class<T>) typeArgs[0], (Class<R>) typeArgs[1]);
  }

  @SuppressWarnings("unchecked")
  public <T> CallableProcedure<T, Void> get(AbstractNetwork network, Consumer<T> consumer) throws IllegalArgumentException {
    return get(network, (Class<T>) TypeUtils.resolveArgumentClass(consumer), Void.class);
  }

  @SuppressWarnings("unchecked")
  public <R> CallableProcedure<Void, R> get(AbstractNetwork network, Supplier<R> supplier) throws IllegalArgumentException {
    return get(network, Void.class, (Class<R>) TypeUtils.resolveArgumentClass(supplier));
  }

  public CallableProcedure<Void, Void> get(AbstractNetwork network, Runnable runnable) throws IllegalArgumentException {
    return get(network, Void.class, Void.class);
  }

  public <T, R> CallableProcedure<T, R> get(AbstractNetwork network, final Class<T> argumentType, final Class<R> resultType)
      throws IllegalArgumentException {
    checkApplicable(argumentType, resultType);
    return new CallableBaseProcedure<>(network, this, argumentType, resultType);
  }

  private void checkApplicable(final Class<?> argumentType, final Class<?> resultType) throws IllegalArgumentException {
    Preconditions.checkArgument(getArgumentClass().isAssignableFrom(argumentType));
    Preconditions.checkArgument(getResultClass().isAssignableFrom(resultType));
  }

  public boolean isApplicable(final Class<?> argumentType, final Class<?> resultType) throws RuntimeException {
    boolean result = true;
    result &= getArgumentClass().isAssignableFrom(argumentType);
    result &= getResultClass().isAssignableFrom(resultType);
    return result;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, argumentType, resultType);
  }

  public Class<?> getArgumentClass() throws RuntimeException {
    try {
      return this.argumentClass.get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Class<?> getResultClass() throws RuntimeException {
    try {
      return this.resultClass.get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this)
      return true;
    if (!(o instanceof Procedure))
      return false;
    final Procedure oi = (Procedure) o;
    if (!name.equals(oi.name) || !argumentType.equals(oi.argumentType) || !resultType.equals(oi.resultType))
      return false;
    return true;
  }

  @Override
  public int compareTo(Procedure o) {
    if (this == o) {
      return 0;
    }
    int n = String.CASE_INSENSITIVE_ORDER.compare(name, o.name);
    if (n != 0) {
      return n;
    }
    int arg = String.CASE_INSENSITIVE_ORDER.compare(argumentType, o.argumentType);
    if (arg != 0) {
      return arg;
    }
    return String.CASE_INSENSITIVE_ORDER.compare(resultType, o.resultType);
  }

  public ProcedureDescription toProtocol() {
    ProcedureDescription.Builder b = ProcedureDescription.newBuilder();
    b.setName(this.name);
    b.setArgumentType(this.argumentType);
    b.setResultType(this.resultType);
    return b.build();
  }

}
