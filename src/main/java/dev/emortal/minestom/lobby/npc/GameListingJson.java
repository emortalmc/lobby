package dev.emortal.minestom.lobby.npc;

import dev.emortal.minestom.lobby.matchmaking.QueueType;

import java.util.Arrays;

public class GameListingJson {
    public String[] description;
    public boolean itemVisible;
    public String item;
    public int slot;
    public boolean npcVisible;
    public String npcEntityType;
    public String[] npcTitles;
    public String npcSkinValue;
    public String npcSkinSignature;
    public QueueType queueType;

    @Override
    public String toString() {
        return "GameListingJson{" +
                "description=" + Arrays.toString(this.description) +
                ", itemVisible=" + this.itemVisible +
                ", item='" + this.item + '\'' +
                ", slot=" + this.slot +
                ", npcVisible=" + this.npcVisible +
                ", npcEntityType='" + this.npcEntityType + '\'' +
                ", npcTitles=" + Arrays.toString(this.npcTitles) +
                ", npcSkinValue='" + this.npcSkinValue + '\'' +
                ", npcSkinSignature='" + this.npcSkinSignature + '\'' +
                ", queueType=" + this.queueType +
                '}';
    }
}
