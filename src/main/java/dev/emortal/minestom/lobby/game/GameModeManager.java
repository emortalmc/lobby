package dev.emortal.minestom.lobby.game;

import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.Futures;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.emortal.api.kurushimi.SearchFields;
import dev.emortal.api.model.ServerType;
import dev.emortal.api.service.PlayerTrackerProto;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.minestom.lobby.game.data.GameListing;
import dev.emortal.minestom.lobby.game.data.PlayerSkinAdapter;
import dev.emortal.minestom.lobby.game.data.SearchFieldsAdapter;
import dev.emortal.minestom.lobby.matchmaking.MatchmakingManager;
import dev.emortal.minestom.lobby.matchmaking.MatchmakingSession;
import dev.emortal.minestom.lobby.util.entity.MultilineHologram;
import dev.emortal.minestom.lobby.util.npc.PacketNPC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemHideFlag;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class GameModeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameModeManager.class);
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(SearchFields.class, new SearchFieldsAdapter())
            .registerTypeAdapter(PlayerSkin.class, new PlayerSkinAdapter())
            .create();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Set<ServerType> SERVER_TYPES = Arrays.stream(ServerType.values())
            .filter(serverType -> serverType != ServerType.UNRECOGNIZED)
            .collect(Collectors.toUnmodifiableSet());

    private static final Tag<Integer> SERVER_TYPE_TAG = Tag.Integer("server_type");

    private final @NotNull Inventory compassInventory;
    private final @NotNull MatchmakingManager matchmakingManager;
    private final @NotNull Instance instance;

    private final @NotNull Map<ServerType, GameListing> gameListings;
    private final @NotNull Map<ServerType, Integer> itemMap = new ConcurrentHashMap<>();
    private final @NotNull Map<ServerType, PacketNPC> npcMap = new ConcurrentHashMap<>();

    public GameModeManager(@NotNull MatchmakingManager matchmakingManager, @NotNull Instance instance) {
        MinecraftServer.getTeamManager().createBuilder("npcTeam")
                .nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER)
                .updateTeamPacket()
                .build();

        this.matchmakingManager = matchmakingManager;
        this.instance = instance;
        this.gameListings = this.parseGameListings();
        this.compassInventory = new Inventory(InventoryType.CHEST_4_ROW, "Join a game!");

        // Player count refresh task
        GrpcStubCollection.getPlayerTrackerService().ifPresent(playerTracker -> {
            MinecraftServer.getSchedulerManager().buildTask(() -> {

                var future = playerTracker.getServerTypesPlayerCount(
                        PlayerTrackerProto.ServerTypesRequest.newBuilder()
                                .addAllServerTypes(SERVER_TYPES)
                                .build()
                );

                Futures.addCallback(future, FunctionalFutureCallback.create(response -> {
                    for (Map.Entry<String, Integer> entry : response.getPlayerCountsMap().entrySet()) {
                        this.refreshPlayerCount(ServerType.valueOf(entry.getKey()), entry.getValue());
                    }
                }, throwable -> LOGGER.error("Failed to get player counts", throwable)), ForkJoinPool.commonPool());
            }).repeat(TaskSchedule.seconds(5)).schedule();
        });

        long npcCount = this.gameListings.values().stream()
                .filter(listing -> listing.skin != null)
                .count();
        int i = 0;

        Pos circleCenter = new Pos(0.5, 69.0, -29.0);
        Pos lookingPos = circleCenter.add(0, 0, 3);

        for (Map.Entry<ServerType, GameListing> entry : this.gameListings.entrySet()) {
            ServerType serverType = entry.getKey();
            GameListing gameListing = entry.getValue();
            Component name = MINI_MESSAGE.deserialize(gameListing.npcTitles[0]);
            List<Component> lore = this.createLore(gameListing);

            double angle = i * (Math.PI / (npcCount - 1));
            double x = Math.cos(angle) * 3.7;
            double y = Math.sin(angle) * 3.7 / 1.5;
            Pos position = circleCenter.add(x, 0, -y).withLookAt(lookingPos);

            if (gameListing.itemVisible)
                this.createItem(serverType, name, lore, gameListing.slot, Material.fromNamespaceId(gameListing.item));
            if (gameListing.skin != null) {
                this.createNpc(serverType, gameListing, position);
                i++;
            }
        }
    }

    private @NotNull Map<ServerType, GameListing> parseGameListings() {
        InputStream is = getClass().getClassLoader().getResourceAsStream("games.json");
        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
        Type type = new TypeToken<HashMap<ServerType, GameListing>>() {
        }.getType();
        Map<ServerType, GameListing> parsed = GSON.fromJson(reader, type);
        if (parsed == null) throw new IllegalStateException("Failed to parse games.json");
        return parsed;
    }

    private List<Component> createLore(GameListing gameListing) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        for (String line : gameListing.description) {
            lore.add(MINI_MESSAGE.deserialize(line).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(
                Component.text()
                        .append(Component.text("● ", NamedTextColor.GREEN))
                        .append(Component.text("0", NamedTextColor.GREEN, TextDecoration.BOLD))
                        .append(Component.text(" playing", NamedTextColor.GREEN))
                        .build()
                        .decoration(TextDecoration.ITALIC, false)
        );

        return Collections.unmodifiableList(lore);
    }

    private void createItem(@NotNull ServerType serverType, @NotNull Component name,
                            @NotNull List<Component> description, int slot, @NotNull Material material) {
        ItemStack item = ItemStack.builder(material)
                .displayName(name.decoration(TextDecoration.ITALIC, false))
                .lore(description)
                .meta(meta -> {
                    meta.hideFlag(ItemHideFlag.HIDE_ATTRIBUTES);
                })
                .set(SERVER_TYPE_TAG, serverType.ordinal())
                .build();

        this.itemMap.put(serverType, slot);
        this.compassInventory.addInventoryCondition(((player, clickedSlot, clickType, inventoryConditionResult) -> {
            if (slot != clickedSlot) return;

            inventoryConditionResult.setCancel(true);
            if (clickType == ClickType.LEFT_CLICK || clickType == ClickType.RIGHT_CLICK) {
                this.handleClick(player, serverType);
                player.closeInventory();
            }
        }));
        this.compassInventory.setItemStack(slot, item);
    }

    private void createNpc(ServerType serverType, GameListing gameListing, Pos pos) {
        List<Component> npcTitles = Arrays.stream(gameListing.npcTitles)
                .map(MINI_MESSAGE::deserialize)
                .collect(Collectors.toList());
        npcTitles.add(Component.text("0 playing", NamedTextColor.GRAY));

        Pos holoPos = pos.add(0, (EntityType.PLAYER.height() + 0.2) / 2.0, 0);
        MultilineHologram hologram = new MultilineHologram(npcTitles, this.instance, holoPos);

        this.npcMap.put(serverType, new PacketNPC(pos, gameListing.skin, hologram, player -> this.handleClick(player, serverType)));
    }

    private void handleClick(Player player, ServerType serverType) {
        MatchmakingSession currentSession = this.matchmakingManager.getCurrentSession(player);
        if (currentSession != null) {
            player.sendMessage(Component.text("You are already in queue for %s!".formatted(currentSession.getGame()), NamedTextColor.RED));
            return;
        }

        // Use acquirable to prevent spam clicking/illegal packet spamming from causing issues
        player.getAcquirable().async(entity ->
                this.matchmakingManager.queuePlayer((Player) entity, this.gameListings.get(serverType).searchFields, () -> player.sendMessage("Failed to queue!")));
    }

    private void refreshPlayerCount(ServerType serverType, int newPlayerCount) {
        PacketNPC npc = this.npcMap.get(serverType);
        if (npc != null) {
            MultilineHologram hologram = npc.getHologram();
            hologram.setLine(hologram.size() - 1, Component.text(newPlayerCount + " playing", NamedTextColor.GRAY));
        }

        Integer slot = this.itemMap.get(serverType);
        if (slot != null) {
            ItemStack item = this.compassInventory.getItemStack(slot);
            List<Component> newLore = new ArrayList<>(item.getLore());
            newLore.set(
                    newLore.size() - 1,
                    Component.text()
                            .append(Component.text("● ", NamedTextColor.GREEN))
                            .append(Component.text(newPlayerCount, NamedTextColor.GREEN, TextDecoration.BOLD))
                            .append(Component.text(" playing", NamedTextColor.GREEN))
                            .build()
                            .decoration(TextDecoration.ITALIC, false)
            );

            this.compassInventory.setItemStack(slot, item.withTag(SERVER_TYPE_TAG, serverType.ordinal()).withLore(newLore));
        }
    }

    public @NotNull Collection<PacketNPC> getNpcs() {
        return this.npcMap.values();
    }

    public @NotNull Inventory getCompassInventory() {
        return this.compassInventory;
    }
}
