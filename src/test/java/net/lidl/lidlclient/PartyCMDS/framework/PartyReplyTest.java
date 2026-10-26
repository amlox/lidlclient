package net.lidl.lidlclient.PartyCMDS.framework;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class PartyReplyTest {
    @Test void templatesSubstitutePlayerArgumentsAndInclusiveRolls() {
        var reply=new PartyReply("!DICE",List.of("{player}: {args} ({roll:2:2})"),true);
        assertEquals("!dice",reply.trigger());
        assertEquals("Alex: {roll:1:6} (2)",reply.render("Alex","{roll:1:6}"));
        var random=new PartyReply("!random",List.of("{roll:-2:2}"),true);
        for (int i=0;i<100;i++) assertTrue(Set.of("-2","-1","0","1","2").contains(random.render("", "")));
        var choices=new PartyReply("!coinflip",List.of("Heads","Tails"),true);
        for (int i=0;i<100;i++) assertTrue(choices.answers().contains(choices.render("Alex","")));
    }
    @Test void reservedTriggersMalformedDataAndReplyLoopsAreRejected() {
        for (String trigger:List.of("dice","!help","!ban","!two words","!"))
            assertThrows(IllegalArgumentException.class,()->new PartyReply(trigger,List.of("Hi"),true));
        for (String answer:List.of(""," ","bad\nline","!dice","{roll:9:1}","x".repeat(257)))
            assertThrows(IllegalArgumentException.class,()->new PartyReply("!ok",List.of(answer),true));
        assertThrows(IllegalArgumentException.class,()->new PartyReply("!ok",List.of(),true));
        assertEquals("[Reply] !dice",new PartyReply("!echo",List.of("{args}"),true).render("Alex","!dice"));
    }
    @Test void generatedMessagesAreBoundedAndKeepUnicodePairsIntact() {
        var reply=new PartyReply("!echo",List.of("{args}"),true);
        String rendered=reply.render("Alex","a".repeat(239)+"😀");
        assertEquals(239,rendered.length());
        assertFalse(Character.isHighSurrogate(rendered.charAt(rendered.length()-1)));
        assertEquals("ab",reply.render("Alex","a\nb\u0000"));
        assertEquals("Ready",new PartyReply("!ok",List.of("Ready"),true).render(null,null));
    }
    @Test void codecRoundTripsAndToleratesBadLocalEntriesOnly() {
        var reply=new PartyReply("!hello",List.of("Hello {player}!","Hi"),false);
        assertEquals(reply,ReplyCodec.read(ReplyCodec.JSON.toJsonTree(reply)));
        var source=JsonParser.parseString("""
                [{"trigger":"!good","answers":["Hi"],"enabled":true},
                 {"trigger":"!bad","answers":["Hi"],"enabled":"true"}]
                """);
        assertEquals(Set.of("!good"),ReplyCodec.readAll(source,true).keySet());
        assertThrows(IllegalArgumentException.class,()->ReplyCodec.readAll(source,false));
        var duplicate=JsonParser.parseString("["+ReplyCodec.JSON.toJson(reply)+","+ReplyCodec.JSON.toJson(reply)+"]");
        assertThrows(IllegalArgumentException.class,()->ReplyCodec.readAll(duplicate,false));
    }
    @Test void duplicateTriggersAreBlockedIncludingBuiltInsAndDifferentCapitalization() {
        var commands=java.util.List.of("!dice","!hello","!hi");
        for (String trigger:java.util.List.of("!dice","!DICE"," !Hello ")) {
            var error=assertThrows(IllegalArgumentException.class,()->PartyReply.requireAvailable(trigger,commands));
            assertTrue(error.getMessage().contains("already exists"));
        }
        assertDoesNotThrow(()->PartyReply.requireAvailable("!high",commands));
        assertDoesNotThrow(()->PartyReply.requireAvailable("!new",commands));
    }
    @Test void oldRepliesDefaultToPartyChatAndPrivateOutputSurvivesSaveAndToggle() {
        var legacy=ReplyCodec.read(JsonParser.parseString("""
                {"trigger":"!hello","answers":["Hello"],"enabled":true}
                """));
        assertTrue(legacy.partyChat());
        var privateReply=new PartyReply("!hello",List.of("Private answer"),true,false);
        assertEquals(privateReply,ReplyCodec.read(ReplyCodec.JSON.toJsonTree(privateReply)));
        var disabled=privateReply.withEnabled(false);
        assertFalse(disabled.enabled()); assertFalse(disabled.partyChat());
        assertEquals(privateReply,disabled.withEnabled(true));
        assertThrows(IllegalArgumentException.class,()->ReplyCodec.read(JsonParser.parseString("""
                {"trigger":"!hello","answers":["Hi"],"enabled":true,"partyChat":"false"}
                """)));
    }

    @Test void loopProtectionBypassAllowsLiteralAndSubstitutedCommandReplies() {
        var literal=new PartyReply("!chain",List.of("!dice"),true,true,true,false);
        assertEquals("!dice",literal.render("Alex",""));
        var echo=new PartyReply("!echo",List.of("{args}"),true,true,true,false);
        assertEquals("!dice",echo.render("Alex","!dice"));
        assertEquals(literal,ReplyCodec.read(ReplyCodec.JSON.toJsonTree(literal)));
        assertEquals(literal,literal.withEnabled(false).withEnabled(true));
        var legacy=ReplyCodec.read(JsonParser.parseString("""
                {"trigger":"!old","answers":["{args}"],"enabled":true}
                """));
        assertFalse(legacy.bypassLoopProtection()); assertFalse(legacy.runCommands());
        assertEquals("[Reply] !dice",legacy.render("Alex","!dice"));
    }
    @Test void directCommandsStripSlashWithoutAddingPartyChatAndPersistTheirMode() {
        var command=new PartyReply("!warp",List.of("/warp {args}"),true,true,false,true);
        assertFalse(command.partyChat()); assertTrue(command.runCommands());
        assertEquals("warp hub",command.commandToRun(command.render("Alex","hub")));
        assertEquals(command,ReplyCodec.read(ReplyCodec.JSON.toJsonTree(command)));
        assertEquals(command,command.withEnabled(false).withEnabled(true));
        for (String invalid:List.of("warp hub","/","/   ","//warp hub"))
            assertThrows(IllegalArgumentException.class,()->command.commandToRun(invalid));
        assertThrows(IllegalArgumentException.class,()->new PartyReply("!chat",List.of("Hi"),true).commandToRun("/warp hub"));
        assertThrows(IllegalArgumentException.class,()->ReplyCodec.read(JsonParser.parseString("""
                {"trigger":"!old","answers":["Hi"],"enabled":true,"bypassLoopProtection":"true"}
                """)));
    }

}
