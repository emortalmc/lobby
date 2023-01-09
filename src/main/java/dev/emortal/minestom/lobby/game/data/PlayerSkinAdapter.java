package dev.emortal.minestom.lobby.game.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minestom.server.entity.PlayerSkin;

public class PlayerSkinAdapter extends TypeAdapter<PlayerSkin> {

    @Override
    public void write(JsonWriter out, PlayerSkin value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Cannot serialize PlayerSkin");
    }

    @Override
    public PlayerSkin read(JsonReader in) {
        JsonElement jsonElement = JsonParser.parseReader(in);
        JsonObject json = jsonElement.getAsJsonObject();

        String texture = json.get("texture").getAsString();
        String signature = json.get("signature").getAsString();
        return new PlayerSkin(texture, signature);
    }
}
