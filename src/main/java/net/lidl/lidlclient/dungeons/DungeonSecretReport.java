package net.lidl.lidlclient.dungeons;

import java.util.regex.Pattern;

/** Versioned, run-scoped party-chat reports. Only the current roster is trusted. */
public record DungeonSecretReport(String instance,String floor,int x,int z,int found,int total,int roomHash) {
    public enum Applied { APPLIED, WAITING, INVALID }
    private static final Pattern REPORT=Pattern.compile("^\\[LidlMap] S1 ([A-Za-z0-9_]{1,24}) (E|[FM][1-7]) (\\d{1,2}) (\\d{1,2}) (\\d{1,2})/(\\d{1,2}) (-?\\d{1,11})$");
    private static final Pattern PARTY=Pattern.compile("^Party > (?:\\[[^]]+]\\s*)*([A-Za-z0-9_]{1,16}): (\\[LidlMap].*)$");
    public record Message(String sender,DungeonSecretReport report) { }
    public String encode() {return "[LidlMap] S1 "+instance+" "+floor+" "+x+" "+z+" "+found+"/"+total+" "+roomHash;}
    public static Message parseParty(String line) {
        var party=PARTY.matcher(line.replaceAll("(?i)§[0-9a-fk-or]", "").strip());
        if(!party.matches()) return null;
        var match=REPORT.matcher(party.group(2));
        if(!match.matches()) return null;
        try {
            var report=new DungeonSecretReport(match.group(1),match.group(2),Integer.parseInt(match.group(3)),Integer.parseInt(match.group(4)),
                    Integer.parseInt(match.group(5)),Integer.parseInt(match.group(6)),Integer.parseInt(match.group(7)));
            if(report.x<0||report.z<0||report.x>10||report.z>10||report.x%2!=0||report.z%2!=0||report.total<=0||report.found<0||report.found>report.total) return null;
            return new Message(party.group(1),report);
        } catch(NumberFormatException e) {return null;}
    }
    public boolean sameRun(String server,String currentFloor) {return instance.equals(server)&&floor.equals(currentFloor);}
    public Applied apply(DungeonMapModel model,RoomCatalog catalog) {
        if(x<0||z<0||x>10||z>10||x%2!=0||z%2!=0||found<0||total<=0||found>total) return Applied.INVALID;
        var room=catalog.findNameHash(roomHash);
        if(room==null||room.secrets()!=total) return Applied.INVALID;
        var tile=model.at(x,z);
        if(tile.kind!=DungeonMapModel.Kind.ROOM||!tile.mapKnown) return Applied.WAITING;
        if(tile.identified&&!tile.name.equals(room.name())) return Applied.INVALID;
        model.identify(x,z,room);model.updateSecrets(x,z,found,total);return Applied.APPLIED;
    }
}
