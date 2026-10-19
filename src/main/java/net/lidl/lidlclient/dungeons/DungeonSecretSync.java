package net.lidl.lidlclient.dungeons;

import net.minecraft.client.Minecraft;
import java.util.*;
import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;

/** Optional party-chat transport: coalesced reports, at most one every five seconds. */
public final class DungeonSecretSync {
    private record Incoming(DungeonSecretReport report,long time) { }
    private final Map<Integer,DungeonSecretReport> outgoing=new LinkedHashMap<>();
    private final Map<Integer,Incoming> incoming=new LinkedHashMap<>();
    private final Map<Integer,Integer> sent=new HashMap<>();
    private long nextSend=Long.MIN_VALUE;
    public void reset() {outgoing.clear();incoming.clear();sent.clear();nextSend=Long.MIN_VALUE;}
    public void local(DungeonMapScanner scanner,int x,int z) {
        if(!DungeonMap.partySync()||scanner.instance().isEmpty()) return;
        var model=scanner.model();int id=z*GRID+x;
        for(var group:model.groups()) if(group.contains(id)) {
            Tile room=model.representative(group);int found=model.foundSecrets(group);
            if(!room.identified||room.secrets<=0||found<=0) return;
            int anchor=group.stream().mapToInt(Integer::intValue).min().orElse(id);
            if(found<=sent.getOrDefault(anchor,0)) return;
            outgoing.put(anchor,new DungeonSecretReport(scanner.instance(),scanner.floor(),anchor%GRID,anchor/GRID,found,room.secrets,room.name.hashCode()));
            return;
        }
    }
    public void receive(DungeonMapScanner scanner,String line) {
        if(!DungeonMap.partySync()||!scanner.active()) return;
        var message=DungeonSecretReport.parseParty(line);
        if(message==null||!scanner.party().contains(message.sender())||!message.report().sameRun(scanner.instance(),scanner.floor())) return;
        var report=message.report();incoming.put(report.z()*GRID+report.x(),new Incoming(report,System.nanoTime()));
    }
    public void tick(Minecraft client,DungeonMapScanner scanner,RoomCatalog catalog) {
        if(!DungeonMap.partySync()) {reset();return;}
        long now=System.nanoTime();
        var iterator=incoming.values().iterator();
        while(iterator.hasNext()) {
            var pending=iterator.next();var report=pending.report();
            if(now-pending.time()>60_000_000_000L||!report.sameRun(scanner.instance(),scanner.floor())) {iterator.remove();continue;}
            if(report.apply(scanner.model(),catalog)!=DungeonSecretReport.Applied.WAITING) iterator.remove();
        }
        if(client.getConnection()==null||scanner.instance().isEmpty()||scanner.party().names().size()<2||now<nextSend||outgoing.isEmpty()) return;
        var entry=outgoing.entrySet().iterator().next();var report=entry.getValue();outgoing.remove(entry.getKey());
        if(!report.sameRun(scanner.instance(),scanner.floor())) return;
        client.getConnection().sendCommand("pc "+report.encode());
        sent.put(entry.getKey(),report.found());nextSend=now+5_000_000_000L;
    }
}
