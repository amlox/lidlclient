package net.lidl.lidlclient.dungeons;

import java.util.*;

/** Position sources in priority order: live entity, server map, retained last position. */
public final class DungeonMarkerPositions {
    public record Position(double x,double z,float yaw) { }
    public record Icon(String id,boolean self,byte x,byte z,byte rotation) { }
    private final Map<String,Position> positions=new HashMap<>();
    public void reset() {positions.clear();}
    public Position get(String name) {return positions.get(name.toLowerCase(Locale.ROOT));}
    public void live(String name,double x,double z,float yaw) {
        positions.put(name.toLowerCase(Locale.ROOT),new Position(DungeonMapStyle.world(x),DungeonMapStyle.world(z),yaw));
    }
    public void map(List<Icon> icons,List<String> livingOthers,String self,DungeonMapPixels.Calibration c) {
        for(Icon icon:icons) {
            String name=null;
            if(icon.self()) name=self;
            else {
                var match=java.util.regex.Pattern.compile("(\\d+)$").matcher(icon.id());
                if(match.find()) {
                    try {int index=Integer.parseInt(match.group(1));if(index<livingOthers.size()) name=livingOthers.get(index);}
                    catch(NumberFormatException ignored) { }
                }
            }
            if(name==null) continue;
            double mapX=(icon.x()+128)/2.0,mapZ=(icon.z()+128)/2.0;
            double half=c.roomSize()/2.0,pitch=c.roomSize()+4.0;
            double x=14+(mapX-c.x()-half)*20/pitch,z=14+(mapZ-c.z()-half)*20/pitch;
            positions.put(name.toLowerCase(Locale.ROOT),new Position(x,z,Byte.toUnsignedInt(icon.rotation())*22.5f));
        }
    }
}
