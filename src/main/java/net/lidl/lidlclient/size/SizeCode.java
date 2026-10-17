package net.lidl.lidlclient.size;

import java.util.UUID;

/** Versioned, portable visual-size preset. No code from chat is executed. */
public record SizeCode(UUID player, double scale) {
    public static final double MIN = 0.25;
    public static final double MAX = 3.0;

    public SizeCode {
        if (player == null || !Double.isFinite(scale) || scale < MIN || scale > MAX) {
            throw new IllegalArgumentException("Size must be between 0.25 and 3.");
        }
    }

    public String encode() {
        return "LIDL-SIZE:1:" + player + ":" + scale;
    }

    public static SizeCode decode(String code) {
        try {
            String[] parts = code.strip().split(":", -1);
            if (parts.length != 4 || !parts[0].equals("LIDL-SIZE") || !parts[1].equals("1")) {
                throw new IllegalArgumentException();
            }
            UUID player = UUID.fromString(parts[2]);
            if (!player.toString().equalsIgnoreCase(parts[2])) throw new IllegalArgumentException();
            return new SizeCode(player, Double.parseDouble(parts[3]));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid size code. Paste the full LIDL-SIZE:1:... code.");
        }
    }
}
