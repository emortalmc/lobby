package dev.emortal.minestom.lobby.matchmaking;

import com.google.protobuf.Empty;
import dev.emortal.api.service.gameserver.GameServerMatchmakingGrpc;
import dev.emortal.api.service.gameserver.GameServerMatchmakingProto;
import dev.emortal.api.utils.GrpcTimestampConverter;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MatchmakingServer extends GameServerMatchmakingGrpc.GameServerMatchmakingImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(MatchmakingServer.class);
    private static final int GRPC_PORT = 9090;

    private final Server server;

    public MatchmakingServer() {
        this.server = ServerBuilder.forPort(GRPC_PORT)
                .addService(this)
                .build();
        try {
            this.server.start();
        } catch (IOException ex) {
            LOGGER.error("Error starting matchmaking server: ", ex);
        }
    }

    public void stop() {
        this.server.shutdownNow();
        try {
            this.server.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            LOGGER.error("Error awaiting termination for gRPC server: ", ex);
        }
    }

    @Override
    public void matchFound(GameServerMatchmakingProto.MatchFoundRequest request, StreamObserver<Empty> responseObserver) {
        UUID playerId = UUID.fromString(request.getPlayerId());
        Instant teleportTime = GrpcTimestampConverter.reverse(request.getTeleportTime());
        int playerCount = request.getPlayerCount();

        Player player = MinecraftServer.getConnectionManager().getPlayer(playerId);
        if (player == null) {
            LOGGER.warn("Could not find a player on this server to notify for matchFound (id: {})", playerId);
            return;
        }
        MatchFoundEvent event = new MatchFoundEvent(player, teleportTime, playerCount);
        MinecraftServer.getGlobalEventHandler().call(event);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void matchCancelled(GameServerMatchmakingProto.MatchCancelledRequest request, StreamObserver<Empty> responseObserver) {
        UUID playerId = UUID.fromString(request.getPlayerId());
        Player player = MinecraftServer.getConnectionManager().getPlayer(playerId);
        if (player == null) {
            LOGGER.warn("Could not find a player on this server to cancel a countdown for (id: {})", playerId);
            return;
        }
        MatchCancelledEvent event = new MatchCancelledEvent(player);
        MinecraftServer.getGlobalEventHandler().call(event);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    public record MatchFoundEvent(@NotNull Player player, @NotNull Instant teleportTime, int playerCount) implements PlayerEvent {

        @Override
        public @NotNull Player getPlayer() {
            return this.player();
        }
    }

    public record MatchCancelledEvent(@NotNull Player player) implements PlayerEvent {

        @Override
        public @NotNull Player getPlayer() {
            return this.player();
        }
    }
}
