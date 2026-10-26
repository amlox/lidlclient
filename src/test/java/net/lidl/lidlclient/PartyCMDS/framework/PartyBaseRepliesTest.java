package net.lidl.lidlclient.PartyCMDS.framework;

import net.lidl.lidlclient.PartyCMDS.PartyCommands;
import net.lidl.lidlclient.PartyCMDS.PartyCommandSwitches;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PartyBaseRepliesTest {
    @Test void everyEditableLegacyCommandKeepsItsBaseInsteadOfAGreeting() {
        for (String trigger:PartyCommandSwitches.COMMANDS) {
            if (trigger.equals("!ban") || trigger.equals("!help")) continue;
            var base=PartyCommands.baseReply(trigger);
            assertFalse(base.answers().contains("Hello {player}!"),trigger);
            assertTrue(PartyCommands.usesOriginalReply(base),trigger);
            assertTrue(PartyCommands.usesOriginalReply(new PartyReply(trigger,base.answers(),false)),trigger);
            assertFalse(PartyCommands.usesOriginalReply(new PartyReply(trigger,List.of("Edited answer"),true)),trigger);
        }
        assertEquals(List.of("You rolled a {roll:1:6}!"),PartyCommands.baseReply("!dice").answers());
        assertEquals(List.of("{player} has {roll:-100:200} IQ!"),PartyCommands.baseReply("!iq").answers());
        assertEquals(List.of("Coinflip: Heads!","Coinflip: Tails!"),PartyCommands.baseReply("!coinflip").answers());
        assertEquals(29,PartyCommands.baseReply("!tesco").answers().size());
        assertEquals(List.of("Hello {player}!"),PartyCommands.baseReply("!new").answers());
    }
    @Test void percentCommandsKeepTheOneToHundredRange() {
        for (String trigger:List.of("!femboy","!jew","!autism","!white","!mexican"))
            assertTrue(PartyCommands.baseReply(trigger).answers().getFirst().contains("{roll:1:100}"),trigger);
    }
    @Test void customSlotFormattingKeepsSymbolsAndJackpotRules() {
        String template=PartyCommands.baseReply("!slots").answers().getFirst();
        assertEquals("Rolled [ <3 | :star: | :yes: ]",PartyReply.slots(template,"<3",":star:",":yes:"));
        assertEquals("Rolled [ <3 | <3 | <3 ]:star: JACKPOT! :star:",PartyReply.slots(template,"<3","<3","<3"));
        String rendered=new PartyReply("!slots",List.of("Custom {slot1} / {slot2} / {slot3}{jackpot}"),true).render("Alex","");
        assertFalse(rendered.contains("{slot"));
        assertFalse(rendered.contains("{jackpot}"));
    }
}
