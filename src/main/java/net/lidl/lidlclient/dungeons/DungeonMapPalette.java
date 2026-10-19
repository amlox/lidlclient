package net.lidl.lidlclient.dungeons;

import java.util.*;
import static net.lidl.lidlclient.dungeons.DungeonMapModel.*;

/** Editable palette and parsing, independent of Minecraft/config initialization. */
public final class DungeonMapPalette {
    public enum Key {
        PUZZLE("Puzzle rooms",0x7B007B), NORMAL("Non-cleared base rooms",0x794600), TRAP("Trap rooms",0xFF8200),
        MINIBOSS("Mini Boss rooms",0xFFC800), BLOOD("Blood rooms",0xB20000),
        NORMAL_DOOR("Normal doors",0x794600), WITHER_DOOR("Wither doors",0x101010), BLOOD_DOOR("Blood doors",0xB20000),
        CLEARED("Cleared rooms",0xA1814C), SECRETED("Cleared + all secrets",0x55FF55);
        public final String label;
        public final int rgb;
        Key(String label,int rgb) {this.label=label;this.rgb=rgb;}
    }
    private static final int[] LEGACY={0x000000,0x0000AA,0x00AA00,0x00AAAA,0xAA0000,0xAA00AA,0xFFAA00,0xAAAAAA,0x555555,0x5555FF,0x55FF55,0x55FFFF,0xFF5555,0xFF55FF,0xFFFF55,0xFFFFFF};
    private final Map<Key,Integer> colors=new EnumMap<>(Key.class);
    public int get(Key key) {return 0xFF000000|colors.getOrDefault(key,key.rgb);}
    public void set(Key key,int value) {colors.put(key,value&0xFFFFFF);}
    public int room(Type type) {
        return switch(type) {
            case PUZZLE->get(Key.PUZZLE);case NORMAL->get(Key.NORMAL);case TRAP->get(Key.TRAP);
            case CHAMPION->get(Key.MINIBOSS);case BLOOD->get(Key.BLOOD);
            case FAIRY->0xFFE39BE2;case RARE->0xFFB2B2B2;case ENTRANCE->0xFF00FF00;default->0xFF414141;
        };
    }
    public int door(Door door) {
        return switch(door) {case NORMAL->get(Key.NORMAL_DOOR);case WITHER->get(Key.WITHER_DOOR);case BLOOD->get(Key.BLOOD_DOOR);case ENTRANCE->0xFF00FF00;};
    }
    public static String hex(int color) {return String.format(Locale.ROOT,"#%06X",color&0xFFFFFF);}
    public static Integer parse(String input) {
        if(input==null) return null;
        String text=input.strip();
        if(text.matches("(?i)[&§][0-9a-f]")) return 0xFF000000|LEGACY[Character.digit(text.charAt(1),16)];
        if(text.startsWith("&#")) text=text.substring(2);
        else if(text.startsWith("#")) text=text.substring(1);
        else if(text.startsWith("0x") || text.startsWith("0X")) text=text.substring(2);
        return text.matches("(?i)[0-9a-f]{6}")?0xFF000000|Integer.parseInt(text,16):null;
    }
}

