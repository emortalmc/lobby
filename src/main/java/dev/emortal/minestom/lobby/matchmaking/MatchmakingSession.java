package dev.emortal.minestom.lobby.matchmaking;

import dev.emortal.api.kurushimi.Ticket;
import dev.emortal.api.kurushimi.WatchCountdownResponse;
import dev.emortal.api.utils.GrpcTimestampConverter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created when a player queues for matchmaking.
 * This stores data suck as the boss bar and tracks teleport time.
 */
public class MatchmakingSession {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final @NotNull List<Character> SPINNER = List.of('▘', '▖');
    private static final @NotNull String IN_QUEUE = "<spinner> <gradient:gold:white:%s>In Queue For <gradient:aqua:blue><bold><game><reset> <spinner>";
    private static final @NotNull String COUNTDOWN = "<gradient:#ff9eed:#ff956b:%s>Teleporting to <gradient:aqua:blue><bold><game></bold></gradient> in <white><time></white> seconds!";
    private static final @NotNull String TELEPORTING = "<green>Teleporting to <gradient:aqua:blue><bold><game></bold></gradient>...";

    private final @NotNull AtomicInteger ticksAlive = new AtomicInteger(0);
    private final @NotNull AtomicBoolean playedTeleportSound = new AtomicBoolean(false);
    private final @NotNull Player player;
    private final @NotNull Ticket ticket;
    private final @NotNull String game;

    private @NotNull State state = State.IN_QUEUE;
    private @NotNull BossBar bossBar;
    private @Nullable Instant teleportTime;
    private @Nullable Task teleportTask; // this doesn't actually teleport the player, it just updates the boss bar and state

    public MatchmakingSession(@NotNull Player player, @NotNull String game, @NotNull Ticket ticket) {
        this.player = player;
        this.ticket = ticket;
        this.game = this.parseGameName(game);

        this.bossBar = BossBar.bossBar(Component.empty(), 0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
        player.showBossBar(this.bossBar);
        player.playSound(Sound.sound(SoundEvent.BLOCK_BEACON_ACTIVATE, Sound.Source.MASTER, 1f, 1f));

        MinecraftServer.getSchedulerManager().buildTask(this::tick).repeat(1, TimeUnit.CLIENT_TICK).schedule();
    }

    public void updateCountdown(WatchCountdownResponse response) {
        if (response.getCancelled()) {
            this.state = State.IN_QUEUE;
            this.teleportTime = null;
            this.playedTeleportSound.set(false);
            return;
        }

        if (this.teleportTime == null && response.hasTeleportTime()) {
            this.teleportTime = GrpcTimestampConverter.reverse(response.getTeleportTime());
            int ticks = (int) (this.teleportTime.toEpochMilli() - System.currentTimeMillis()) / 50;

            if (ticks <= 20) {
                this.state = State.TELEPORTING;
                this.player.playSound(Sound.sound(SoundEvent.BLOCK_BEACON_POWER_SELECT, Sound.Source.MASTER, 1f, 1.5f));
                this.bossBar.name(this.createTeleportingName());
                this.playedTeleportSound.set(true);
            } else {
                this.state = State.COUNTDOWN;
                this.player.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1f, 1.5f));
            }
            return;
        }
    }

    private void tick() {
        int tick = this.ticksAlive.incrementAndGet();

        switch (this.state) {
            case IN_QUEUE -> this.bossBar.name(this.createInQueueName());
            case COUNTDOWN -> {
                if (this.teleportTime.minus(500, ChronoUnit.MILLIS).isBefore(Instant.now())) {
                    this.state = State.TELEPORTING;
                    this.bossBar.name(this.createTeleportingName());
                    this.player.playSound(Sound.sound(SoundEvent.BLOCK_BEACON_POWER_SELECT, Sound.Source.MASTER, 1f, 1.5f));
                    this.playedTeleportSound.set(true);
                    return;
                }
                this.bossBar.name(this.createCountdownName());
            }
            default -> {} // do nothing
        }
    }

    private Component createInQueueName() {
        double gradient = (((this.ticksAlive.get() * 0.05) % 2) - 1.0);
        String spinner = String.valueOf(this.getSpinnerChar());
        String miniValue = IN_QUEUE.formatted(gradient);

        return MINI_MESSAGE.deserialize(miniValue, Placeholder.unparsed("spinner", spinner), Placeholder.unparsed("game", this.game));
    }

    private Component createCountdownName() {
        double gradient = (((this.ticksAlive.get() * 0.05) % 2) - 1.0);
        String miniValue = COUNTDOWN.formatted(gradient);

        int secondsToTeleport = (int) (this.teleportTime.getEpochSecond() - Instant.now().getEpochSecond());
        return MINI_MESSAGE.deserialize(miniValue,
                Placeholder.unparsed("game", this.game),
                Placeholder.unparsed("time", String.valueOf(secondsToTeleport))
        );
    }

    private Component createTeleportingName() {
        return MINI_MESSAGE.deserialize(TELEPORTING, Placeholder.unparsed("game", this.game));
    }

    private char getSpinnerChar() {
        int tick = this.ticksAlive.get();
        return SPINNER.get((int) (Math.floor(tick / 10.0) % SPINNER.size()));
    }

    private String parseGameName(String game) {
        // game.block_sumo
        game = game.split("\\.")[1];
        // block_sumo
        return switch (game) {
            case "block_sumo" -> "Block Sumo";
            case "parkour_tag" -> "Parkour Tag";
            default -> game;
        };
    }

    private enum State {
        IN_QUEUE,
        COUNTDOWN,
        TELEPORTING
    }
}
