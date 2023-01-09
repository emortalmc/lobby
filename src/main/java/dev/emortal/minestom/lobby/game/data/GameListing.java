package dev.emortal.minestom.lobby.game.data;

import dev.emortal.api.kurushimi.SearchFields;
import net.minestom.server.entity.PlayerSkin;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class GameListing {
    public String[] description;
    public boolean itemVisible;
    public String item;
    public int slot;
    public String npcEntityType;
    public String[] npcTitles;
    public @Nullable PlayerSkin skin;
    public SearchFields searchFields;

    @Override
    public String toString() {
        return "GameListing{" +
                "description=" + Arrays.toString(this.description) +
                ", itemVisible=" + this.itemVisible +
                ", item='" + this.item + '\'' +
                ", slot=" + this.slot +
                ", npcEntityType='" + this.npcEntityType + '\'' +
                ", npcTitles=" + Arrays.toString(this.npcTitles) +
                ", npcSkinValue='" + this.skin + '\'' +
                ", searchFields=" + this.searchFields +
                '}';
    }
}
