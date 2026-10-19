package net.lidl.lidlclient;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LidlClient {
    public static final String MOD_ID="lidlclient";
    public static final Logger LOGGER=LoggerFactory.getLogger(MOD_ID);
    private LidlClient() { }
    public static Identifier id(String path) {return Identifier.fromNamespaceAndPath(MOD_ID,path);}
}
