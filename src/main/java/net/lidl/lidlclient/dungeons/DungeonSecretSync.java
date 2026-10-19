package net.lidl.lidlclient.dungeons;

import net.minecraft.client.Minecraft;
import java.util.*;
import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;
import static net.lidl.lidlclient.dungeons.DungeonRelayProtocol.*;

/** Lidl-to-Lidl counts on the Odin relay; never sends secret reports to chat. */
public final class DungeonSecretSync {
    private record Local(String room,int found,List<Cell> cells) { }
    private record Pending(Update update,long time) { }
    private final DungeonRelayConnection connection=new DungeonRelayConnection();
    private final Map<String,Integer> sent=new HashMap<>();
    private final Map<String,Local> locals=new LinkedHashMap<>();
    private final Map<String,Pending> pending=new LinkedHashMap<>();
    private final Map<String,Long> lastRequests=new HashMap<>();
    private String scope="";
    private long nextSend;
    public String status() {
        if(!DungeonMap.secretSync()) return "Disabled";
        if(scope.isEmpty()) return "Waiting for dungeon instance";
        return connection.status();
    }
    public void reset() {connection.close();sent.clear();locals.clear();pending.clear();lastRequests.clear();scope="";nextSend=0;}
    public void local(DungeonMapScanner scanner,int x,int z) {
        if(!DungeonMap.secretSync()) return;
        var model=scanner.model();int id=z*GRID+x;
        for(var group:model.groups()) if(group.contains(id)) {
            Tile room=model.representative(group);
            if(!room.identified||room.secrets<=0||group.size()>4) return;
            var cells=group.stream().map(index->new Cell((index%GRID)/2,(index/GRID)/2)).sorted(Comparator.comparingInt(Cell::z).thenComparingInt(Cell::x)).toList();
            String key=room.name+cells;Local previous=locals.get(key);int found=model.foundSecrets(group);
            if(previous==null||found>previous.found()) locals.put(key,new Local(room.name,found,cells));return;
        }
    }
    /** Returns true for applied/invalid updates, false when awaiting server map room geometry. */
    public static boolean apply(Update update,DungeonMapModel model,RoomCatalog catalog) {
        var room=catalog.findName(update.room());if(room==null||update.found()>room.secrets()) return true;
        if(!update.cells().isEmpty()) {
            Set<Cell> cells=new HashSet<>(update.cells());Set<Cell> visited=new HashSet<>();ArrayDeque<Cell> queue=new ArrayDeque<>();queue.add(update.cells().getFirst());
            while(!queue.isEmpty()) {Cell c=queue.remove();if(!visited.add(c)) continue;for(Cell n:cells) if(Math.abs(n.x()-c.x())+Math.abs(n.z()-c.z())==1&&!visited.contains(n)) queue.add(n);}
            if(visited.size()!=cells.size()) return true;
            for(Cell cell:cells) {Tile tile=model.at(cell.x()*2,cell.z()*2);if(tile.kind!=Kind.ROOM||!tile.mapKnown) return false;if(tile.identified&&!tile.name.equals(room.name())) return true;}
            Set<Integer> expected=new HashSet<>();for(Cell cell:cells) expected.add(cell.z()*2*GRID+cell.x()*2);
            var geometry=model.groups().stream().filter(g->g.contains(expected.iterator().next())).findFirst().orElse(List.of());
            if(!expected.equals(new HashSet<>(geometry))) return false;
            for(Cell cell:cells) model.identify(cell.x()*2,cell.z()*2,room);
            if(update.found()>=0) for(Cell cell:cells) model.updateSecrets(cell.x()*2,cell.z()*2,update.found(),room.secrets());
            return true;
        }
        var matches=model.groups().stream().filter(group->{Tile tile=model.representative(group);return tile.identified&&tile.name.equals(room.name());}).toList();
        if(matches.isEmpty()) return false;
        if(matches.size()!=1) return true;
        int id=matches.getFirst().getFirst();if(update.found()>=0) model.updateSecrets(id%GRID,id/GRID,update.found(),room.secrets());return true;
    }

    public void tick(Minecraft client,DungeonMapScanner scanner,RoomCatalog catalog) {
        if(!DungeonMap.secretSync()||!scanner.active()) {if(!scope.isEmpty()) reset();return;}
        var endpoint=endpoint(scanner.instance());
        if(endpoint==null||scanner.instance().isEmpty()||client.player==null||client.getConnection()==null) return;
        Set<String> members=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);members.addAll(scanner.party().names());members.add(client.player.getName().getString());
        if(members.size()>5) return;
        String current=endpoint+"/"+scanner.instance()+"/"+scanner.floor()+members;
        if(!scope.isEmpty()&&!scope.equals(current)) {connection.close();sent.clear();pending.clear();lastRequests.clear();nextSend=0;}scope=current;
        String instance=scanner.instance(),floor=scanner.floor();String self=client.player.getName().getString();
        connection.connect(client,endpoint,()->{sent.clear();nextSend=0;connection.send(request(instance,floor,self));},message->{
            String peer=requestedBy(message,instance,floor,members);
            if(peer!=null&&!peer.equalsIgnoreCase(self)) {
                long now=System.nanoTime();String key=peer.toLowerCase(Locale.ROOT);Long last=lastRequests.get(key);
                if(last==null||now-last>=10_000_000_000L) {lastRequests.put(key,now);sent.clear();nextSend=0;}
                return;
            }
            Update update=decode(message,instance,floor,members);if(update==null||update.sender().equalsIgnoreCase(self)) return;
            String key=update.room()+update.cells();Pending old=pending.get(key);
            if(old!=null&&old.update().found()>update.found()) return;
            if(pending.size()>=256&&!pending.containsKey(key)) pending.remove(pending.keySet().iterator().next());
            pending.put(key,new Pending(update,System.nanoTime()));
        });
        long now=System.nanoTime();var iterator=pending.values().iterator();
        while(iterator.hasNext()) {var p=iterator.next();if(now-p.time()>60_000_000_000L||apply(p.update(),scanner.model(),catalog)) iterator.remove();}
        if(!connection.connected()||now<nextSend) return;
        for(var entry:locals.entrySet()) {
            Local local=entry.getValue();if(sent.getOrDefault(entry.getKey(),-1)>=local.found()) continue;
            if(connection.send(count(instance,floor,self,local.room(),local.found(),local.cells()))) {sent.put(entry.getKey(),local.found());nextSend=now+1_000_000_000L;}break;
        }
    }
}
