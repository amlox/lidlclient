package net.lidl.lidlclient.PartyCMDS.framework;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/** Saved reply templates with explicit output and loop-protection choices. */
public record PartyReply(String trigger,List<String> answers,boolean enabled,boolean partyChat,
                         boolean bypassLoopProtection,boolean runCommands,int consecutiveLimit) {
    public PartyReply(String trigger,List<String> answers,boolean enabled) { this(trigger,answers,enabled,true); }
    public PartyReply(String trigger,List<String> answers,boolean enabled,boolean partyChat) {
        this(trigger,answers,enabled,partyChat,false,false);
    }
    public PartyReply(String trigger,List<String> answers,boolean enabled,boolean partyChat,
                      boolean bypassLoopProtection,boolean runCommands) {
        this(trigger,answers,enabled,partyChat,bypassLoopProtection,runCommands,0);
    }
    public static int parseConsecutiveLimit(String value) {
        try {
            if (!value.matches("[0-9]{1,10}")) throw new NumberFormatException();
            return Integer.parseInt(value);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Consecutive limit: enter 0–2147483647 (0 = unlimited)");
        }
    }
    public static void requireAvailable(String trigger,java.util.Collection<String> existing) {
        String normalized=trigger.strip().toLowerCase(Locale.ROOT);
        if (existing.stream().anyMatch(value -> value.equalsIgnoreCase(normalized)))
            throw new IllegalArgumentException(normalized+" already exists. Edit the existing command instead.");
    }
    private static final Pattern ROLL=Pattern.compile("\\{roll:(-?\\d{1,6}):(-?\\d{1,6})}");
    public PartyReply {
        if (consecutiveLimit<0) throw new IllegalArgumentException("Consecutive limit cannot be negative");
        if (trigger==null || !trigger.matches("![A-Za-z0-9_-]{1,24}"))
            throw new IllegalArgumentException("Trigger must be ! followed by 1–24 letters, numbers, _ or -");
        trigger=trigger.toLowerCase(Locale.ROOT);
        if (runCommands) partyChat=false;
        if (trigger.equals("!ban") || trigger.equals("!help"))
            throw new IllegalArgumentException("!ban and !help are reserved built-in commands");
        if (answers==null || answers.isEmpty() || answers.size()>32)
            throw new IllegalArgumentException("Add 1–32 answers");
        answers=List.copyOf(answers);
        for (String answer:answers) {
            if (answer==null || answer.isBlank() || answer.length()>256 || answer.codePoints().anyMatch(Character::isISOControl))
                throw new IllegalArgumentException("Each answer must contain 1–256 characters on one line");
            if (!bypassLoopProtection && answer.stripLeading().startsWith("!"))
                throw new IllegalArgumentException("Answers cannot start with ! (prevents reply loops)");
            var matcher=ROLL.matcher(answer);
            while (matcher.find()) if (Integer.parseInt(matcher.group(1))>Integer.parseInt(matcher.group(2)))
                throw new IllegalArgumentException("Roll minimum must not exceed maximum");
        }
    }
    public PartyReply withEnabled(boolean value) { return new PartyReply(trigger,answers,value,partyChat,bypassLoopProtection,runCommands,consecutiveLimit); }
    public String render(String player,String args) {
        String template=answers.get(ThreadLocalRandom.current().nextInt(answers.size()));
        if (template.contains("{slot1}") || template.contains("{slot2}") || template.contains("{slot3}") || template.contains("{jackpot}"))
            template=slots(template,randomSymbol(),randomSymbol(),randomSymbol());
        var matcher=ROLL.matcher(template);
        StringBuilder rendered=new StringBuilder();
        while (matcher.find()) {
            int min=Integer.parseInt(matcher.group(1)),max=Integer.parseInt(matcher.group(2));
            matcher.appendReplacement(rendered,Integer.toString(ThreadLocalRandom.current().nextInt(min,max+1)));
        }
        matcher.appendTail(rendered);
        // Substitute user text last: arguments containing placeholders remain plain text.
        String result=rendered.toString().replace("{player}",clean(player)).replace("{args}",clean(args));
        if (!bypassLoopProtection && result.stripLeading().startsWith("!")) result="[Reply] "+result;
        if (result.length()>240) result=result.substring(0,Character.isHighSurrogate(result.charAt(239))?239:240);
        return result.strip();
    }
    public String commandToRun(String response) {
        String value=response.strip();
        if (!runCommands || !value.startsWith("/") || value.startsWith("//") || value.substring(1).isBlank())
            throw new IllegalArgumentException("Command output must start with / followed by a command");
        return value.substring(1).strip();
    }
    private static String randomSymbol() {
        var symbols=List.of("<3",":star:",":yes:",":no:",":java:",":arrow:");
        return symbols.get(ThreadLocalRandom.current().nextInt(symbols.size()));
    }
    static String slots(String template,String first,String second,String third) {
        return template.replace("{slot1}",first).replace("{slot2}",second).replace("{slot3}",third)
                .replace("{jackpot}",first.equals(second)&&second.equals(third)?":star: JACKPOT! :star:":"");
    }
    private static String clean(String text) {
        return text==null?"":text.replaceAll("[\\p{Cc}\\p{Cf}]","");
    }
}
