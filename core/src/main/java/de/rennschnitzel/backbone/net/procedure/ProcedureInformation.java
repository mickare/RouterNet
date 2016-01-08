package de.rennschnitzel.backbone.net.procedure;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.net.Network;
import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ProcedureDescription;
import de.rennschnitzel.backbone.util.LazyCache;
import de.rennschnitzel.backbone.util.TypeUtils;
import lombok.Getter;

public class ProcedureInformation implements Comparable<ProcedureInformation> {

  private static final Object LOCK = new Object();

  @SuppressWarnings("unchecked")
  public static <T, R> ProcedureInformation of(String name, Function<T, R> function) {
    Class<?>[] typeArgs = TypeUtils.resolveArgumentClass(function);
    return new ProcedureInformation(name, (Class<T>) typeArgs[0], (Class<R>) typeArgs[1]);
  }

  public static ProcedureInformation of(String name, Consumer<?> function) {
    return new ProcedureInformation(name, TypeUtils.resolveArgumentClass(function), Void.class);
  }


  public static ProcedureInformation of(String name, Supplier<?> function) {
    return new ProcedureInformation(name, Void.class, TypeUtils.resolveArgumentClass(function));
  }


  public static ProcedureInformation of(String name, Runnable function) {
    return new ProcedureInformation(name, Void.class, Void.class);
  }



  public static ProcedureInformation of(String name, final Class<?> argument, final Class<?> result) {
    return new ProcedureInformation(name, argument, result);
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

  public ProcedureInformation(final ProcedureDescription msg) throws IllegalArgumentException, NullPointerException {
    this(msg.getName(), msg.getArgument(), msg.getResult());
  }

  /*
   * public ProcedureInformation(String name, Function<?, ?> function) { this(name,
   * TypeUtils.resolveArgumentClass(function)); }
   * 
   * private ProcedureInformation(String name, Class<?>[] typeArgs) { this(name, typeArgs[0],
   * typeArgs[1]); }
   */

  public ProcedureInformation(final String name, final Class<?> argument, final Class<?> result) {
    this(name, argument.getName(), result.getName());
    this.argumentClass.set(argument);
    this.resultClass.set(result);
  }

  public ProcedureInformation(final String name, final String argumentType, final String resultType)
      throws IllegalArgumentException, NullPointerException {
    Preconditions.checkArgument(!name.isEmpty());
    Preconditions.checkArgument(!argumentType.isEmpty());
    Preconditions.checkArgument(!resultType.isEmpty());
    this.name = name.toLowerCase();
    this.argumentType = argumentType;
    this.resultType = resultType;
  }


  public <T, R> Procedure<T, R> getProcedure(Function<T, R> function) throws RuntimeException {
    return getProcedure(Network.getInstance(), function);
  }

  public <T, R> Procedure<T, R> getProcedure(final Class<T> argument, final Class<R> result) throws RuntimeException {
    return getProcedure(Network.getInstance(), argument, result);
  }

  @SuppressWarnings("unchecked")
  public <T, R> Procedure<T, R> getProcedure(Network network, Function<T, R> function) throws RuntimeException {
    Class<?>[] typeArgs = TypeUtils.resolveArgumentClass(function);
    return getProcedure(network, (Class<T>) typeArgs[0], (Class<R>) typeArgs[1]);
  }

  @SuppressWarnings("unchecked")
  public <T> Procedure<T, Void> getProcedure(Network network, Consumer<T> function) throws RuntimeException {
    return getProcedure(network, (Class<T>) TypeUtils.resolveArgumentClass(function), Void.class);
  }

  @SuppressWarnings("unchecked")
  public <R> Procedure<Void, R> getProcedure(Network network, Supplier<R> function) throws RuntimeException {
    return getProcedure(network, Void.class, (Class<R>) TypeUtils.resolveArgumentClass(function));
  }

  public Procedure<Void, Void> getProcedure(Network network, Runnable function) throws RuntimeException {
    return getProcedure(network, Void.class, Void.class);
  }

  public <T, R> Procedure<T, R> getProcedure(Network network, final Class<T> argumentType, final Class<R> resultType)
      throws RuntimeException {
    checkApplicable(argumentType, resultType);
    return new BaseProcedure<>(network, this, argumentType, resultType);
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
    if (!(o instanceof ProcedureInformation))
      return false;
    final ProcedureInformation oi = (ProcedureInformation) o;
    if (!name.equals(oi.name) || !argumentType.equals(oi.argumentType) || !resultType.equals(oi.resultType))
      return false;
    return true;
  }

  @Override
  public int compareTo(ProcedureInformation o) {
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
    b.setArgument(this.argumentType);
    b.setResult(this.resultType);
    return b.build();
  }

}
