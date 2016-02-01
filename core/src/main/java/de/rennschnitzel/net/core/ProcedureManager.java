package de.rennschnitzel.net.core;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;

import de.rennschnitzel.net.ProtocolUtils;
import de.rennschnitzel.net.core.procedure.BoundProcedure;
import de.rennschnitzel.net.core.procedure.CallableProcedure;
import de.rennschnitzel.net.core.procedure.CallableRegisteredProcedure;
import de.rennschnitzel.net.core.procedure.MultiProcedureCall;
import de.rennschnitzel.net.core.procedure.Procedure;
import de.rennschnitzel.net.core.procedure.ProcedureCall;
import de.rennschnitzel.net.core.procedure.ProcedureCallResult;
import de.rennschnitzel.net.core.procedure.SingleProcedureCall;
import de.rennschnitzel.net.exception.ProtocolException;
import de.rennschnitzel.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureCallMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.net.util.TypeUtils;
import de.rennschnitzel.net.util.concurrent.CloseableLock;
import de.rennschnitzel.net.util.concurrent.ReentrantCloseableReadWriteLock;
import io.netty.util.concurrent.Future;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.jodah.typetools.TypeResolver;

@RequiredArgsConstructor
public class ProcedureManager {


  /**
   * Default timeout of procedures. (in milliseconds)
   */
  public static long PROCEDURE_DEFAULT_TIMEOUT = 10 * 1000; // 10 seconds
  public static long MAX_TIMEOUT = 60 * 60 * 1000; // 1 hour

  private final ReentrantCloseableReadWriteLock lock = new ReentrantCloseableReadWriteLock();
  private final Map<Procedure, CallableRegisteredProcedure<?, ?>> registeredProcedures = Maps.newHashMap();

  @NonNull
  @Getter
  private final AbstractNetwork network;

  private final Cache<Integer, ProcedureCall<?, ?>> openCalls = CacheBuilder.newBuilder()//
      .expireAfterWrite(MAX_TIMEOUT, TimeUnit.MILLISECONDS)//
      .removalListener(new RemovalListener<Integer, ProcedureCall<?, ?>>() {
        @Override
        public void onRemoval(RemovalNotification<Integer, ProcedureCall<?, ?>> notify) {
          notify.getValue().checkTimeout();
        }
      })//
      .build();



  @SuppressWarnings("unchecked")
  public <T, R> CallableRegisteredProcedure<T, R> registerProcedure(String name, Function<T, R> function) {
    final Class<?>[] args = TypeUtils.resolveArgumentClass(function);
    return registerProcedure(name, function, (Class<T>) args[0], (Class<R>) args[1]);
  }


  public <T> CallableRegisteredProcedure<T, Void> registerProcedure(final String name, final Consumer<T> consumer) {
    return registerProcedure(name, (t) -> {
      consumer.accept(t);
      return null;
    } , (Class<T>) TypeUtils.resolveArgumentClass(consumer), Void.class);
  }

  public <R> CallableRegisteredProcedure<Void, R> registerProcedure(final String name, final Supplier<R> supplier) {
    return registerProcedure(name, (t) -> supplier.get(), Void.class, (Class<R>) TypeUtils.resolveArgumentClass(supplier));
  }

  public CallableRegisteredProcedure<Void, Void> registerProcedure(final String name, final Runnable run) {
    return registerProcedure(name, (t) -> {
      run.run();
      return null;
    } , Void.class, Void.class);
  }

  public <T, R> CallableRegisteredProcedure<T, R> registerProcedure(final String name, final Function<T, R> function,
      final Class<T> argClass, final Class<R> resultClass) {
    Preconditions.checkNotNull(name);
    Preconditions.checkNotNull(function);
    Preconditions.checkArgument(!name.isEmpty());
    Preconditions.checkNotNull(argClass);
    Preconditions.checkNotNull(resultClass);
    Preconditions.checkArgument(argClass != TypeResolver.Unknown.class);
    Preconditions.checkArgument(resultClass != TypeResolver.Unknown.class);
    final CallableRegisteredProcedure<T, R> proc = new CallableRegisteredProcedure<T, R>(network, name, argClass, resultClass, function);
    proc.setRegisterFuture(_registerProcedure(proc));
    return proc;
  }


  public <T, R> CallableRegisteredProcedure<T, R> registerProcedure(CallableProcedure<T, R> procedure, Function<T, R> function) {
    final CallableRegisteredProcedure<T, R> proc = new CallableRegisteredProcedure<>(network, procedure, function);
    proc.setRegisterFuture(_registerProcedure(proc));
    return proc;
  }

  public <T, R> CallableRegisteredProcedure<T, R> registerProcedure(BoundProcedure<T, R> procedure) {
    final CallableRegisteredProcedure<T, R> proc = new CallableRegisteredProcedure<>(network, procedure, procedure.getFunction());
    proc.setRegisterFuture(_registerProcedure(proc));
    return proc;
  }



  private Future<?> _registerProcedure(CallableRegisteredProcedure<?, ?> proc) {
    try (CloseableLock l = lock.writeLock().open()) {
      registeredProcedures.put(proc, proc);
    }
    network.getHome().addRegisteredProcedure(proc);
    return network.getHome().newUpdatePromise();
  }

