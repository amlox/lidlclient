package net.lidl.lidlclient.packaging;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ModMetadataTest {
    private JsonObject read(String name) throws IOException {
        var resources=getClass().getClassLoader().getResources(name);
        while(resources.hasMoreElements()) {
            try(var stream=resources.nextElement().openStream()) {
                JsonObject json=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
                if(!name.equals("fabric.mod.json")||json.has("id")&&json.get("id").getAsString().equals("lidlclient")) return json;
            }
        }
        throw new IOException("Missing LidlClient resource: "+name);
    }
    @Test void allGameplayInitializersAndMixinClassesRemainLoadableWithoutTemplateHooks() throws Exception {
        JsonObject mod=read("fabric.mod.json");assertEquals("client",mod.get("environment").getAsString());
        var entrypoints=mod.getAsJsonObject("entrypoints");assertEquals(Set.of("client"),entrypoints.keySet());
        var expected=Set.of("net.lidl.lidlclient.PartyCMDS.PartyCommands","net.lidl.lidlclient.slots.Slots","net.lidl.lidlclient.gui.ModuleFramework","net.lidl.lidlclient.commands.WarpCommands","net.lidl.lidlclient.hotbar.DungeonbreakerSwap","net.lidl.lidlclient.pets.ZeroPingPets","net.lidl.lidlclient.size.PlayerSize","net.lidl.lidlclient.dungeons.DungeonMap");
        Set<String> actual=new HashSet<>();for(var entry:entrypoints.getAsJsonArray("client")) {String name=entry.getAsString();actual.add(name);Class.forName(name,false,getClass().getClassLoader());}assertEquals(expected,actual);
        JsonObject mixins=read("lidlclient.mixins.json");assertFalse(mixins.has("mixins"));
        for(var entry:mixins.getAsJsonArray("client")) Class.forName(mixins.get("package").getAsString()+"."+entry.getAsString(),false,getClass().getClassLoader());
    }
}
