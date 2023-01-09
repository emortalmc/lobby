package dev.emortal.minestom.lobby;

import dev.emortal.minestom.core.module.Module;
import dev.emortal.minestom.core.module.ModuleData;
import dev.emortal.minestom.core.module.ModuleEnvironment;
import dev.emortal.minestom.lobby.game.GameModeManager;
import dev.emortal.minestom.lobby.matchmaking.MatchmakingManager;
import dev.emortal.tnt.TNTLoader;
import dev.emortal.tnt.source.FileTNTSource;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTException;

import java.io.IOException;
import java.nio.file.Path;

@ModuleData(name = "lobby", required = true)
public class LobbyModule extends Module {
    private static final Pos SPAWN_POINT = new Pos(0, 65, 0, 180f, 0f);

    protected LobbyModule(@NotNull ModuleEnvironment environment) {
        super(environment);
    }

    @Override
    public boolean onLoad() {
        InstanceManager im = MinecraftServer.getInstanceManager();

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

        MatchmakingManager matchmakingManager = new MatchmakingManager();

        GameModeManager gmm = new GameModeManager(matchmakingManager, instance);

        this.eventNode.addListener(PlayerLoginEvent.class, e -> {
            e.setSpawningInstance(instance);
            e.getPlayer().setRespawnPoint(SPAWN_POINT);
        });

        this.eventNode.addListener(PlayerMoveEvent.class, e -> {
            if (e.getPlayer().getPosition().y() < -10) {
                e.getPlayer().teleport(SPAWN_POINT);
            }
        });

        this.eventNode.addListener(ItemDropEvent.class, e -> e.setCancelled(true));
        this.eventNode.addListener(PlayerSwapItemEvent.class, e -> e.setCancelled(true));

        LobbyEvents.registerEvents(this.eventNode, gmm);

        return true;
    }

    @Override
    public void onUnload() {

    }

//    private void createGames(GameModeManager gameModeManager, Instance instance) {
//        InputStream is = getClass().getClassLoader().getResourceAsStream("games.json");
//        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
//        Type type = new TypeToken<HashMap<String, GameListing>>(){}.getType();
//        Map<String, GameListing> parsed = GSON.fromJson(reader, type);
//
//        LOGGER.info(parsed.toString());
//
//        Pos circleCenter = new Pos(0.5, 69.0, -29.0);
//        Pos lookingPos = circleCenter.add(0, 0, 3);
//
//        long visibleSize = parsed.values().stream().filter(a -> a.npcVisible).count();
//        int i = 0;
//
//        for (Map.Entry<String, GameListing> entry : parsed.entrySet()) {
//            GameListing gameListing = entry.getValue();
//            String id = entry.getKey();
//
//            double angle = i * (Math.PI / (visibleSize - 1));
//            double x = Math.cos(angle) * 3.7;
//            double y = Math.sin(angle) * 3.7 / 1.5;
//            Pos position = circleCenter.add(x, 0, -y).withLookAt(lookingPos);
//
//            List<Component> lores = new ArrayList<>();
//            lores.add(Component.empty());
//            for (String line : gameListing.description) {
//                lores.add(MINI_MESSAGE.deserialize(line).decoration(TextDecoration.ITALIC, false));
//            }
//            lores.add(Component.empty());
//            lores.add(
//                    Component.text()
//                            .append(Component.text("● ", NamedTextColor.GREEN))
//                            .append(Component.text("0", NamedTextColor.GREEN, TextDecoration.BOLD))
//                            .append(Component.text(" playing", NamedTextColor.GREEN))
//                            .build()
//                            .decoration(TextDecoration.ITALIC, false)
//            );
//
//            List<Component> npcTitles = new ArrayList<>();
//            for (String line : gameListing.npcTitles) {
//                npcTitles.add(MINI_MESSAGE.deserialize(line));
//            }
//            npcTitles.add(Component.text("0 playing", NamedTextColor.GRAY));
//
//            MultilineHologram hologram = new MultilineHologram(npcTitles);
//
//            gameModeManager.createGame(
//                    ServerType.valueOf(id),
//                    MINI_MESSAGE.deserialize(gameListing.npcTitles[0]),
//                    lores,
//                    gameListing.slot,
//                    Material.fromNamespaceId(gameListing.item),
//                    new PlayerSkin(gameListing.npcSkinValue, gameListing.npcSkinSignature),
//                    position,
//                    hologram,
//                    gameListing.itemVisible,
//                    gameListing.npcVisible
//            );
//
//            if (gameListing.npcVisible) {
//                hologram.setInstance(instance, position.add(0, (EntityType.PLAYER.height() + 0.2) / 2.0, 0));
//
//                i++;
//            }
//        }
//    }
}