  public CallableRegisteredProcedure<?, ?> getRegisteredProcedure(Procedure info) {
    try (CloseableLock l = lock.readLock().open()) {
      return this.registeredProcedures.get(info);
    }
  }

  public <T, R> ProcedureCallResult<T, R> callProcedure(Node node, CallableProcedure<T, R> procedure, T argument) {
    return this.callProcedure(node, procedure, argument, PROCEDURE_DEFAULT_TIMEOUT);
  }

  public <T, R> ProcedureCallResult<T, R> callProcedure(Node node, CallableProcedure<T, R> procedure, T argument, long timeout) {
    Preconditions.checkNotNull(node);
    Preconditions.checkNotNull(procedure);
    Preconditions.checkArgument(timeout > 0);
    final SingleProcedureCall<T, R> call = new SingleProcedureCall<>(node, procedure, argument, timeout);
    if (!call.isDone()) {
      openCalls.put(call.getId(), call);
      try {
        network.sendProcedureCall(call);
      } catch (Exception e) {
        call.setException(e);
      }
    }
    return call.getResult();
  }

  public <T, R> Map<UUID, ? extends ListenableFuture<R>> callProcedure(Collection<Node> nodes, CallableProcedure<T, R> procedure,
      T argument) {
    return this.callProcedure(nodes, procedure, argument, PROCEDURE_DEFAULT_TIMEOUT);
  }

  public <T, R> Map<UUID, ? extends ListenableFuture<R>> callProcedure(Collection<Node> nodes, CallableProcedure<T, R> procedure,
      T argument, long timeout) {
    Preconditions.checkNotNull(nodes);
    Preconditions.checkArgument(!nodes.isEmpty());
    Preconditions.checkNotNull(procedure);
    Preconditions.checkArgument(timeout > 0);
    final MultiProcedureCall<T, R> call = new MultiProcedureCall<>(nodes, procedure, argument, timeout);
    if (!call.isDone()) {
      openCalls.put(call.getId(), call);
      try {
        network.sendProcedureCall(call);
      } catch (Exception e) {
        call.setException(e);
      }
    }
    return call.getResult();
  }

  public <T, R> void handle(final ProcedureCall<T, R> call) {

    try {
      @SuppressWarnings("unchecked")
      CallableRegisteredProcedure<T, R> proc = (CallableRegisteredProcedure<T, R>) this.registeredProcedures.get(call.getProcedure());
      if (proc == null) {
        throw new IllegalStateException("no registered procedure");
      }
      call.execute(proc);
    } catch (Exception e) {
      call.setException(e);
    }

  }

  public void handle(final ProcedureMessage msg) throws ProtocolException {
    switch (msg.getContentCase()) {
      case CALL:
        if (!this.getNetwork().getHome().isPart(msg.getTarget())) {
          sendFail(msg, msg.getCall(), ErrorMessage.newBuilder().setType(ErrorMessage.Type.UNDEFINED).setMessage("wrong target"));
        }
        handle(msg, msg.getCall());
        break;
      case RESPONSE:
        handle(msg, msg.getResponse());
        break;
      default:
        throw new ProtocolException("unknown procedure content!");
    }
  }


  private void handle(final ProcedureMessage msg, final ProcedureResponseMessage response) {
    ProcedureCall<?, ?> call = this.openCalls.getIfPresent(response.getId());
    if (call != null) {
      this.getNetwork().getExecutor().execute(() -> {
        call.receive(msg, response);
      });
    }
  }

  private ProcedureResponseMessage.Builder newResponse(final ProcedureCallMessage call) {
    ProcedureResponseMessage.Builder b = ProcedureResponseMessage.newBuilder();
    b.setProcedure(call.getProcedure());
    b.setId(call.getId());
    b.setTimestamp(call.getTimestamp());
    return b;
  }

  private void sendFail(final ProcedureMessage msg, final ProcedureCallMessage call, final ErrorMessage.Builder error)
      throws ProtocolException {
    ProcedureResponseMessage.Builder b = newResponse(call);
    b.setSuccess(false);
    b.setCancelled(false);
    b.setError(error);
    network.sendProcedureResponse(ProtocolUtils.convert(msg.getSender()), b.build());
  }

  private void handle(final ProcedureMessage msg, final ProcedureCallMessage call) throws ProtocolException {

    final Procedure key = new Procedure(call.getProcedure());
    final CallableRegisteredProcedure<?, ?> proc = this.registeredProcedures.get(key);
    if (proc == null) {
      sendFail(msg, call, ErrorMessage.newBuilder().setType(ErrorMessage.Type.UNDEFINED).setMessage("unregistered procedure"));
    }

    this.getNetwork().getExecutor().execute(() -> {
      try {
        ProcedureResponseMessage.Builder b = newResponse(call);
        proc.remoteCalled(call, b);
        b.setSuccess(true);
        b.setCancelled(false);
        network.sendProcedureResponse(ProtocolUtils.convert(msg.getSender()), b.build());
      } catch (final Exception e) {
        network.getLogger().log(Level.SEVERE, "Procedure handling failed\n" + e.getMessage(), e);
        try {
          sendFail(msg, call, ErrorMessage.newBuilder().setType(ErrorMessage.Type.UNDEFINED)
              .setMessage("exception in procedure call + (" + e.getMessage() + ")"));
        } catch (Exception e1) {
          network.getLogger().log(Level.SEVERE, e1.getMessage(), e1);
        }
      }
    });

  }



}
