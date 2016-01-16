package de.rennschnitzel.backbone.net;

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
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.util.concurrent.ListenableFuture;

import de.rennschnitzel.backbone.exception.ProtocolException;
import de.rennschnitzel.backbone.net.procedure.MultiProcedureCall;
import de.rennschnitzel.backbone.net.procedure.Procedure;
import de.rennschnitzel.backbone.net.procedure.ProcedureCall;
import de.rennschnitzel.backbone.net.procedure.ProcedureCallResult;
import de.rennschnitzel.backbone.net.procedure.ProcedureInformation;
import de.rennschnitzel.backbone.net.procedure.RegisteredProcedure;
import de.rennschnitzel.backbone.net.procedure.SingleProcedureCall;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ErrorMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureCallMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.ProcedureResponseMessage;
import de.rennschnitzel.backbone.util.TypeUtils;
import de.rennschnitzel.backbone.util.concurrent.CloseableLock;
import de.rennschnitzel.backbone.util.concurrent.ReentrantCloseableReadWriteLock;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.jodah.typetools.TypeResolver;

@RequiredArgsConstructor
public class ProcedureManager {


  /**
   * Default timeout of procedures. (in seconds)
   */
  public static long PROCEDURE_DEFAULT_TIMEOUT = 60 * 60;

  private final ReentrantCloseableReadWriteLock lock = new ReentrantCloseableReadWriteLock();
  private final BiMap<ProcedureInformation, RegisteredProcedure<?, ?>> registeredProcedures = HashBiMap.create();

  @NonNull
  @Getter
  private final Network network;

  private final Cache<Integer, ProcedureCall<?, ?>> openCalls = CacheBuilder.newBuilder()//
      .expireAfterWrite(1, TimeUnit.HOURS)//
      .removalListener(new RemovalListener<Integer, ProcedureCall<?, ?>>() {
        @Override
        public void onRemoval(RemovalNotification<Integer, ProcedureCall<?, ?>> notify) {
          notify.getValue().checkTimeout();
        }
      })//
      .build();



  @SuppressWarnings("unchecked")
  public <T, R> RegisteredProcedure<T, R> registerProcedure(String name, Function<T, R> function) {
    final Class<?>[] args = TypeUtils.resolveArgumentClass(function);
    return registerProcedure(name, function, (Class<T>) args[0], (Class<R>) args[1]);
  }


  @SuppressWarnings("unchecked")
  public <T> RegisteredProcedure<T, Void> registerProcedure(final String name, final Consumer<T> consumer) {
    return registerProcedure(name, (t) -> {
      consumer.accept(t);
      return null;
    } , (Class<T>) TypeUtils.resolveArgumentClass(consumer), Void.class);
  }

  @SuppressWarnings("unchecked")
  public <R> RegisteredProcedure<Void, R> registerProcedure(final String name, final Supplier<R> supplier) {
    return registerProcedure(name, (t) -> supplier.get(), Void.class, (Class<R>) TypeUtils.resolveArgumentClass(supplier));
  }

  public RegisteredProcedure<Void, Void> registerProcedure(final String name, final Runnable run) {
    return registerProcedure(name, (t) -> {
      run.run();
      return null;
    } , Void.class, Void.class);
  }

  public <T, R> RegisteredProcedure<T, R> registerProcedure(final String name, final Function<T, R> function, final Class<T> argClass,
      final Class<R> resultClass) {
    Preconditions.checkNotNull(name);
    Preconditions.checkNotNull(function);
    Preconditions.checkArgument(!name.isEmpty());
    Preconditions.checkNotNull(argClass);
    Preconditions.checkNotNull(resultClass);
    Preconditions.checkArgument(argClass != TypeResolver.Unknown.class);
    Preconditions.checkArgument(resultClass != TypeResolver.Unknown.class);
    final RegisteredProcedure<T, R> proc = new RegisteredProcedure<T, R>(network, name, argClass, resultClass, function);
    try (CloseableLock l = lock.writeLock().open()) {
      registeredProcedures.put(proc.getInfo(), proc);
    }
    network.getHome().addRegisteredProcedure(proc);
    return proc;
  }

  public RegisteredProcedure<?, ?> getRegisteredProcedure(ProcedureInformation info) {
    try (CloseableLock l = lock.readLock().open()) {
      return this.registeredProcedures.get(info);
    }
  }

