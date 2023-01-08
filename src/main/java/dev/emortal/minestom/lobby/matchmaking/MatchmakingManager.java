package dev.emortal.minestom.lobby.matchmaking;

import com.google.common.util.concurrent.Futures;
import dev.emortal.api.kurushimi.CreateTicketRequest;
import dev.emortal.api.kurushimi.FrontendGrpc;
import dev.emortal.api.kurushimi.SearchFields;
import dev.emortal.api.kurushimi.Ticket;
import dev.emortal.api.kurushimi.WatchCountdownRequest;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.api.utils.callback.FunctionalStreamObserver;
import dev.emortal.minestom.core.Environment;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import net.minestom.server.entity.Player;
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

    private final @Nullable FrontendGrpc.FrontendFutureStub matchmakingService;
    private final @Nullable FrontendGrpc.FrontendStub matchmakingBlockingService;

    public MatchmakingManager() {
        if (!Environment.isProduction()) {
            LOGGER.info("Matchmaking not enabled as server is running in development.");
        }

        ManagedChannel frontendChannel = ManagedChannelBuilder.forAddress("matchmaker", 9090)
                .defaultLoadBalancingPolicy("round_robin")
                .usePlaintext()
                .build();

        this.matchmakingService = FrontendGrpc.newFutureStub(frontendChannel);
        this.matchmakingBlockingService = FrontendGrpc.newStub(frontendChannel);
    }

    public void queuePlayer(Player player, String game, Runnable failureRunnable) {
        if (this.matchmakingService == null || this.matchmakingBlockingService == null) {
            LOGGER.warn("Not queueing player {} as the matchmaker is not available", player.getUuid());
            return;
        }

        var createTicketFuture = this.matchmakingService.createTicket(CreateTicketRequest.newBuilder()
                .setTicket(
                        Ticket.newBuilder()
                                .setPlayerId(player.getUuid().toString())
                                .setSearchFields(
                                        SearchFields.newBuilder().addTags(game)
                                )
                ).build());

        Futures.addCallback(createTicketFuture, FunctionalFutureCallback.create(
                ticket -> {
                    String ticketId = ticket.getId();
                    MatchmakingSession mmSession = new MatchmakingSession(player, game, ticket);

                    this.matchmakingBlockingService.watchTicketCountdown(WatchCountdownRequest.newBuilder()
                            .setTicketId(ticketId).build(), FunctionalStreamObserver.create(
                            mmSession::updateCountdown,
                            throwable -> LOGGER.error("Failed to watch countdown for ticket {}", ticketId, throwable),
                            () -> LOGGER.info("Countdown for ticket {} finished", ticketId)
                    ));

                    // todo show player in the queue
                },
                throwable -> {
                    LOGGER.error("Failed to queue player {} for game {}", player.getUuid(), game, throwable);
                    failureRunnable.run();
                }
        ), ForkJoinPool.commonPool());
    }
}
