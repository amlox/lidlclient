package net.lidl.lidlclient.PartyCMDS.framework;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/** Data-only replies: no scripts, URLs to execute, or server-command actions. */
public record PartyReply(String trigger,List<String> answers,boolean enabled) {
    private static final Pattern ROLL=Pattern.compile("\\{roll:(-?\\d{1,6}):(-?\\d{1,6})}");
    public PartyReply {
        if (trigger==null || !trigger.matches("![A-Za-z0-9_-]{1,24}"))
            throw new IllegalArgumentException("Trigger must be ! followed by 1–24 letters, numbers, _ or -");
        trigger=trigger.toLowerCase(Locale.ROOT);
        if (trigger.equals("!ban") || trigger.equals("!help"))
            throw new IllegalArgumentException("!ban and !help are reserved built-in commands");
        if (answers==null || answers.isEmpty() || answers.size()>32)
            throw new IllegalArgumentException("Add 1–32 answers");
        answers=List.copyOf(answers);
        for (String answer:answers) {
            if (answer==null || answer.isBlank() || answer.length()>256 || answer.codePoints().anyMatch(Character::isISOControl))
                throw new IllegalArgumentException("Each answer must contain 1–256 characters on one line");
            if (answer.stripLeading().startsWith("!"))
                throw new IllegalArgumentException("Answers cannot start with ! (prevents reply loops)");
            var matcher=ROLL.matcher(answer);
            while (matcher.find()) if (Integer.parseInt(matcher.group(1))>Integer.parseInt(matcher.group(2)))
                throw new IllegalArgumentException("Roll minimum must not exceed maximum");
        }
    }
    public String render(String player,String args) {
        String template=answers.get(ThreadLocalRandom.current().nextInt(answers.size()));
        var matcher=ROLL.matcher(template);
        StringBuilder rendered=new StringBuilder();
        while (matcher.find()) {
            int min=Integer.parseInt(matcher.group(1)),max=Integer.parseInt(matcher.group(2));
            matcher.appendReplacement(rendered,Integer.toString(ThreadLocalRandom.current().nextInt(min,max+1)));
        }
        matcher.appendTail(rendered);
        // Substitute user text last: arguments containing placeholders remain plain text.
        String result=rendered.toString().replace("{player}",clean(player)).replace("{args}",clean(args));
        if (result.stripLeading().startsWith("!")) result="[Reply] "+result;
        if (result.length()>240) result=result.substring(0,Character.isHighSurrogate(result.charAt(239))?239:240);
        return result.strip();
    }
    private static String clean(String text) {
        return text==null?"":text.replaceAll("[\\p{Cc}\\p{Cf}]","");
    }
}
