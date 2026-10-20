package net.lidl.lidlclient.pets;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Pure recognition and bounded reopen suppression, separate from Minecraft events. */
public final class PetsMenuRules {
    public static final long SUPPRESSION_NS = 2_000_000_000L;
    private static final Pattern TITLE = Pattern.compile("^(?:Pets(?: \\(\\d+/\\d+\\))?|\\(\\d+/\\d+\\) Pets)$");
    private boolean armed;
    private long selectedAt;

    private static String plain(String text) {
        return text.replaceAll("(?i)§[0-9A-FK-ORX]", "").replace('\u00a0', ' ').strip().replaceAll("\\s+", " ");
    }

    public static boolean isPetsMenu(String title) {
        return TITLE.matcher(plain(title)).matches();
    }

    public static boolean isPetSelection(String title, boolean normalLeftClick, boolean playerHead,
                                         boolean topContainerSlot, List<String> lore) {
        if (!normalLeftClick || !playerHead || !topContainerSlot || !isPetsMenu(title)) return false;
        return lore.stream().map(line -> plain(line).toLowerCase(Locale.ROOT))
                .anyMatch(line -> line.matches("(?:left[- ]click|click)\\s+to\\s+summon(?: this pet| your pet)?[!.]?"));
    }

    public void selected(long now) {
        armed = true;
        selectedAt = now;
    }

    public boolean shouldSuppress(String title, long now) {
        if (!armed) return false;
        long elapsed = now - selectedAt;
        if (elapsed < 0 || elapsed >= SUPPRESSION_NS) {
            reset();
            return false;
        }
        return isPetsMenu(title);
    }

    public void reset() { armed = false; }
}
