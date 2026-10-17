package net.lidl.lidlclient.PartyCMDS;

import net.fabricmc.api.ClientModInitializer;
import java.util.Map;
import net.lidl.lidlclient.config.ClientConfig;
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
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import java.util.ArrayList;
import java.util.List;





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


    private static final PartyCommandSwitches SWITCHES = new PartyCommandSwitches();
    private static boolean settingsLoaded;
    private static String pendingCommand;

    private static void loadSettings() {
        if (settingsLoaded) return;
        settingsLoaded = true;
        ClientConfig.migrateLegacy("party_commands", "lidlclient-party-commands.json", Map.of("enabled", "enabled"));
        SWITCHES.setEnabled(ClientConfig.bool("party_commands", "enabled", true));
        PartyCommandSwitches.COMMANDS.forEach(command ->
                SWITCHES.set(command, ClientConfig.bool("party_commands", command, true)));
    }

    private static void saveSettings() {
        ClientConfig.set("party_commands", "enabled", SWITCHES.isEnabled());
        SWITCHES.snapshot().forEach((command, value) -> ClientConfig.set("party_commands", command, value));
    }

    public static List<String> commandNames() { return PartyCommandSwitches.COMMANDS; }
    public static boolean isEnabled() { loadSettings(); return SWITCHES.isEnabled(); }
    public static boolean isCommandConfigured(String command) { loadSettings(); return SWITCHES.configured(command); }
    public static boolean isCommandEnabled(String command) { loadSettings(); return SWITCHES.allows(command); }

    public static void setEnabled(boolean value) {
        loadSettings();
        SWITCHES.setEnabled(value);
        if (!value) { pendingMessage = null; pendingCommand = null; }
        saveSettings();
    }

    public static void setCommandEnabled(String command, boolean value) {
        loadSettings();
        SWITCHES.set(command, value);
        if (!value && command.equalsIgnoreCase(pendingCommand)) {
            pendingMessage = null;
            pendingCommand = null;
        }
        saveSettings();
    }

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

        loadSettings();
        LOGGER.info("Party Commands initialized!");
        initializeBanCommand();

        // Listen to server chat
        ClientReceiveMessageEvents.GAME.register(
                (message, overlay) -> {

                    if (overlay || !isEnabled()) return;

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
                    pendingCommand = PartyCommandSwitches.commandName(partyMessage);
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

            if (pendingCommand == null || !isCommandEnabled(pendingCommand)) {
                pendingMessage = null;
                pendingCommand = null;
                return;
            }

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
            sendPartyMessage(pendingMessage, pendingCommand);

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

        if (!isCommandEnabled(command)) return null;

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
                        "i dislocated my toe",
                        "his dih pattern is recognizable",
                        "i only have jerk mate premium",
                        "rocks are made of rocks",
                        "ive actually gooned to a penguin before",
                        "that rock over is actually my favourite. \nit reminds me of the penguin",
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

            case "!help" -> "Commands: " + String.join(", ", SWITCHES.activeCommands());
            //when x times !help call retarded
            // Unknown command
            default -> null;
        };
    }

    // ========================================
    // PARTY MESSAGE SENDER
    // ========================================

    private static void sendPartyMessage(String message, String command) {

        Minecraft client = Minecraft.getInstance();

        client.execute(() -> {

            if (client.getConnection() == null || !isCommandEnabled(command)) return;

            LOGGER.info("Sending party message: {}", message);

            // Sends /pc <message>
            client.getConnection().sendCommand("pc " + message);
        });
    }


    private ClientPacketListener pendingConnection;
    private boolean preview;

    private static boolean isHypixel(Minecraft client) {
        var server = client.getCurrentServer();
        if (server == null) return false;
        String host = server.ip.toLowerCase(Locale.ROOT).split(":", 2)[0];
        return host.equals("hypixel.net") || host.endsWith(".hypixel.net");
    }

    private void receiveCommand(Component message) {
        Minecraft client = Minecraft.getInstance();
        if (!isCommandEnabled("!ban") || client.player == null || !isHypixel(client)
                || pendingConnection != null || !ChatBanTrigger.matches(message.getString())) return;
        pendingConnection = client.getConnection();
        preview = false;
        LOGGER.info("Chat ban prank received; disconnecting on this tick.");
    }

    private void initializeBanCommand() {
        LOGGER.info("Chat ban prank initialized; use /puzzleprank test to preview.");
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("puzzleprank")
                        .then(ClientCommands.literal("test").executes(context -> {
                            if (!isCommandEnabled("!ban")) {
                                context.getSource().sendError(Component.literal("!ban is disabled in /lidl > PartyCMDS."));
                                return 0;
                            }
                            Minecraft client = Minecraft.getInstance();
                            pendingConnection = client.getConnection();
                            preview = true;
                            context.getSource().sendFeedback(Component.literal(
                                    "[LidlClient] Fake ban test: disconnecting now."));
                            return 1;
                        }))));

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) receiveCommand(message);
        });
        ClientReceiveMessageEvents.CHAT.register(
                (message, signedMessage, sender, params, timestamp) -> receiveCommand(message));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingConnection == null) return;
            if (!isCommandEnabled("!ban") || client.getConnection() != pendingConnection || client.player == null
                    || (!preview && !isHypixel(client))) {
                pendingConnection = null;
                return;
            }
            pendingConnection = null;
            preview = false;
            client.disconnect(new FakeBanScreen(), false);
        });
    }

    /** Only exact !ban messages in the supported Hypixel chat channels can trigger. */
    static final class ChatBanTrigger {
        private static final String PLAYER = "(?:\\[[^]\\r\\n]+]\\s*)*[A-Za-z0-9_]{1,16}";
        private static final Pattern COMMAND = Pattern.compile(
                "^(?:(?:Party|Co-op|Coop)\\s*>\\s*|(?:From|To)\\s+)"
                        + PLAYER + ":\\s*!ban\\s*$",
                Pattern.CASE_INSENSITIVE);
    
        private ChatBanTrigger() { }
    
        public static boolean matches(String message) {
            String plain = message.replaceAll("(?i)§[0-9A-FK-ORX]", "")
                    .replace('\u00a0', ' ').strip();
            return COMMAND.matcher(plain).matches();
        }
    }
    
    /** A local imitation of the legacy Hypixel ban screen, shown after disconnecting. */
    private static final class FakeBanScreen extends Screen {
        private static final Identifier DIRT = Identifier.withDefaultNamespace("textures/block/dirt.png");
        private final List<FormattedCharSequence> lines = new ArrayList<>();
        private int textTop;
    
        public FakeBanScreen() {
            super(Component.literal("Failed to connect to the server"));
        }
    
        @Override
        protected void init() {
            lines.clear();
            add(Component.literal("Failed to connect to the server").withStyle(ChatFormatting.GRAY));
            add(Component.empty());
            add(Component.literal("You are permanently banned from this server!").withStyle(ChatFormatting.RED));
            add(Component.empty());
            add(Component.literal("Reason: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("Cheating through the use of unfair game advantages.").withStyle(ChatFormatting.WHITE)));
            add(Component.literal("Find out more: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("https://www.hypixel.net/appeal")
                            .withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE)));
            add(Component.empty());
            add(Component.literal("Ban ID: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("#F9DF549F").withStyle(ChatFormatting.WHITE)));
            add(Component.literal("Sharing your Ban ID may affect the processing of your appeal!").withStyle(ChatFormatting.GRAY));
            int blockHeight = lines.size() * font.lineHeight;
            textTop = Math.max(8, height / 2 - blockHeight / 2 - 20);
            int buttonWidth = Math.min(200, Math.max(1, width - 20));
            addRenderableWidget(Button.builder(Component.literal("Back to server list"), button -> onClose())
                    .bounds((width - buttonWidth) / 2,
                            Math.min(height - 28, textTop + blockHeight + 20), buttonWidth, 20).build());
        }
    
        private void add(Component text) {
            if (text.getString().isEmpty()) lines.add(Component.empty().getVisualOrderText());
            else lines.addAll(font.split(text, Math.max(1, width - 32)));
        }
    
        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, DIRT, 0, 0, 0.0f, 0.0f, width, height, 32, 32);
            graphics.fill(0, 0, width, height, 0xC0000000);
            for (int i = 0; i < lines.size(); i++) {
                FormattedCharSequence line = lines.get(i);
                graphics.text(font, line, (width - font.width(line)) / 2,
                        textTop + i * font.lineHeight, 0xFFFFFFFF, true);
            }
            super.extractRenderState(graphics, mouseX, mouseY, delta);
        }
    
        @Override
        public void onClose() {
            Minecraft.getInstance().setScreen(new JoinMultiplayerScreen(new TitleScreen()));
        }
    
        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
    
}
