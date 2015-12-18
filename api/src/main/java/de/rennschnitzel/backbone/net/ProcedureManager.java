package de.rennschnitzel.backbone.net;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.util.concurrent.ListenableFuture;

import de.rennschnitzel.backbone.net.procedure.MultiProcedureCall;
import de.rennschnitzel.backbone.net.procedure.Procedure;
import de.rennschnitzel.backbone.net.procedure.ProcedureCall;
import de.rennschnitzel.backbone.net.procedure.ProcedureCallResult;
import de.rennschnitzel.backbone.net.procedure.ProcedureInformation;
import de.rennschnitzel.backbone.net.procedure.RegisteredProcedure;
import de.rennschnitzel.backbone.net.procedure.SingleProcedureCall;
import de.rennschnitzel.backbone.util.concurrent.CloseableLock;
import de.rennschnitzel.backbone.util.concurrent.CloseableReentrantReadWriteLock;

public class ProcedureManager {

  /**
   * Default timeout of procedures. (in seconds)
   */
  public static long PROCEDURE_DEFAULT_TIMEOUT = 60 * 60;

  private final CloseableReentrantReadWriteLock lock = new CloseableReentrantReadWriteLock();
  private final BiMap<ProcedureInformation, RegisteredProcedure<?, ?>> registeredProcedures = HashBiMap.create();


  private final Cache<Integer, ProcedureCall<?, ?>> openCalls = CacheBuilder.newBuilder()//
      .expireAfterWrite(1, TimeUnit.HOURS)//
      .removalListener(new RemovalListener<Integer, ProcedureCall<?, ?>>() {
        @Override
        public void onRemoval(RemovalNotification<Integer, ProcedureCall<?, ?>> notify) {
          notify.getValue().checkTimeout();
        }
      })//
      .build();

  public ProcedureManager() {
    // TODO Auto-generated constructor stub
  }


  public <T, R> void registerProcedure(String name, Function<T, R> function) {
    Preconditions.checkNotNull(name);
    Preconditions.checkNotNull(function);
    Preconditions.checkArgument(!name.isEmpty());
    RegisteredProcedure<T, R> proc = new RegisteredProcedure<>(name, function);
    try (CloseableLock l = lock.writeLock().open()) {
      registeredProcedures.put(proc.getInfo(), proc);
    }
  }

  public RegisteredProcedure<?, ?> getRegisteredProcedure(ProcedureInformation info) {
    try (CloseableLock l = lock.readLock().open()) {
      return this.registeredProcedures.get(info);
    }
  }

  public <T, R> ProcedureCallResult<T, R> callProcedure(NetworkMember server, Procedure<T, R> procedure, T argument) {
    return this.callProcedure(server, procedure, argument, PROCEDURE_DEFAULT_TIMEOUT);
  }

  public <T, R> ProcedureCallResult<T, R> callProcedure(NetworkMember server, Procedure<T, R> procedure, T argument, long timeout) {
    Preconditions.checkNotNull(server);
    Preconditions.checkNotNull(procedure);
    Preconditions.checkArgument(timeout > 0);
    final SingleProcedureCall<T, R> call = new SingleProcedureCall<>(server, procedure, argument, timeout);
    try (CloseableLock l = lock.readLock().open()) {
      if (!call.isDone()) {
        openCalls.put(call.getId(), call);
        Network.getInstance().sendCall(call);
      }
    } catch (Exception e) {
      call.setException(e);
    }
    return call.getResult();
  }

  public <T, R> Map<UUID, ? extends ListenableFuture<R>> callProcedure(Collection<NetworkMember> servers, Procedure<T, R> procedure,
      T argument) {
    return this.callProcedure(servers, procedure, argument, PROCEDURE_DEFAULT_TIMEOUT);
  }

  public <T, R> Map<UUID, ? extends ListenableFuture<R>> callProcedure(Collection<NetworkMember> servers, Procedure<T, R> procedure,
      T argument, long timeout) {
    Preconditions.checkNotNull(servers);
    Preconditions.checkArgument(!servers.isEmpty());
    Preconditions.checkNotNull(procedure);
    Preconditions.checkArgument(timeout > 0);
    final MultiProcedureCall<T, R> call = new MultiProcedureCall<>(servers, procedure, argument, timeout);
    try (CloseableLock l = lock.readLock().open()) {
      if (!call.isDone()) {
        openCalls.put(call.getId(), call);
        Network.getInstance().sendCall(call);
      }
    } catch (Exception e) {
      call.setException(e);
    }
    return call.getResult();
  }


}
