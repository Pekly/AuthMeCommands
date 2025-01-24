package me.pekly.authmecommands;

import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.service.GeoIpService;
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

    // Variables to track the last time the commands/messages were executed
    private Map<String, Long> lastCommandTimes;
    private long commandCooldown;
    private long messageCooldown;

    // Configuration variables
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

    private AuthMeApi authMeApi;

    @Override
    public void onEnable() {
        lastCommandTimes = new HashMap<>();
        saveDefaultConfig();
        loadConfig();

        // Initialize AuthMeApi instance
        AuthMe authMePlugin = (AuthMe) Bukkit.getPluginManager().getPlugin("AuthMe");  // Get the AuthMe plugin instance
        if (authMePlugin != null) {
            authMeApi = new AuthMeApi(authMePlugin, authMePlugin.getDataSource(), authMePlugin.getPlayerCache(), authMePlugin.getPasswordSecurity(), 
                    authMePlugin.getManagement(), authMePlugin.getValidationService(), authMePlugin.getGeoIpService());
        } else {
            getLogger().warning("AuthMe plugin not found!");
        }

        // Register events
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Any cleanup if necessary
    }

    // Method to load configuration settings
    private void loadConfig() {
        FileConfiguration config = getConfig();

        // Load commands
        commandsOnLogin = config.getStringList("commands-on-login").toArray(new String[0]);
        commandsOnRegister = config.getStringList("commands-on-register").toArray(new String[0]);

        // Load messages
        loginMessages = config.getStringList("login-messages").toArray(new String[0]);
        registerMessages = config.getStringList("register-messages").toArray(new String[0]);

        // Load booleans for enabling/disabling features
        sendLoginMessages = config.getBoolean("send-login-messages", true);
        sendRegisterMessages = config.getBoolean("send-register-messages", true);
        runLoginCommands = config.getBoolean("run-login-commands", true);
        runRegisterCommands = config.getBoolean("run-register-commands", true);

        // Load delays and cooldowns
        commandDelay = config.getLong("command-delay", 1000);
        messageDelay = config.getLong("message-delay", 1000);
        commandCooldown = config.getLong("command-cooldown", 60000);
        messageCooldown = config.getLong("message-cooldown", 60000);
    }

    // Event handler when a player joins
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check if the player is registered
        if (authMeApi.isRegistered(player.getName())) {
            if (authMeApi.isAuthenticated(player)) {
                // If logged in, run login commands and send login messages
                if (runLoginCommands) {
                    runCommands(player, commandsOnLogin);
                }
                if (sendLoginMessages) {
                    sendMessages(player, loginMessages);
                }
            } else {
                // If not logged in yet, run registration commands and send registration messages
                if (runRegisterCommands) {
                    runCommands(player, commandsOnRegister);
                }
                if (sendRegisterMessages) {
                    sendMessages(player, registerMessages);
                }
            }
        }
    }

    // Method to run commands with a delay and cooldown check
    private void runCommands(Player player, String[] commands) {
        long currentTime = System.currentTimeMillis();
        String playerName = player.getName();

        // Check if the command cooldown has passed
        if (lastCommandTimes.containsKey(playerName) && (currentTime - lastCommandTimes.get(playerName)) < commandCooldown) {
            long timeLeft = (commandCooldown - (currentTime - lastCommandTimes.get(playerName))) / 1000;
            player.sendMessage("§cYou need to wait " + timeLeft + " more seconds to use this command again.");
            return;
        }

        // Run the commands with a delay
        for (String command : commands) {
            String commandToRun = command.replace("{player}", playerName); // Replace {player} with the player's name
            new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), commandToRun);
                }
            }.runTaskLater(this, commandDelay / 50); // Delay in ticks (50 ticks = 1 second)
        }

        lastCommandTimes.put(playerName, currentTime); // Update last command time
    }

    // Method to send messages with a delay and cooldown check
    private void sendMessages(Player player, String[] messages) {
        long currentTime = System.currentTimeMillis();
        String playerName = player.getName();

        // Check if the message cooldown has passed
        if (currentTime - lastCommandTimes.getOrDefault(playerName, 0L) < messageCooldown) {
            long timeLeft = (messageCooldown - (currentTime - lastCommandTimes.get(playerName))) / 1000;
            player.sendMessage("§cYou need to wait " + timeLeft + " more seconds to receive another message.");
            return;
        }

        // Send messages with a delay
        for (String message : messages) {
            String finalMessage = message.replace("{player}", playerName); // Replace {player} with the player's name
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.sendMessage(finalMessage);
                }
            }.runTaskLater(this, messageDelay / 50); // Delay in ticks (50 ticks = 1 second)
        }

        lastCommandTimes.put(playerName, currentTime); // Update last message time
    }
}
