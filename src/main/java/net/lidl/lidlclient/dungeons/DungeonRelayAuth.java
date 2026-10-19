package net.lidl.lidlclient.dungeons;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.lidl.lidlclient.mixin.DungeonProfileKeysAccessor;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import java.net.URI;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.security.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** Signed public profile-certificate proof. Never sends access tokens or private keys. */
public final class DungeonRelayAuth {
    public record Token(String value,long expiresAt) { }
    private static final Map<DungeonRelayProtocol.Backend,Token> CACHE=new ConcurrentHashMap<>();
    private DungeonRelayAuth() { }
    public static String version() {return FabricLoader.getInstance().getModContainer("lidlclient").orElseThrow().getMetadata().getVersion().getFriendlyString();}
    public static JsonObject proof(UUID uuid,PublicKey publicKey,byte[] certificate,long expires,PrivateKey privateKey,String minecraftVersion,String modVersion) throws GeneralSecurityException {
        UUID nonce=UUID.randomUUID();byte[] challenge=ByteBuffer.allocate(16).putLong(nonce.getMostSignificantBits()).putLong(nonce.getLeastSignificantBits()).array();
        Signature signature=Signature.getInstance("SHA256withRSA");signature.initSign(privateKey);signature.update(challenge);
        Base64.Encoder b64=Base64.getEncoder();JsonObject pair=new JsonObject();
        pair.addProperty("uuid",uuid.toString());pair.addProperty("publicKey",b64.encodeToString(publicKey.getEncoded()));pair.addProperty("publicKeySignature",b64.encodeToString(certificate));pair.addProperty("expiresAt",expires);
        JsonObject signed=new JsonObject();signed.addProperty("original",b64.encodeToString(challenge));signed.addProperty("signed",b64.encodeToString(signature.sign()));
        JsonObject request=new JsonObject();request.add("keyPair",pair);request.add("signedData",signed);
        request.addProperty("mod","lidlclient");request.addProperty("minecraftVersion",minecraftVersion);request.addProperty("modVersion",modVersion);return request;
    }
    public static CompletableFuture<Token> authenticate(HttpClient http,DungeonRelayProtocol.Backend backend,Minecraft client) {
        Token cached=CACHE.get(backend);if(cached!=null&&cached.expiresAt()>System.currentTimeMillis()+60_000) return CompletableFuture.completedFuture(cached);
        String endpoint=switch(backend) {case NOAMM->"https://api.noamm.org/hypixel/auth";case SKYBLOCKER->"https://hysky.de/api/aaron/authenticate";default->throw new IllegalArgumentException();};
        UUID uuid=client.getUser().getProfileId();
        return ((DungeonProfileKeysAccessor)client).lidlclient$profileKeys().prepareKeyPair().thenComposeAsync(optional->{
            if(optional.isEmpty()||optional.get().publicKey().data().hasExpired()) return CompletableFuture.failedFuture(new IllegalStateException("Minecraft profile keys unavailable"));
            try {
                var keys=optional.get();var data=keys.publicKey().data();
                JsonObject request=proof(uuid,data.key(),data.keySignature(),data.expiresAt().toEpochMilli(),keys.privateKey(),SharedConstants.getCurrentVersion().name(),version());
                return http.sendAsync(HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(12)).header("Content-Type","application/json")
                        .header("User-Agent","LidlClient/"+version()).POST(HttpRequest.BodyPublishers.ofString(request.toString())).build(),HttpResponse.BodyHandlers.ofString())
                        .thenApply(response->{
                            if(response.statusCode()!=200||response.body().length()>16384) throw new IllegalStateException("Authentication HTTP "+response.statusCode());
                            JsonObject json=JsonParser.parseString(response.body()).getAsJsonObject();String token=json.get("token").getAsString();long expires=json.get("expiresAt").getAsLong();
                            if(token.isBlank()||token.length()>8192||expires<=System.currentTimeMillis()) throw new IllegalStateException("Invalid authentication response");
                            Token result=new Token(token,expires);CACHE.put(backend,result);return result;
                        });
            } catch(GeneralSecurityException|RuntimeException e) {return CompletableFuture.failedFuture(e);}
        });
    }
    public static void invalidate(DungeonRelayProtocol.Backend backend) {CACHE.remove(backend);}
}
