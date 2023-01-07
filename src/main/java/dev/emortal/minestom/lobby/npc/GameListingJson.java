package dev.emortal.minestom.lobby.npc;

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

    @Override
    public String toString() {
        return "GameListingJson{" +
                "description=" + Arrays.toString(description) +
                ", itemVisible=" + itemVisible +
                ", item='" + item + '\'' +
                ", slot=" + slot +
                ", npcVisible=" + npcVisible +
                ", npcEntityType='" + npcEntityType + '\'' +
                ", npcTitles=" + Arrays.toString(npcTitles) +
                ", npcSkinValue='" + npcSkinValue + '\'' +
                ", npcSkinSignature='" + npcSkinSignature + '\'' +
                '}';
    }
}
