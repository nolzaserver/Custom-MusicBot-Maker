package com.projectdecuple.Core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.projectdecuple.MusicListener;
import net.dv8tion.jda.api.entities.User;

public class DecupleAPI {

    private final String API_URL = "http://www.developerdecuple.kro.kr:7777";

    public DecupleAPI() {

    }

    public String getResult(User user) throws Exception {
        return new GetJSON().getJsonByUrl(API_URL + "/users/" + user.getId());
    }

    public String[] getPlaylistElements(User user) throws Exception {

        String res = getResult(user);

        JsonParser jp = new JsonParser();
        JsonObject userResObject = (JsonObject) jp.parse(res);

        if (userResObject.get("playlists").getClass() == JsonArray.class) {
            return MusicListener.jsonArrayToStrArray(userResObject.get("playlists").getAsJsonArray());
        } else {
            return new String[] {userResObject.get("playlists").getAsJsonPrimitive().getAsString()};
        }

    }

}
