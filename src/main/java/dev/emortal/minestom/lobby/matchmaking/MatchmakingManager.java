package dev.emortal.minestom.lobby.matchmaking;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Any;
import com.google.protobuf.StringValue;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.minestom.core.Environment;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import net.minestom.server.entity.Player;
import openmatch.Frontend;
import openmatch.FrontendServiceGrpc;
import openmatch.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

public class MatchmakingManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MatchmakingManager.class);

    private final @NotNull Map<UUID, String> playerTickets = new ConcurrentHashMap<>();

    private final @Nullable FrontendServiceGrpc.FrontendServiceFutureStub openMatchClient = this.createService();

    public void queuePlayer(Player player, String game, Runnable failureRunnable) {
        if (this.openMatchClient == null) {
            LOGGER.warn("Not queueing player {} as the matchmaker is not available", player.getUuid());
            return;
        }
        Any playerIdAny = Any.pack(StringValue.of(player.getUuid().toString()));

        ListenableFuture<Messages.Ticket> responseFuture = this.openMatchClient.createTicket(Frontend.CreateTicketRequest.newBuilder()
                .setTicket(
                        Messages.Ticket.newBuilder()
                                .setSearchFields(
                                        Messages.SearchFields.newBuilder()
                                                .addTags("game." + game).build()
                                )
                                .putPersistentField("playerId", playerIdAny).build()
                ).build());

        Futures.addCallback(responseFuture, FunctionalFutureCallback.create(
                ticket -> this.playerTickets.put(player.getUuid(), ticket.getId()),
                throwable -> {
                    LOGGER.error("Unable to queue {}: {}", player.getUuid(), throwable);
                    failureRunnable.run();
                }
        ), ForkJoinPool.commonPool());
    }

    private @Nullable FrontendServiceGrpc.FrontendServiceFutureStub createService() {
        if (!Environment.isProduction()) {
            LOGGER.info("Matchmaking not enabled as server is running in development.");
            return null;
        }

        ManagedChannel frontendChannel = ManagedChannelBuilder.forAddress("open-match-frontend.open-match.svc.cluster.local", 50504)
                .defaultLoadBalancingPolicy("round_robin")
                .usePlaintext()
                .build();

        return FrontendServiceGrpc.newFutureStub(frontendChannel);
    }
}