  public <T, R> ProcedureCallResult<T, R> callProcedure(Node node, Procedure<T, R> procedure, T argument) {
    return this.callProcedure(node, procedure, argument, PROCEDURE_DEFAULT_TIMEOUT);
  }

  public <T, R> ProcedureCallResult<T, R> callProcedure(Node node, Procedure<T, R> procedure, T argument, long timeout) {
    Preconditions.checkNotNull(node);
    Preconditions.checkNotNull(procedure);
    Preconditions.checkArgument(timeout > 0);
    final SingleProcedureCall<T, R> call = new SingleProcedureCall<>(node, procedure, argument, timeout);
    try (CloseableLock l = lock.readLock().open()) {
      if (!call.isDone()) {
        openCalls.put(call.getId(), call);
        network.sendProcedureCall(call);
      }
    } catch (Exception e) {
      call.setException(e);
    }
    return call.getResult();
  }

  public <T, R> Map<UUID, ? extends ListenableFuture<R>> callProcedure(Collection<Node> nodes, Procedure<T, R> procedure, T argument) {
    return this.callProcedure(nodes, procedure, argument, PROCEDURE_DEFAULT_TIMEOUT);
  }

  public <T, R> Map<UUID, ? extends ListenableFuture<R>> callProcedure(Collection<Node> nodes, Procedure<T, R> procedure, T argument,
      long timeout) {
    Preconditions.checkNotNull(nodes);
    Preconditions.checkArgument(!nodes.isEmpty());
    Preconditions.checkNotNull(procedure);
    Preconditions.checkArgument(timeout > 0);
    final MultiProcedureCall<T, R> call = new MultiProcedureCall<>(nodes, procedure, argument, timeout);
    try (CloseableLock l = lock.readLock().open()) {
      if (!call.isDone()) {
        openCalls.put(call.getId(), call);
        network.sendProcedureCall(call);
      }
    } catch (Exception e) {
      call.setException(e);
    }
    return call.getResult();
  }


  public void handle(ProcedureMessage msg) throws Exception {
    switch (msg.getContentCase()) {
      case CALL:
        handle(msg, msg.getCall());
        break;
      case RESPONSE:
        handle(msg, msg.getResponse());
        break;
      default:
        throw new ProtocolException("Unknown content!");
    }
  }


  private void handle(ProcedureMessage msg, ProcedureResponseMessage response) {
    ProcedureCall<?, ?> call = this.openCalls.getIfPresent(response.getId());
    if (call != null) {
      call.receive(msg, response);
    }
  }

  private ProcedureResponseMessage.Builder newResponse(ProcedureCallMessage call) {
    ProcedureResponseMessage.Builder b = ProcedureResponseMessage.newBuilder();
    b.setProcedure(call.getProcedure());
    b.setId(call.getId());
    b.setTimestamp(call.getTimestamp());
    return b;
  }

  private void sendFail(ProcedureMessage msg, ProcedureCallMessage call, ErrorMessage.Builder error) {
    ProcedureResponseMessage.Builder b = newResponse(call);
    b.setSuccess(false);
    b.setCancelled(false);
    b.setError(error);
    network.sendProcedureResponse(msg.getSender(), b.build());
  }

  private void handle(ProcedureMessage msg, ProcedureCallMessage call) {
    try {
      ProcedureInformation key = new ProcedureInformation(call.getProcedure());
      RegisteredProcedure<?, ?> proc = this.registeredProcedures.get(key);
      if (proc == null) {
        sendFail(msg, call, ErrorMessage.newBuilder().setType(ErrorMessage.Type.UNDEFINED).setMessage("unregistered procedure"));
      }

      ProcedureResponseMessage.Builder out = newResponse(call);
      proc.call(call, out);
      out.setSuccess(true);
      out.setCancelled(false);
      network.sendProcedureResponse(msg.getSender(), out.build());

    } catch (Exception e) {
      network.getLogger().log(Level.SEVERE, "Procedure handling failed\n" + e.getMessage(), e);
      sendFail(msg, call, ErrorMessage.newBuilder().setType(ErrorMessage.Type.UNDEFINED)
          .setMessage("exception in procedure call + (" + e.getMessage() + ")"));
    }
  }


}
