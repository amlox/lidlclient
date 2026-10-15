package net.lidl.lidlclient.PartyCMDS;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PartyCommands implements ClientModInitializer {


    private static String getRandomSymbol() {

        String[] symbols = {
                "<3",
                ":star:",
                ":yes:",
                ":no:",
                ":java:",
                ":arrow:"
        };

        int index = ThreadLocalRandom.current()
                .nextInt(symbols.length);

        return symbols[index];
    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger("lidlclient-party");

    // Time between automatic responses
    // Delay after detecting a command (0.3 seconds)
    private static final long RESPONSE_DELAY = 300;

    // Minimum time between outgoing responses (1 second)
    private static final long SEND_COOLDOWN = 1000;

    private static long lastSent = 0;
    private static String pendingMessage = null;
    private static long pendingSince = 0;

    // Extract player messages from Hypixel-style party chat
    private static final Pattern PARTY_PATTERN = Pattern.compile(
            "^Party\\s*>\\s*(?:\\[[^]]+\\]\\s*)?([A-Za-z0-9_]{1,16}):\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public void onInitializeClient() {

        LOGGER.info("Party Commands initialized!");

        // Listen to server chat
        ClientReceiveMessageEvents.GAME.register(
                (message, overlay) -> {

                    if (overlay) return;

                    String text = message.getString()
                            .replaceAll("(?i)§[0-9a-fk-or]", "");

                    // debug, delete after
                    LOGGER.info("[GAME CHAT] {}", text);
                    // Find party messages
                    Matcher matcher = PARTY_PATTERN.matcher(text);

                    if (!matcher.matches()) return;

                    // Extract the player's username
                    String playerName = matcher.group(1);
                    // Extract the actual message
                    String partyMessage = matcher.group(2).trim();
                    // Generate a response
                    String response = handleCommand(partyMessage, playerName);

                    // Ignore unknown commands
                    if (response == null) return;

                    // Only queue one response at a time
                    if (pendingMessage != null) {
                        LOGGER.info("Another response is already queued.");
                        return;
                    }

                    // Queue the response
                    pendingMessage = response;
                    pendingSince = System.currentTimeMillis();

                    LOGGER.info("Queued party response: {}", response);
                    // Generate a response
                    // Only allow one pending response at a time
                    if (pendingMessage != null) {
                        LOGGER.info("Another party response is already queued.");
                        return;
                    }

                    // Queue the response instead of sending immediately
                    pendingMessage = response;
                    pendingSince = System.currentTimeMillis();

                    LOGGER.info("Queued party response: {}", response);
                }
        );
        // Debug incoming player chat
        ClientReceiveMessageEvents.CHAT.register(
                (message, signedMessage, sender, params, timestamp) -> {

                    String text = message.getString();

                    LOGGER.info("[PLAYER CHAT] {}", text);
                }
        );
        /* Local testing command
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> {

                    dispatcher.register(
                            ClientCommands.literal("partytest")
                                    .executes(context -> {

                                        String response = handleCommand("!dice");

                                        context.getSource().sendFeedback(
                                                Component.literal(
                                                        "[Party Test] " + response
                                                )
                                        );

                                        return 1;
                                    })
                    );
                }
        );*/
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            // Nothing waiting to be sent
            if (pendingMessage == null) return;

            // Don't send while disconnected
            if (client.getConnection() == null) {
                pendingMessage = null;
                return;
            }

            long now = System.currentTimeMillis();

            // Wait for the initial response delay
            if (now - pendingSince < RESPONSE_DELAY) return;

            // Respect the cooldown between messages
            if (lastSent != 0 && now - lastSent < SEND_COOLDOWN) return;

            // Send our queued response
            sendPartyMessage(pendingMessage);

            lastSent = now;
            pendingMessage = null;
        });
    }

    // ========================================
    // COMMAND HANDLER
    // ========================================

    private static String handleCommand(String message, String playerName) {

        String[] parts = message.trim().split("\\s+", 2);

        String command = parts[0].toLowerCase(Locale.ROOT);
        String args = parts.length > 1 ? parts[1].trim() : "";

        return switch (command) {

            case "!iq" -> {
                int roll = ThreadLocalRandom.current().nextInt(-100, 201);
                yield playerName + " has " + roll + " IQ!";
            }

            case "!femboy" -> {
                if (playerName.equalsIgnoreCase("syntocxleftfoot")) {
                    yield playerName + " is 100% a femboy!";
                }
                int roll = ThreadLocalRandom.current().nextInt(1, 101);
                yield playerName + " is " + roll + "% a femboy!";
            }

            case "!jew" -> {
                int roll = ThreadLocalRandom.current().nextInt(1, 101);
                yield playerName + " is " + roll + "% a jew!";
            }

            case "!autism" -> {
                int roll = ThreadLocalRandom.current().nextInt(1, 101);
                yield playerName + " is " + roll + "% autistic!";
            }

            case "!white" -> {
                int roll = ThreadLocalRandom.current().nextInt(1, 101);
                yield playerName + " is " + roll + "% white!";
            }

            case "!mexican" -> {
                int roll = ThreadLocalRandom.current().nextInt(1, 101);
                yield playerName + " is " + roll + "% mexican!";
            }

            case "!dice" -> {
                int roll = ThreadLocalRandom.current().nextInt(1, 7);
                yield "You rolled a " + roll + "!";
            }

            case "!coinflip" -> {
                boolean heads = ThreadLocalRandom.current().nextBoolean();
                yield heads ? "Coinflip: Heads!" : "Coinflip: Tails!";
            }

            case "!tesco" -> {

                if (args.isEmpty()) {
                    yield "Usage: !tesco <question>";
                }

                String[] answers = {
                        "fallamatugang",
                        "i dont like em chocolate colored people",
                        "they should add ｓｌａｖｅｓ to skyblock",
                        "i blame the chocolate people",
                        "grubrbi hand",
                        "just cvm in me",
                        "grubberberg tail",
                        "i dont like the illegals",
                        "doctor house can get it",
                        "doctor house is gey",
                        "why do i look like a horse",
                        "jew",
                        "whos walking, im RUNNING",
                        "i have so many ｓｌａｖｅｓ",
                        "im beating my dead frozen baby with my big black veiny dihh",
                        "i love substance abuse",
                        "I got head from my hamster",
                        "take off your clothes",
                        "circle of penetration",
                        "cousins by chance lovers by choice",
                        "wall breaker left",
                        "okay garmin video schrieben",
                        "schriepen",
                        "i dislocated my toe"
                };

                int index = ThreadLocalRandom.current()
                        .nextInt(answers.length);

                yield "[MVP+] Tesco_Manager: " + answers[index];
            }

            case "!slots" -> {

                String slot1 = getRandomSymbol();
                String slot2 = getRandomSymbol();
                String slot3 = getRandomSymbol();

                String result = "Rolled [ " +
                        slot1 + " | " +
                        slot2 + " | " +
                        slot3 + " ]";

                // All three symbols match = JACKPOT!
                if (slot1.equals(slot2) && slot2.equals(slot3)) {
                    result += ":star: JACKPOT! :star:";
                }

                yield result;
            }

            case "!help" -> "Commands: !help, !dice, !coinflip, !tesco, !jew, !autism";
            //when x times !help call retarded
            // Unknown command
            default -> null;
        };
    }

    // ========================================
    // PARTY MESSAGE SENDER
    // ========================================

    private static void sendPartyMessage(String message) {

        Minecraft client = Minecraft.getInstance();

        client.execute(() -> {

            if (client.getConnection() == null) return;

            LOGGER.info("Sending party message: {}", message);

            // Sends /pc <message>
            client.getConnection().sendCommand("pc " + message);
        });
    }
}
