package dev.emortal.minestom.lobby;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import dev.emortal.api.model.ServerType;
import dev.emortal.minestom.core.module.Module;
import dev.emortal.minestom.core.module.ModuleData;
import dev.emortal.minestom.core.module.ModuleEnvironment;
import dev.emortal.minestom.lobby.entity.MultilineHologram;
import dev.emortal.minestom.lobby.npc.GameListingJson;
import dev.emortal.minestom.lobby.npc.GameModeManager;
import dev.emortal.tnt.TNTLoader;
import dev.emortal.tnt.source.FileTNTSource;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerStartSneakingEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@ModuleData(name = "lobby", required = true)
public class LobbyModule extends Module {
    private static final Logger LOGGER = LoggerFactory.getLogger(LobbyModule.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Gson GSON = new Gson();

    private static final Pos SPAWN_POINT = new Pos(0, 65, 0, 180f, 0f);

    protected LobbyModule(@NotNull ModuleEnvironment environment) {
        super(environment);
    }

    @Override
    public boolean onLoad() {
        InstanceManager im = MinecraftServer.getInstanceManager();
        GameModeManager gmm = new GameModeManager();

        InstanceContainer instance = im.createInstanceContainer();
        try {
            instance.setChunkLoader(new TNTLoader(new FileTNTSource(Path.of("lobby.tnt"))));
        } catch (IOException | NBTException e) {
            e.printStackTrace();
        }

        instance.enableAutoChunkLoad(false);
        int chunkRadius = 5;
        for (int x = -chunkRadius; x < chunkRadius; x++) {
            for (int y = -chunkRadius; y < chunkRadius; y++) {
                instance.loadChunk(x, y);
            }
        }

        this.eventNode.addListener(PlayerLoginEvent.class, e -> {
            e.setSpawningInstance(instance);
            e.getPlayer().setRespawnPoint(SPAWN_POINT);
        });

        this.eventNode.addListener(PlayerMoveEvent.class, e -> {
            if (e.getPlayer().getPosition().y() < -10) {
                e.getPlayer().teleport(SPAWN_POINT);
            }
        });

        this.eventNode.addListener(PlayerStartSneakingEvent.class, e -> {
            mockGameJoin(e.getPlayer());
        });

        this.eventNode.addListener(ItemDropEvent.class, e -> e.setCancelled(true));
        this.eventNode.addListener(InventoryPreClickEvent.class, e -> e.setCancelled(true));
        this.eventNode.addListener(PlayerSwapItemEvent.class, e -> e.setCancelled(true));

        createGames(gmm, instance);

        LobbyEvents.registerEvents(this.eventNode, gmm);

        return true;
    }

    @Override
    public void onUnload() {

    }


    private void mockGameJoin(Player player) {

        BossBar bossBar = BossBar.bossBar(Component.empty(), 0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);

        player.showBossBar(bossBar);
        player.playSound(Sound.sound(SoundEvent.BLOCK_BEACON_ACTIVATE, Sound.Source.MASTER, 1f, 1f));

        List<String> spinner = List.of("▘", "▖");


        player.scheduler().submitTask(new Supplier() {
            int i = 0;

            @Override
            public Object get() {
                if (i == 170) {
                    player.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1f, 1.5f));
                    player.scheduler().submitTask(new Supplier<>() {
                        int i = 0;

                        @Override
                        public TaskSchedule get() {
                            if (i == 20*10) {
                                player.playSound(Sound.sound(SoundEvent.BLOCK_BEACON_POWER_SELECT, Sound.Source.MASTER, 1f, 1.5f));

                                bossBar.name(Component.text("Teleporting...", NamedTextColor.GREEN));

                                player.scheduler().buildTask(() -> {
                                    player.hideBossBar(bossBar);
                                }).delay(TaskSchedule.seconds(2)).schedule();

                                return TaskSchedule.stop();
                            }

                            if (i % 20 == 0) {
                                player.playSound(Sound.sound(Key.key("battle.showdown.count" + ((i / 20) % 2 == 0 ? 1 : 2)), Sound.Source.MASTER, 1f, 1f));
                            }

                            bossBar.name(MINI_MESSAGE.deserialize("<gradient:#ff9eed:#ff956b:" + (((i * 0.05) % 2) - 1.0) + ">Teleporting to <gradient:aqua:blue><bold>Block Sumo</bold></gradient> in <white>" + (10 - (i / 20)) + "</white> seconds!"));

                            i++;
                            return TaskSchedule.nextTick();
                        }
                    });

                    return TaskSchedule.stop();
                }

                String spinnerChar = spinner.get((int) (Math.floor(i / 10.0) % spinner.size()));
                bossBar.name(MINI_MESSAGE.deserialize(spinnerChar + " <gradient:gold:white:" + (((i * 0.05) % 2) - 1.0) + ">In queue for <gradient:aqua:blue><bold>Block Sumo<reset> " + spinnerChar));

                if (i % (20 * 10) == 0) {
                    player.playSound(Sound.sound(SoundEvent.BLOCK_BEACON_AMBIENT, Sound.Source.MASTER, 0.75f, 1f));
                }

                i++;

                return TaskSchedule.nextTick();
            }
        });
    }


    private void createGames(GameModeManager gameModeManager, Instance instance) {
        InputStream is = getClass().getClassLoader().getResourceAsStream("games.json");
        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
        Type type = new TypeToken<HashMap<String, GameListingJson>>(){}.getType();
        Map<String, GameListingJson> parsed = GSON.fromJson(reader, type);

        LOGGER.info(parsed.toString());

        Pos circleCenter = new Pos(0.5, 69.0, -29.0);
        Pos lookingPos = circleCenter.add(0, 0, 3);

        long visibleSize = parsed.values().stream().filter(a -> a.npcVisible).count();
        int i = 0;

        for (Map.Entry<String, GameListingJson> entry : parsed.entrySet()) {
            GameListingJson gameListing = entry.getValue();
            String id = entry.getKey();

            double angle = i * (Math.PI / (visibleSize - 1));
            double x = Math.cos(angle) * 3.7;
            double y = Math.sin(angle) * 3.7 / 1.5;
            Pos position = circleCenter.add(x, 0, -y).withLookAt(lookingPos);

            List<Component> lores = new ArrayList<>();
            lores.add(Component.empty());
            for (String line : gameListing.description) {
                lores.add(MINI_MESSAGE.deserialize(line).decoration(TextDecoration.ITALIC, false));
            }
            lores.add(Component.empty());
            lores.add(
                    Component.text()
                            .append(Component.text("● ", NamedTextColor.GREEN))
                            .append(Component.text("0", NamedTextColor.GREEN, TextDecoration.BOLD))
                            .append(Component.text(" playing", NamedTextColor.GREEN))
                            .build()
                            .decoration(TextDecoration.ITALIC, false)
            );

            List<Component> npcTitles = new ArrayList<>();
            for (String line : gameListing.npcTitles) {
                npcTitles.add(MINI_MESSAGE.deserialize(line));
            }
            npcTitles.add(Component.text("0 playing", NamedTextColor.GRAY));

            MultilineHologram hologram = new MultilineHologram(npcTitles);

            gameModeManager.createGame(
                    ServerType.valueOf(id),
                    MINI_MESSAGE.deserialize(gameListing.npcTitles[0]),
                    lores,
                    gameListing.slot,
                    Material.fromNamespaceId(gameListing.item),
                    new PlayerSkin(gameListing.npcSkinValue, gameListing.npcSkinSignature),
                    position,
                    hologram,
                    gameListing.itemVisible,
                    gameListing.npcVisible
            );

            if (gameListing.npcVisible) {
                hologram.setInstance(instance, position.add(0, (EntityType.PLAYER.height() + 0.2) / 2.0, 0));

                i++;
            }
        }



    }
}