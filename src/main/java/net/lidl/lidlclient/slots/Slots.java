package net.lidl.lidlclient.slots;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.Identifier;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class Slots implements ClientModInitializer {

    // Controlled by the LidlClient GUI. Enabled by default.
    private static boolean enabled = true;

    public static void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;

        if (!enabled) {
            autoRoll = false;
            autoRollWait = -1;
            // Cancel an unfinished spin without taking the player's virtual bet.
            if (rolling) {
                points = safeAdd(points, currentBet);
                rolling = false;
                rollTick = 0;
                for (int i = 0; i < reels.length; i++) reels[i] = -1;
            }
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }


    // ========================================
    // SETTINGS
    // ========================================

    private static boolean showUi = false;
    private static boolean muteMessages = true;
    // Settings controlled from the Slots dropdown in /lidl.
    public static boolean isMuteMessages() {
        return muteMessages;
    }

    public static void setMuteMessages(boolean value) {
        muteMessages = value;
    }

    public static boolean isShowUi() {
        return showUi;
    }

    public static void setShowUi(boolean value) {
        showUi = value;
    }

    private static final boolean FAST_ROLL = true;
    private static final float FONT_SCALE = 3.0f;

    private static final int MIN_BET = 10;
    private static final int[] STOP_TIMES = {40, 60, 80};

    // ========================================
    // SYMBOLS
    // ========================================

    private static final String[] SYMBOLS = {
            "❤", "✮", "☕", "➜", "♦",
            "7", "♛", "☂", "●"
    };

    private static final ChatFormatting[] COLORS = {
            ChatFormatting.RED,
            ChatFormatting.YELLOW,
            ChatFormatting.GOLD,
            ChatFormatting.AQUA,
            ChatFormatting.AQUA,
            ChatFormatting.DARK_RED,
            ChatFormatting.LIGHT_PURPLE,
            ChatFormatting.DARK_AQUA,
            ChatFormatting.BLUE
    };

    // 5 = number seven, our special symbol
    private static final int SEVEN = 5;

    // ========================================
    // STATE
    // ========================================

    private static long points = 1000;
    private static long currentBet = 0;

    private static long totalSpins = 0;
    private static long totalBet = 0;
    private static long totalWinnings = 0;
    private static long totalLosses = 0;

    private static boolean autoRoll = false;
    private static boolean rolling = false;

    private static int rollTick = 0;
    private static int autoRollWait = -1;

    private static final int[] reels = {-1, -1, -1};

    // ========================================
    // INITIALIZATION
    // ========================================

    @Override
    public void onInitializeClient() {

        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> {

                    // /slots <bet>
                    dispatcher.register(
                            ClientCommands.literal("slots")
                                    .executes(ctx -> {
                                        msg("Usage: /slots <bet>", ChatFormatting.YELLOW);
                                        return 1;
                                    })
                                    .then(ClientCommands.argument(
                                            "bet", StringArgumentType.word()
                                    ).executes(ctx -> {
                                        placeBet(StringArgumentType.getString(ctx, "bet"));
                                        return 1;
                                    }))
                    );

                    // /gamble <bet>
                    dispatcher.register(
                            ClientCommands.literal("gamble")
                                    .executes(ctx -> {
                                        msg("Usage: /gamble <bet>", ChatFormatting.YELLOW);
                                        return 1;
                                    })
                                    .then(ClientCommands.argument(
                                            "bet", StringArgumentType.word()
                                    ).executes(ctx -> {
                                        placeBet(StringArgumentType.getString(ctx, "bet"));
                                        return 1;
                                    }))
                    );

                    // /autoroll
                    dispatcher.register(
                            ClientCommands.literal("autoroll")
                                    .executes(ctx -> {
                                        toggleAutoRoll();
                                        return 1;
                                    })
                    );

                    // /money
                    dispatcher.register(
                            ClientCommands.literal("money")
                                    .executes(ctx -> {
                                        msg("Your balance: $" + points, ChatFormatting.GOLD);
                                        return 1;
                                    })
                    );

                    // /addmoney <amount>
                    dispatcher.register(
                            ClientCommands.literal("addmoney")
                                    .executes(ctx -> {
                                        msg("Usage: /addmoney <amount>", ChatFormatting.YELLOW);
                                        return 1;
                                    })
                                    .then(ClientCommands.argument(
                                            "amount", StringArgumentType.word()
                                    ).executes(ctx -> {
                                        addMoney(StringArgumentType.getString(ctx, "amount"));
                                        return 1;
                                    }))
                    );

                    // /slotstats
                    dispatcher.register(
                            ClientCommands.literal("slotstats")
                                    .executes(ctx -> {
                                        showStats();
                                        return 1;
                                    })
                    );
                }
        );

        ClientTickEvents.END_CLIENT_TICK.register(Slots::tick);

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("lidlclient", "slots_hud"),
                Slots::render
        );
    }

    // ========================================
    // BETTING
    // ========================================

    private static void placeBet(String input) {

        if (!enabled) {
            msg("Slots is disabled. Enable it in /lidl.", ChatFormatting.RED);
            return;
        }

        if (rolling) {
            msg("Already rolling!", ChatFormatting.RED);
            return;
        }

        input = input.toLowerCase(Locale.ROOT).replace("$", "");

        long amount;

        try {
            if (input.equals("all") || input.equals("allin")) {
                amount = points;
            } else if (input.endsWith("%")) {
                double percent = Double.parseDouble(
                        input.substring(0, input.length() - 1)
                );

                if (!Double.isFinite(percent)
                        || percent <= 0 || percent > 100) {
                    msg("Enter a percentage between 0 and 100.",
                            ChatFormatting.RED);
                    return;
                }

                amount = (long) Math.floor(points * percent / 100.0);
            } else {
                amount = Long.parseLong(input);
            }
        } catch (NumberFormatException e) {
            msg("Invalid bet amount!", ChatFormatting.RED);
            return;
        }

        if (amount < MIN_BET) {
            msg("Minimum bet is $" + MIN_BET, ChatFormatting.RED);
            return;
        }

        if (amount > points) {
            msg("Not enough money! Balance: $" + points,
                    ChatFormatting.RED);
            return;
        }

        // Starting a new bet disables any previous auto-roll
        autoRoll = false;
        currentBet = amount;

        startRolling();
    }

    private static void addMoney(String input) {

        input = input.replace("$", "");

        try {
            long amount = Long.parseLong(input);

            if (amount <= 0 || amount > Long.MAX_VALUE - points) {
                msg("Invalid amount!", ChatFormatting.RED);
                return;
            }

            points += amount;
            msg("Added $" + amount + ". Balance: $" + points,
                    ChatFormatting.GREEN);

        } catch (NumberFormatException e) {
            msg("Invalid amount!", ChatFormatting.RED);
        }
    }

    // ========================================
    // AUTO ROLL
    // ========================================

    private static void toggleAutoRoll() {

        if (!enabled) {
            msg("Slots is disabled. Enable it in /lidl.", ChatFormatting.RED);
            return;
        }

        autoRoll = !autoRoll;

        if (!autoRoll) {
            autoRollWait = -1;
            msg("Auto-roll DISABLED.", ChatFormatting.AQUA);
            return;
        }

        if (currentBet < MIN_BET) {
            autoRoll = false;
            msg("Set a bet with /slots <bet> first.",
                    ChatFormatting.RED);
            return;
        }

        if (points < currentBet) {
            autoRoll = false;
            msg("Not enough money for auto-roll.",
                    ChatFormatting.RED);
            return;
        }

        msg("Auto-roll ENABLED. Betting $" + currentBet,
                ChatFormatting.AQUA);

        if (!rolling) {
            startRolling();
        }
    }

    // ========================================
    // SLOT ANIMATION
    // ========================================

    private static void startRolling() {

        if (rolling || points < currentBet) {
            autoRoll = false;
            return;
        }

        points -= currentBet;

        if (!muteMessages) {
            msg("Betting $" + currentBet +
                            ". Balance: $" + points,
                    ChatFormatting.GREEN);
        }

        rolling = true;
        rollTick = 0;
        autoRollWait = -1;

        for (int i = 0; i < 3; i++) {
            reels[i] = -1;
        }
    }

    private static void tick(Minecraft client) {

        if (!enabled) return;

        if (client.player == null) {
            rolling = false;
            autoRoll = false;
            autoRollWait = -1;
            return;
        }

        // Wait briefly between automatic spins
        if (!rolling) {
            if (autoRoll && autoRollWait >= 0) {
                autoRollWait--;

                if (autoRollWait <= 0) {
                    if (points >= currentBet) {
                        startRolling();
                    } else {
                        autoRoll = false;
                        autoRollWait = -1;
                        msg("Auto-roll stopped: insufficient balance.",
                                ChatFormatting.RED);
                    }
                }
            }
            return;
        }

        rollTick++;

        double multiplier = FAST_ROLL ? 0.3 : 1.0;

        for (int i = 0; i < 3; i++) {
            int stop = (int) (STOP_TIMES[i] * multiplier);

            if (rollTick < stop) {
                reels[i] = randomSymbol();

                // Play a quiet click every 2 ticks
                if (rollTick % 2 == 0) {
                    playSound(1.5f);
                }
            } else if (reels[i] == -1) {
                reels[i] = randomSymbol();
            }
        }

        int lastStop = (int) (STOP_TIMES[2] * multiplier);

        // Result appears 10 ticks after last reel stops
        if (rollTick >= lastStop + 10) {
            rolling = false;
            showResult();
        }
    }

    // ========================================
    // RESULTS
    // ========================================

    private static void showResult() {

        int a = reels[0];
        int b = reels[1];
        int c = reels[2];

        long winnings = 0;

        totalSpins++;
        totalBet += currentBet;

        int sevens = (a == SEVEN ? 1 : 0)
                + (b == SEVEN ? 1 : 0)
                + (c == SEVEN ? 1 : 0);

        if (a == b && b == c) {

            if (a == SEVEN) {
                winnings = safePayout((double) currentBet * currentBet);
                playSound(0.65f);

                msg("777 JACKPOT! You won $" + winnings + "!",
                        ChatFormatting.RED);
            } else {
                winnings = safePayout(
                        currentBet * Math.sqrt(currentBet)
                );
                playSound(1.8f);

                msg("✮ JACKPOT! You won $" + winnings + "!",
                        ChatFormatting.GREEN);
            }

        } else if (sevens == 2) {

            winnings = safePayout(
                    currentBet * Math.sqrt(currentBet)
            );
            playSound(1.3f);

            if (!muteMessages) {
                msg("Lucky double 7! Won $" + winnings,
                        ChatFormatting.GOLD);
            }

        } else if (a == b || b == c || a == c) {

            winnings = safePayout(
                    currentBet * Math.cbrt(currentBet)
            );
            playSound(1.6f);

            if (!muteMessages) {
                msg("Two matches! Won $" + winnings,
                        ChatFormatting.YELLOW);
            }

        } else {
            if (!muteMessages) {
                msg("No matches. Better luck next time!",
                        ChatFormatting.RED);
            }
        }

        if (winnings == 0) {
            totalLosses += currentBet;
        } else {
            totalWinnings += winnings;
        }

        // Prevent signed integer overflow
        points = safeAdd(points, winnings);

        if (!muteMessages) {
            msg("Balance: $" + points, ChatFormatting.GOLD);
        }

        // Small pause before next automatic spin
        if (autoRoll) {
            autoRollWait = 15;
        }
    }

    // ========================================
    // STATS
    // ========================================

    private static void showStats() {

        long net = totalWinnings - totalBet;

        msg("--- Slot Stats ---", ChatFormatting.AQUA);
        msg("Total Spins: " + totalSpins, ChatFormatting.YELLOW);
        msg("Total Bet: $" + totalBet, ChatFormatting.YELLOW);
        msg("Total Winnings: $" + totalWinnings,
                ChatFormatting.GREEN);
        msg("Total Losses: $" + totalLosses,
                ChatFormatting.RED);
        msg("Net Profit/Loss: $" + net, ChatFormatting.GOLD);
    }

    // ========================================
    // HUD - NO BACKGROUND
    // ========================================

    private static void render(
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker
    ) {

        if (!enabled || !rolling) return;

        Minecraft client = Minecraft.getInstance();

        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;

        // Build colored symbol display
        MutableComponent display =
                Component.literal("[ ").withStyle(ChatFormatting.WHITE);

        for (int i = 0; i < 3; i++) {

            if (i > 0) {
                display.append(Component.literal(" | ")
                        .withStyle(ChatFormatting.WHITE));
            }

            int index = reels[i];

            if (index == -1) {
                display.append(Component.literal("?")
                        .withStyle(ChatFormatting.WHITE));
            } else {
                MutableComponent symbol =
                        Component.literal(SYMBOLS[index])
                                .withStyle(COLORS[index]);

                if (index == SEVEN) {
                    symbol.withStyle(ChatFormatting.BOLD);
                }

                display.append(symbol);
            }
        }

        display.append(Component.literal(" ]")
                .withStyle(ChatFormatting.WHITE));

        // Center the symbols
        int textWidth = client.font.width(display);

        float scale = FONT_SCALE;

        graphics.pose().pushMatrix();

        graphics.pose().translate(
                centerX,
                centerY - 67
        );

        graphics.pose().scale(scale, scale);

        graphics.text(
                client.font,
                display,
                -textWidth / 2,
                0,
                0xFFFFFFFF,
                true
        );

        graphics.pose().popMatrix();

        if (showUi) {

            Component bet = Component.literal(
                    "Bet: $" + currentBet
            ).withStyle(ChatFormatting.YELLOW);

            Component balance = Component.literal(
                    "Balance: $" + points
            ).withStyle(ChatFormatting.GOLD);

            graphics.text(
                    client.font,
                    bet,
                    centerX - client.font.width(bet) / 2,
                    centerY - 25,
                    0xFFFFFFFF,
                    true
            );

            graphics.text(
                    client.font,
                    balance,
                    centerX - client.font.width(balance) / 2,
                    centerY + 25,
                    0xFFFFFFFF,
                    true
            );
        }
    }

    // ========================================
    // UTILITIES
    // ========================================

    private static int randomSymbol() {
        return ThreadLocalRandom.current()
                .nextInt(SYMBOLS.length);
    }

    private static long safePayout(double amount) {
        if (!Double.isFinite(amount) || amount < 0) {
            return 0;
        }
        return (long) Math.min(amount, Long.MAX_VALUE);
    }

    private static long safeAdd(long a, long b) {
        if (b > Long.MAX_VALUE - a) {
            return Long.MAX_VALUE;
        }
        return a + b;
    }

    private static void playSound(float pitch) {

        Minecraft client = Minecraft.getInstance();

        client.getSoundManager().play(
                SimpleSoundInstance.forUI(
                        SoundEvents.UI_BUTTON_CLICK,
                        pitch
                )
        );
    }

    private static void msg(String text, ChatFormatting color) {

        Minecraft client = Minecraft.getInstance();

        if (client.player == null) return;

        client.player.sendSystemMessage(
                Component.literal("[Slots] ")
                        .withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(text)
                                .withStyle(color))
        );
    }
}
