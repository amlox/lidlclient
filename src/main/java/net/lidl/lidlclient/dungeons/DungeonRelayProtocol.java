package net.lidl.lidlclient.dungeons;

import com.google.gson.*;
import java.util.*;

/** Independent adapters for the three mods' published JSON wire formats. */
public final class DungeonRelayProtocol {
    public enum Backend { ODIN, NOAMM, SKYBLOCKER }
    public record Cell(int x,int z) { }
    public record Update(String room,int found,String sender,List<Cell> cells) { }
    private DungeonRelayProtocol() { }
    public static JsonObject skyEnvelope(String type,String instance,JsonObject message) {
        JsonObject json=new JsonObject();json.addProperty("type",type);json.addProperty("service","DUNGEON_SECRETS");
        json.addProperty("serverId",instance);if(message!=null) json.add("message",message);return json;
    }
    public static JsonObject count(Backend backend,String instance,String player,UUID uuid,String room,String skyRoom,int found,List<Cell> cells) {
        JsonObject json=new JsonObject();
        switch(backend) {
            case ODIN -> {json.addProperty("playerName",player);json.addProperty("roomName",room);json.add("tiles",tiles(cells,"z",false));json.addProperty("foundSecrets",found);}
            case NOAMM -> {json.addProperty("type","dungeonroomsecrets");json.addProperty("room",room);json.addProperty("secrets",found);}
            case SKYBLOCKER -> {
                if(skyRoom==null) return null;
                json.addProperty("type","room_secret_count");json.addProperty("uuid",uuid.toString());json.addProperty("room",skyRoom);json.addProperty("secretCount",found);
                return skyEnvelope("publish",instance,json);
            }
        }
        return json;
    }
    public static JsonObject noammStart(String instance,String floor,List<String> members,Cell entrance) {
        JsonObject json=new JsonObject();json.addProperty("type","dungeon_start");json.addProperty("serverId",instance);json.addProperty("floor",floor);
        JsonArray team=new JsonArray();members.forEach(team::add);json.add("members",team);
        JsonObject pair=new JsonObject();pair.addProperty("first",entrance.x()*2);pair.addProperty("second",entrance.z()*2);json.add("entrance",pair);return json;
    }
    public static JsonObject noammRoom(String name,Cell cell) {
        JsonObject json=new JsonObject();json.addProperty("type","dungeonroom");json.addProperty("name",name);
        json.addProperty("x",-185+32*cell.x());json.addProperty("z",-185+32*cell.z());
        json.addProperty("col",cell.x()*2);json.addProperty("row",cell.z()*2);json.addProperty("isSeparator",false);return json;
    }
    private static JsonArray tiles(List<Cell> cells,String second,boolean physical) {
        JsonArray list=new JsonArray();for(Cell cell:cells) {JsonObject tile=new JsonObject();tile.addProperty("x",physical?-200+32*cell.x():cell.x());tile.addProperty(second,physical?-200+32*cell.z():cell.z());list.add(tile);}return list;
    }
    private static String string(JsonObject o,String key) {return o.has(key)&&o.get(key).isJsonPrimitive()?o.get(key).getAsString():null;}
    private static int integer(JsonObject o,String key) {
        if(!o.has(key)||!o.get(key).isJsonPrimitive()||!o.get(key).getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException();
        return o.get(key).getAsBigDecimal().intValueExact();
    }
    private static List<Cell> cells(JsonArray array,String second,boolean physical) {
        if(array==null||array.isEmpty()||array.size()>4) throw new IllegalArgumentException();
        List<Cell> result=new ArrayList<>();for(JsonElement entry:array) {
            JsonObject tile=entry.getAsJsonObject();int x=integer(tile,"x"),z=integer(tile,second);
            if(physical) {if(Math.floorMod(x+200,32)!=0||Math.floorMod(z+200,32)!=0) throw new IllegalArgumentException();x=(x+200)/32;z=(z+200)/32;}
            if(x<0||z<0||x>5||z>5) throw new IllegalArgumentException();
            Cell cell=new Cell(x,z);if(result.contains(cell)) throw new IllegalArgumentException();result.add(cell);
        }
        return List.copyOf(result);
    }
    /** Null means an unrelated, malformed, untrusted, or out-of-run message. found=-1 is room metadata. */
    public static Update decode(Backend backend,String text,String instance,Set<String> partyNames,Set<UUID> partyUuids) {
        if(text==null||text.length()>131072) return null;
        try {
            JsonObject json=JsonParser.parseString(text).getAsJsonObject();String room,sender=null;int found=-1;List<Cell> cells=List.of();
            switch(backend) {
                case ODIN -> {
                    sender=string(json,"playerName");if(sender==null||partyNames.stream().noneMatch(sender::equalsIgnoreCase)) return null;
                    room=string(json,"roomName");cells=cells(json.getAsJsonArray("tiles"),"z",false);
                    if(json.has("foundSecrets")&&!json.get("foundSecrets").isJsonNull()) found=integer(json,"foundSecrets");
                }
                case NOAMM -> {
                    String type=string(json,"type");
                    if("dungeonroomsecrets".equals(type)) {room=string(json,"room");found=integer(json,"secrets");}
                    else if("dungeonroom".equals(type)) {
                        room=string(json,"name");if(json.has("isSeparator")&&json.get("isSeparator").getAsBoolean()) return null;
                        int x=integer(json,"col"),z=integer(json,"row");
                        if(x<0||z<0||x>10||z>10||x%2!=0||z%2!=0||integer(json,"x")!=-185+x*16||integer(json,"z")!=-185+z*16) return null;
                        cells=List.of(new Cell(x/2,z/2));
                    } else return null;
                }
                case SKYBLOCKER -> {
                    if(!"response".equals(string(json,"type"))||!"DUNGEON_SECRETS".equals(string(json,"service"))) return null;
                    if(json.has("serverId")&&!instance.equals(string(json,"serverId"))) return null;
                    json=json.getAsJsonObject("message");if(json==null) return null;
                    sender=string(json,"uuid");if(sender==null||!partyUuids.contains(UUID.fromString(sender))) return null;
                    room=string(json,"room");String type=string(json,"type");
                    if("room_secret_count".equals(type)) found=integer(json,"secretCount");
                    else if("room_match".equals(type)) cells=cells(json.getAsJsonArray("pos"),"y",true);
                    else return null;
                }
                default -> {return null;}
            }
            if(room==null||room.isBlank()||room.length()>120||found< -1||found>99) return null;
            return new Update(room,found,sender,cells);
        } catch(RuntimeException ignored) {return null;}
    }
}
