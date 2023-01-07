package dev.emortal.minestom.lobby.npc;

import dev.emortal.minestom.lobby.entity.MultilineHologram;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.*;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.packet.server.play.PlayerInfoPacket;
import net.minestom.server.network.packet.server.play.SpawnPlayerPacket;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PacketNPC {
    private final MultilineHologram hologram;

    private final PlayerInfoPacket playerInfoPacket;
    private final SpawnPlayerPacket spawnPlayerPacket;
    private final TeamsPacket teamPacket;
    private final EntityMetaDataPacket entityMetaPacket;
    private final PlayerInfoPacket listRemovePacket;

    public PacketNPC(@NotNull Pos position, @NotNull Component name, @NotNull PlayerSkin skin, @NotNull MultilineHologram hologram) {
        this.hologram = hologram;

        UUID uuid = UUID.randomUUID();
        int entityId = Entity.generateId();

        String npcName = uuid.toString().substring(0, 8);
        System.out.println(npcName);

        List<PlayerInfoPacket.AddPlayer.Property> properties = new ArrayList<>();
        properties.add(
                new PlayerInfoPacket.AddPlayer.Property(
                        "textures",
                        skin.textures(),
                        skin.signature()
                )
        );

        this.playerInfoPacket = new PlayerInfoPacket(PlayerInfoPacket.Action.ADD_PLAYER, new PlayerInfoPacket.AddPlayer(uuid, npcName, properties, GameMode.CREATIVE, 0, Component.empty(), null));
        this.spawnPlayerPacket = new SpawnPlayerPacket(entityId, uuid, position);
        this.teamPacket = new TeamsPacket("npcTeam", new TeamsPacket.AddEntitiesToTeamAction(List.of(npcName)));
        this.entityMetaPacket = new EntityMetaDataPacket(entityId, Map.of(17, Metadata.Byte((byte) 127 /*All layers enabled*/)));
        this.listRemovePacket = new PlayerInfoPacket(PlayerInfoPacket.Action.REMOVE_PLAYER, new PlayerInfoPacket.RemovePlayer(uuid));
    }

    public void addViewer(@NotNull Player player) {
        player.sendPackets(
                this.playerInfoPacket,
                this.spawnPlayerPacket,
                this.teamPacket,
                this.entityMetaPacket
//                this.listRemovePacket
        );
    }

    public MultilineHologram getHologram() {
        return hologram;
    }
}
