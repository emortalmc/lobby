package dev.emortal.minestom.lobby.npc;

import com.google.common.util.concurrent.Futures;
import dev.emortal.api.model.ServerType;
import dev.emortal.api.service.PlayerTrackerProto;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.api.utils.callback.FunctionalFutureCallback;
import dev.emortal.minestom.lobby.entity.MultilineHologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemHideFlag;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class GameModeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameModeManager.class);

    private static final Set<ServerType> SERVER_TYPES = Arrays.stream(ServerType.values())
            .filter(serverType -> serverType != ServerType.UNRECOGNIZED)
            .collect(Collectors.toUnmodifiableSet());

    private final @NotNull Inventory compassInventory;

    private final @NotNull ConcurrentHashMap<String, Integer> itemMap = new ConcurrentHashMap<>();
    private final @NotNull ConcurrentHashMap<String, PacketNPC> npcMap = new ConcurrentHashMap<>();

    public GameModeManager() {
        MinecraftServer.getTeamManager().createBuilder("npcTeam")
                .nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER)
                .updateTeamPacket()
                .build();

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
                        this.refreshPlayerCount(entry.getKey(), entry.getValue());
                    }
                }, throwable -> LOGGER.error("Failed to get player counts", throwable)), ForkJoinPool.commonPool());
            }).repeat(TaskSchedule.seconds(5)).schedule();
        });
    }

    public void createGame(@NotNull ServerType serverType, @NotNull Component name, @NotNull List<Component> description,
                           int slot, @NotNull Material material,
                           @NotNull PlayerSkin skin, @NotNull Pos position, @NotNull MultilineHologram hologram,
                           boolean itemVisible, boolean npcVisible) {
        if (itemVisible) this.createItem(serverType, name, description, slot, material);
        if (npcVisible) this.npcMap.put(serverType.name(), new PacketNPC(position, name, skin, hologram));
    }

    private void createItem(@NotNull ServerType serverType, @NotNull Component name, @NotNull List<Component> description, int slot, @NotNull Material material) {
        ItemStack item = ItemStack.builder(material)
                .displayName(name.decoration(TextDecoration.ITALIC, false))
                .lore(description)
                .meta(meta -> {
                    meta.hideFlag(ItemHideFlag.HIDE_ATTRIBUTES);
                })
                .build();

        this.itemMap.put(serverType.name(), slot);

        this.compassInventory.setItemStack(slot, item);
    }

    private void refreshPlayerCount(String serverType, int newPlayerCount) {
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

            this.compassInventory.setItemStack(slot, item.withLore(newLore));
        }
    }

    public @NotNull Collection<PacketNPC> getNpcs() {
        return this.npcMap.values();
    }

    public @NotNull Inventory getCompassInventory() {
        return this.compassInventory;
    }
}
