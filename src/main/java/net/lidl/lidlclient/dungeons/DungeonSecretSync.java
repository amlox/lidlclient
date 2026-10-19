package net.lidl.lidlclient.dungeons;

import net.minecraft.client.Minecraft;
import java.util.*;
import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;
import static net.lidl.lidlclient.dungeons.DungeonRelayProtocol.*;

/** Native WebSocket adapters. There is deliberately no Hypixel chat transport. */
public final class DungeonSecretSync {
    private record Local(String room,int found,List<Cell> cells) { }
    private record Pending(Update update,long time) { }
    private final Map<Backend,DungeonRelayConnection> connections=new EnumMap<>(Backend.class);
    private final Map<Backend,Map<String,Integer>> sent=new EnumMap<>(Backend.class);
    private final Map<Backend,Long> nextSend=new EnumMap<>(Backend.class);
    private final Map<String,Local> locals=new LinkedHashMap<>();
    private final Map<String,Pending> pending=new LinkedHashMap<>();
    private String scope="",noammRegistration="";
    public DungeonSecretSync() {for(Backend b:Backend.values()) {connections.put(b,new DungeonRelayConnection(b));sent.put(b,new HashMap<>());}}
    public String status(Backend backend) {
        if(!DungeonMap.secretSync()) return "Disabled";
        if(scope.isEmpty()) return "Waiting for dungeon instance";
        return connections.get(backend).status();
    }
    public void reset() {
        connections.values().forEach(DungeonRelayConnection::close);sent.values().forEach(Map::clear);nextSend.clear();locals.clear();pending.clear();scope="";noammRegistration="";
    }
    public void local(DungeonMapScanner scanner,int x,int z) {
        if(!DungeonMap.secretSync()||scanner.instance().isEmpty()) return;
        var model=scanner.model();int id=z*GRID+x;
        for(var group:model.groups()) if(group.contains(id)) {
            Tile room=model.representative(group);
            if(!room.identified||room.secrets<=0||group.size()>4) return;
            var cells=group.stream().map(index->new Cell((index%GRID)/2,(index/GRID)/2)).toList();
            String key=room.name+cells;Local previous=locals.get(key);int found=model.foundSecrets(group);
            if(previous==null||found>previous.found()) locals.put(key,new Local(room.name,found,cells));return;
        }
    }
    private void receive(Minecraft client,DungeonMapScanner scanner,Backend backend,String message) {
        if(!DungeonMap.secretSync()||!scanner.active()) return;
        Set<String> names=new HashSet<>(scanner.party().names());Set<UUID> uuids=new HashSet<>();
        if(client.player!=null) {names.add(client.player.getName().getString());uuids.add(client.player.getUUID());}
        if(client.getConnection()!=null) for(String name:names) {var info=client.getConnection().getPlayerInfo(name);if(info!=null) uuids.add(info.getProfile().id());}
        Update update=decode(backend,message,scanner.instance(),names,uuids);if(update==null) return;
        String key=backend+":"+update.room()+":"+(update.found()<0?"room":"count")+update.cells();
        Pending old=pending.get(key);if(old!=null&&old.update().found()>update.found()) return;
        if(pending.size()>=256&&!pending.containsKey(key)) pending.remove(pending.keySet().iterator().next());
        pending.put(key,new Pending(update,System.nanoTime()));
    }
    /** Returns true for applied/invalid updates, false when awaiting server map room geometry. */
    public static boolean apply(Update update,DungeonMapModel model,RoomCatalog catalog) {
        var room=catalog.findName(update.room());if(room==null||update.found()>room.secrets()) return true;
        if(!update.cells().isEmpty()) {
            Set<Cell> cells=new HashSet<>(update.cells());Set<Cell> visited=new HashSet<>();ArrayDeque<Cell> queue=new ArrayDeque<>();queue.add(update.cells().getFirst());
            while(!queue.isEmpty()) {Cell c=queue.remove();if(!visited.add(c)) continue;for(Cell n:cells) if(Math.abs(n.x()-c.x())+Math.abs(n.z()-c.z())==1&&!visited.contains(n)) queue.add(n);}
            if(visited.size()!=cells.size()) return true;
            for(Cell cell:cells) {Tile tile=model.at(cell.x()*2,cell.z()*2);if(tile.kind!=Kind.ROOM||!tile.mapKnown) return false;if(tile.identified&&!tile.name.equals(room.name())) return true;}
            for(Cell cell:cells) model.identify(cell.x()*2,cell.z()*2,room);
            if(update.found()>=0) for(Cell cell:cells) model.updateSecrets(cell.x()*2,cell.z()*2,update.found(),room.secrets());
            return true;
        }
        var matches=model.groups().stream().filter(group->{Tile tile=model.representative(group);return tile.identified&&tile.name.equals(room.name());}).toList();
        if(matches.isEmpty()) return false;
        if(matches.size()!=1) return true;
        int id=matches.getFirst().getFirst();if(update.found()>=0) model.updateSecrets(id%GRID,id/GRID,update.found(),room.secrets());return true;
    }
    private static Cell entrance(DungeonMapModel model) {
        for(int z=0;z<GRID;z+=2) for(int x=0;x<GRID;x+=2) if(model.at(x,z).kind==Kind.ROOM&&model.at(x,z).type==Type.ENTRANCE) return new Cell(x/2,z/2);return null;
    }
    public void tick(Minecraft client,DungeonMapScanner scanner,RoomCatalog catalog) {
        if(!DungeonMap.secretSync()||!scanner.active()) {if(!scope.isEmpty()) reset();return;}
        if(scanner.instance().isEmpty()||client.player==null) return;
        String current=scanner.instance()+"/"+scanner.floor();
        if(!scope.isEmpty()&&!scope.equals(current)) reset();scope=current;
        Cell entrance=entrance(scanner.model());
        LinkedHashSet<String> members=new LinkedHashSet<>(scanner.party().names());members.add(client.player.getName().getString());
        for(Backend backend:Backend.values()) {
            var connection=connections.get(backend);
            if(backend==Backend.NOAMM&&entrance==null) {connection.waiting("Waiting for entrance room");continue;}
            connection.connect(client,backend==Backend.ODIN?scanner.odinInstance():scanner.instance(),()->{
                sent.get(backend).clear();nextSend.remove(backend);
                if(backend==Backend.SKYBLOCKER) connection.send(skyEnvelope("subscribe",scanner.instance(),null));
                if(backend==Backend.NOAMM) noammRegistration="";
            },message->receive(client,scanner,backend,message));
            if(!connection.connected()) continue;
            if(backend==Backend.NOAMM) {
                String registration=current+members+entrance;
                if(!registration.equals(noammRegistration)) {connection.send(noammStart(scanner.instance(),scanner.floor(),List.copyOf(members),entrance));noammRegistration=registration;}
            }
        }
        long now=System.nanoTime();var iterator=pending.values().iterator();
        while(iterator.hasNext()) {var p=iterator.next();if(now-p.time()>60_000_000_000L||apply(p.update(),scanner.model(),catalog)) iterator.remove();}
        for(Backend backend:Backend.values()) {
            var connection=connections.get(backend);if(!connection.connected()||now<nextSend.getOrDefault(backend,Long.MIN_VALUE)) continue;
            for(var entry:locals.entrySet()) {
                Local local=entry.getValue();if(sent.get(backend).getOrDefault(entry.getKey(),-1)>=local.found()) continue;
                var message=count(backend,scanner.instance(),client.player.getName().getString(),client.player.getUUID(),local.room(),catalog.skyName(local.room()),local.found(),local.cells());
                if(message==null) {sent.get(backend).put(entry.getKey(),local.found());continue;}
                if(backend==Backend.NOAMM) for(Cell cell:local.cells()) connection.send(noammRoom(local.room(),cell));
                if(connection.send(message)) {sent.get(backend).put(entry.getKey(),local.found());nextSend.put(backend,now+1_000_000_000L);}break;
            }
        }
    }
}
