package net.lidl.lidlclient.dungeons;

import com.google.gson.*;
import java.net.URI;
import java.util.*;

/** LidlClient-tagged counts carried by the Odin relay, never by Hypixel chat. */
public final class DungeonRelayProtocol {
    public record Cell(int x,int z) { }
    public record Update(String room,int found,String sender,List<Cell> cells) { }
    private DungeonRelayProtocol() { }
    public static URI endpoint(String instance) {
        return instance!=null&&instance.matches("[A-Za-z0-9_]{1,6}")?URI.create("wss://ws.odtheking.com/"+instance):null;
    }
    public static JsonObject count(String instance,String floor,String player,String room,int found,List<Cell> cells) {
        JsonObject json=new JsonObject();json.addProperty("lidlclient",1);json.addProperty("instance",instance);json.addProperty("floor",floor);
        json.addProperty("playerName",player);json.addProperty("roomName",room);json.addProperty("foundSecrets",found);
        JsonArray tiles=new JsonArray();for(Cell cell:cells) {JsonObject tile=new JsonObject();tile.addProperty("x",cell.x());tile.addProperty("z",cell.z());tiles.add(tile);}json.add("tiles",tiles);return json;
    }
    public static JsonObject request(String instance,String floor,String player) {
        JsonObject json=count(instance,floor,player,"",0,List.of());json.addProperty("request",true);return json;
    }
    public static String requestedBy(String text,String instance,String floor,Set<String> members) {
        if(text==null||text.length()>8192) return null;
        try {
            JsonObject json=JsonParser.parseString(text).getAsJsonObject();
            if(!json.has("request")||!json.get("request").getAsBoolean()||integer(json.get("lidlclient"))!=1||!instance.equals(json.get("instance").getAsString())||!floor.equals(json.get("floor").getAsString())) return null;
            String sender=json.get("playerName").getAsString();return members.stream().anyMatch(sender::equalsIgnoreCase)?sender:null;
        } catch(RuntimeException ignored) {return null;}
    }
    private static int integer(JsonElement value) {if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException();return value.getAsBigDecimal().intValueExact();}
    public static Update decode(String text,String instance,String floor,Set<String> members) {
        if(text==null||text.length()>8192) return null;
        try {
            JsonObject json=JsonParser.parseString(text).getAsJsonObject();
            if(integer(json.get("lidlclient"))!=1||!instance.equals(json.get("instance").getAsString())||!floor.equals(json.get("floor").getAsString())) return null;
            String sender=json.get("playerName").getAsString();if(members.stream().noneMatch(sender::equalsIgnoreCase)) return null;
            String room=json.get("roomName").getAsString();int found=integer(json.get("foundSecrets"));
            if(room.isBlank()||room.length()>120||found<0||found>99) return null;
            JsonArray array=json.getAsJsonArray("tiles");if(array.isEmpty()||array.size()>4) return null;
            List<Cell> cells=new ArrayList<>();for(JsonElement entry:array) {
                JsonObject tile=entry.getAsJsonObject();int x=integer(tile.get("x")),z=integer(tile.get("z"));
                Cell cell=new Cell(x,z);if(x<0||z<0||x>5||z>5||cells.contains(cell)) return null;cells.add(cell);
            }
            return new Update(room,found,sender,List.copyOf(cells));
        } catch(RuntimeException ignored) {return null;}
    }
}
