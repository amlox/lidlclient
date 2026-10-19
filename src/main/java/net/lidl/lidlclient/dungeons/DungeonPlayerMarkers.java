package net.lidl.lidlclient.dungeons;

import net.lidl.lidlclient.mixin.DungeonMapDataAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import java.util.*;

/** Party roster drives markers, never the list of player-shaped mobs in the level. */
public final class DungeonPlayerMarkers {
    public record Marker(String name,Identifier skin,DungeonMarkerPositions.Position position,boolean self,boolean dead) { }
    private final DungeonMarkerPositions positions=new DungeonMarkerPositions();
    private final Map<String,Identifier> skins=new HashMap<>();
    private List<Marker> markers=List.of();
    public List<Marker> markers() {return markers;}
    public void reset() {positions.reset();skins.clear();markers=List.of();}
    public void map(Minecraft client,DungeonParty party,MapItemSavedData data,DungeonMapPixels.Calibration calibration) {
        if(!DungeonMap.isEnabled()||client.player==null) return;
        String self=client.player.getName().getString();
        var others=party.names().stream().filter(name->!name.equalsIgnoreCase(self)&&!party.dead(name)).toList();
        var decorations=((DungeonMapDataAccessor)data).lidlclient$decorations();
        var icons=decorations.entrySet().stream().map(entry->{var d=entry.getValue();return new DungeonMarkerPositions.Icon(
                entry.getKey(),d.type().value()==MapDecorationTypes.FRAME.value(),d.x(),d.y(),d.rot());}).toList();
        positions.map(icons,others,self,calibration);
    }
    public void update(Minecraft client,DungeonParty party) {
        if(!DungeonMap.isEnabled()) return;
        if(client.player==null||client.level==null||client.getConnection()==null) {reset();return;}
        String self=client.player.getName().getString();
        LinkedHashSet<String> names=new LinkedHashSet<>(party.names());names.add(self);
        List<Marker> next=new ArrayList<>();
        for(String name:names) {
            var info=client.getConnection().getPlayerInfo(name);
            if(info!=null) skins.put(name.toLowerCase(Locale.ROOT),info.getSkin().body().texturePath());
            for(var player:client.level.players()) if(player.getName().getString().equalsIgnoreCase(name)) {
                positions.live(name,player.getX(),player.getZ(),player.getYRot());
                if((Object)player instanceof AbstractClientPlayer avatar) skins.put(name.toLowerCase(Locale.ROOT),avatar.getSkin().body().texturePath());
                break;
            }
            next.add(new Marker(name,skins.get(name.toLowerCase(Locale.ROOT)),positions.get(name),name.equalsIgnoreCase(self),party.dead(name)));
        }
        markers=List.copyOf(next);
    }
}
