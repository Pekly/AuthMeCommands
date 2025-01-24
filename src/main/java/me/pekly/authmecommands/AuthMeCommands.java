package me.pekly.authmecommands;

import fr.xephi.authme.api.v3.AuthMeApi;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthMeCommands extends JavaPlugin implements Listener {

    // Maps to track last command time for each player to enforce cooldown
    private Map<String, Long> lastCommandTimes;
    private long commandCooldown;
    private long messageCooldown;

    // Variables to hold configuration data
    private String[] commandsOnLogin;
    private String[] commandsOnRegister;
    private String[] loginMessages;
    private String[] registerMessages;
    private boolean sendLoginMessages;
    private boolean sendRegisterMessages;
    private boolean runLoginCommands;
    private boolean runRegisterCommands;
    private long commandDelay;
    private long messageDelay;

    @Override
    public void onEnable() {
        // Initialize variables
        lastCommandTimes = new HashMap<>();
        saveDefaultConfig();  // Ensure the config file exists
        loadConfig();         // Load the plugin configuration

        // Register the event listener for player joins
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Any cleanup if needed (currently not used)
    }

    // Load the configuration from config.yml
    private void loadConfig() {
        FileConfiguration config = getConfig();

        // Load the list of commands for login and registration
        commandsOnLogin = config.getStringList("commands-on-login").toArray(new String[0]);
        commandsOnRegister = config.getStringList("commands-on-register").toArray(new String[0]);

        // Load the messages to send upon login and registration
        loginMessages = config.getStringList("login-messages").toArray(new String[0]);
        registerMessages = config.getStringList("register-messages").toArray(new String[0]);

        // Get configuration flags to enable/disable command and message sending
        sendLoginMessages = config.getBoolean("send-login-messages", true);
        sendRegisterMessages = config.getBoolean("send-register-messages", true);
        runLoginCommands = config.getBoolean("run-login-commands", true);
        runRegisterCommands = config.getBoolean("run-register-commands", true);

        // Get delay for commands and messages in milliseconds
        commandDelay = config.getLong("command-delay", 1000);
        messageDelay = config.getLong("message-delay", 1000);

        // Get cooldown times for commands and messages in milliseconds
        commandCooldown = config.getLong("command-cooldown", 60000);  // Default to 60 seconds
        messageCooldown = config.getLong("message-cooldown", 60000);  // Default to 60 seconds
    }

    // Event handler for when a player joins the server
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check if the player is registered and logged in
        if (AuthMeApi.isRegistered(player)) {
            if (AuthMeApi.isAuthenticated(player)) {
                // Player is logged in, run login commands and send login messages
                if (runLoginCommands) {
                    runCommands(player, commandsOnLogin);
                }
                if (sendLoginMessages) {
                    sendMessages(player, loginMessages);
                }
            } else {
                // Player is registered but not logged in, run register commands and send register messages
                if (runRegisterCommands) {
                    runCommands(player, commandsOnRegister);
                }
                if (sendRegisterMessages) {
                    sendMessages(player, registerMessages);
                }
            }
        }
    }

    // Method to run the configured commands
    private void runCommands(Player player, String[] commands) {
        long currentTime = System.currentTimeMillis();
        String playerName = player.getName();

        // Check if cooldown period has passed for the player to execute the command
        if (lastCommandTimes.containsKey(playerName) && (currentTime - lastCommandTimes.get(playerName)) < commandCooldown) {
            long timeLeft = (commandCooldown - (currentTime - lastCommandTimes.get(playerName))) / 1000; // Time left in seconds
            player.sendMessage("§cYou need to wait " + timeLeft + " more seconds to use this command again.");
            return; // Skip running commands if cooldown hasn't passed
        }

        // Run each command with a delay
        for (String command : commands) {
            String commandToRun = command.replace("{player}", player.getName()); // Replace {player} with player's name
            new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), commandToRun);
                }
            }.runTaskLater(this, commandDelay / 50); // Delay in ticks (1 second = 20 ticks)
        }

        // Update the last command time for the player
        lastCommandTimes.put(playerName, currentTime);
    }

    // Method to send messages to the player
    private void sendMessages(Player player, String[] messages) {
        long currentTime = System.currentTimeMillis();
        String playerName = player.getName();

        // Ensure the message cooldown is respected
        if (currentTime - lastCommandTimes.getOrDefault(playerName, 0L) < messageCooldown) {
            long timeLeft = (messageCooldown - (currentTime - lastCommandTimes.get(playerName))) / 1000; // Time left in seconds
            player.sendMessage("§cYou need to wait " + timeLeft + " more seconds to receive another message.");
            return;
        }

        // Send each message with delay
        for (String message : messages) {
            String finalMessage = message.replace("{player}", player.getName());
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.sendMessage(finalMessage);  // Send the final message to the player
                }
            }.runTaskLater(this, messageDelay / 50); // Delay in ticks (1 second = 20 ticks)
        }

        // Update the last message time for the player
        lastCommandTimes.put(playerName, currentTime);
    }
}
