package net.lidl.lidlclient.dungeons;

import java.util.regex.Pattern;

public final class DungeonSecretProgress {
    private static final Pattern FORMAT=Pattern.compile("(?i)§[0-9a-fk-or]");
    private static final Pattern COUNT=Pattern.compile("(?i)(?<![\\d-])(\\d{1,3})\\s*/\\s*(\\d{1,3})\\s+Secrets\\b");
    public record Count(int found,int total) { }
    private DungeonSecretProgress() { }
    public static Count parse(String actionBar) {
        if(actionBar==null) return null;
        var match=COUNT.matcher(FORMAT.matcher(actionBar).replaceAll(""));
        if(!match.find()) return null;
        int found=Integer.parseInt(match.group(1)),total=Integer.parseInt(match.group(2));
        return found<=total?new Count(found,total):null;
    }
    public static String label(int found,int total) {return Math.clamp(found,0,Math.max(0,total))+"/"+Math.max(0,total);}
}

