package dev.emortal.minestom.lobby.game.data;

import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.protobuf.util.JsonFormat;
import dev.emortal.api.kurushimi.SearchFields;

import java.io.IOException;

public class SearchFieldsAdapter extends TypeAdapter<SearchFields> {

    @Override
    public void write(JsonWriter out, SearchFields value) throws IOException {
        out.jsonValue(JsonFormat.printer().print(value));
    }

    @Override
    public SearchFields read(JsonReader in) throws IOException {
        SearchFields.Builder builder = SearchFields.newBuilder();
        JsonFormat.parser().merge(JsonParser.parseReader(in).toString(), builder);
        return builder.build();
    }
}
