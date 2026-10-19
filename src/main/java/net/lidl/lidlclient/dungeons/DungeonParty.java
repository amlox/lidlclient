package net.lidl.lidlclient.dungeons;

import java.util.*;
import java.util.regex.Pattern;

/** Accept dungeon roster rows, not arbitrary player entities or NPC tab entries. */
public final class DungeonParty {
    private static final Pattern FORMAT=Pattern.compile("(?i)§[0-9a-fk-or]");
    private static final Pattern ROW = Pattern.compile(
            "^\\[\\d+] (?:\\[[^]]+] )*([A-Za-z0-9_]{1,16}) .*\\((Mage|Archer|Berserk|Healer|Tank|DEAD)(?: [A-Za-z0-9]+)?\\)$",
            Pattern.CASE_INSENSITIVE);
    private final Set<String> members = new HashSet<>(), dead = new HashSet<>();
    private final Map<String,String> names=new LinkedHashMap<>();
    public void reset() { members.clear(); dead.clear(); names.clear(); }
    public void update(Collection<String> lines) {
        reset();
        for(String line:lines) {
            var match=ROW.matcher(FORMAT.matcher(line).replaceAll("").strip());
            if(!match.matches()) continue;
            String name=match.group(1).toLowerCase(Locale.ROOT);
            members.add(name);
            names.put(name,match.group(1));
            if(match.group(2).equalsIgnoreCase("DEAD")) dead.add(name);
        }
    }
    public boolean show(String realName,boolean self) {
        String name=realName.toLowerCase(Locale.ROOT);
        return !dead.contains(name) && (self || members.contains(name));
    }
    public List<String> names() {return List.copyOf(names.values());}
    public boolean dead(String name) {return dead.contains(name.toLowerCase(Locale.ROOT));}
    public boolean contains(String name) {return members.contains(name.toLowerCase(Locale.ROOT));}
}


