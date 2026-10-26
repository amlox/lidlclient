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
}
