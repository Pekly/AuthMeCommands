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

    private Map<String, Long> lastCommandTimes;
    private long commandCooldown;
    private long messageCooldown;

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

    private AuthMeApi authMeApi; // Add AuthMeApi instance

    @Override
    public void onEnable() {
        lastCommandTimes = new HashMap<>();
        saveDefaultConfig();
        loadConfig();

        // Initialize AuthMeApi instance
        authMeApi = new AuthMeApi();

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Cleanup if necessary
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();

        commandsOnLogin = config.getStringList("commands-on-login").toArray(new String[0]);
        commandsOnRegister = config.getStringList("commands-on-register").toArray(new String[0]);

        loginMessages = config.getStringList("login-messages").toArray(new String[0]);
        registerMessages = config.getStringList("register-messages").toArray(new String[0]);

        sendLoginMessages = config.getBoolean("send-login-messages", true);
        sendRegisterMessages = config.getBoolean("send-register-messages", true);
        runLoginCommands = config.getBoolean("run-login-commands", true);
        runRegisterCommands = config.getBoolean("run-register-commands", true);

        commandDelay = config.getLong("command-delay", 1000);
        messageDelay = config.getLong("message-delay", 1000);

        commandCooldown = config.getLong("command-cooldown", 60000);
        messageCooldown = config.getLong("message-cooldown", 60000);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (authMeApi.isRegistered(player)) {
            if (authMeApi.isAuthenticated(player)) {
                if (runLoginCommands) {
                    runCommands(player, commandsOnLogin);
                }
                if (sendLoginMessages) {
                    sendMessages(player, loginMessages);
                }
            } else {
                if (runRegisterCommands) {
                    runCommands(player, commandsOnRegister);
                }
                if (sendRegisterMessages) {
                    sendMessages(player, registerMessages);
                }
            }
        }
    }

    private void runCommands(Player player, String[] commands) {
        long currentTime = System.currentTimeMillis();
        String playerName = player.getName();

        if (lastCommandTimes.containsKey(playerName) && (currentTime - lastCommandTimes.get(playerName)) < commandCooldown) {
            long timeLeft = (commandCooldown - (currentTime - lastCommandTimes.get(playerName))) / 1000;
            player.sendMessage("§cYou need to wait " + timeLeft + " more seconds to use this command again.");
            return;
        }

        for (String command : commands) {
            String commandToRun = command.replace("{player}", playerName); // Use playerName here, not player
            new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), commandToRun);
                }
            }.runTaskLater(this, commandDelay / 50);
        }

        lastCommandTimes.put(playerName, currentTime);
    }

    private void sendMessages(Player player, String[] messages) {
        long currentTime = System.currentTimeMillis();
        String playerName = player.getName();

        if (currentTime - lastCommandTimes.getOrDefault(playerName, 0L) < messageCooldown) {
            long timeLeft = (messageCooldown - (currentTime - lastCommandTimes.get(playerName))) / 1000;
            player.sendMessage("§cYou need to wait " + timeLeft + " more seconds to receive another message.");
            return;
        }

        for (String message : messages) {
            String finalMessage = message.replace("{player}", playerName);
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.sendMessage(finalMessage);
                }
            }.runTaskLater(this, messageDelay / 50);
        }

        lastCommandTimes.put(playerName, currentTime);
    }
}
