package com.oheers.fish;

import com.oheers.fish.competition.AutoRunner;
import com.oheers.fish.competition.Competition;
import com.oheers.fish.competition.JoinChecker;
import com.oheers.fish.competition.reward.LoadRewards;
import com.oheers.fish.competition.reward.Reward;
import com.oheers.fish.config.FishFile;
import com.oheers.fish.config.MainConfig;
import com.oheers.fish.config.RaritiesFile;
import com.oheers.fish.config.messages.MessageFile;
import com.oheers.fish.config.messages.Messages;
import com.oheers.fish.database.Database;
import com.oheers.fish.fishing.FishEvent;
import com.oheers.fish.fishing.items.Fish;
import com.oheers.fish.fishing.items.Names;
import com.oheers.fish.fishing.items.Rarity;
import com.oheers.fish.selling.GUICache;
import com.oheers.fish.selling.InteractHandler;
import com.oheers.fish.selling.SellGUI;
import com.tchristofferson.configupdater.ConfigUpdater;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public class EvenMoreFish extends JavaPlugin {

    public static FishFile fishFile;
    public static RaritiesFile raritiesFile;
    public static MessageFile messageFile;

    public static Messages msgs;
    public static MainConfig mainConfig;

    public static Permission permission = null;
    public static Economy econ = null;

    public static Map<Integer, Set<String>> fish = new HashMap<>();

    public static Map<Rarity, List<Fish>> fishCollection = new HashMap<>();
    public static Map<Integer, List<Reward>> rewards = new HashMap<>();
    // ^ <Position in competition, list of rewards to be given>

    public static Competition active;

    public static ArrayList<SellGUI> guis;

    public static boolean isUpdateAvailable;
    private final int MSG_CONFIG_VERSION = 2;
    private final int MAIN_CONFIG_VERSION = 2;

    public void onEnable() {

        fishFile = new FishFile(this);
        raritiesFile = new RaritiesFile(this);
        messageFile = new MessageFile(this);

        msgs = new Messages();
        mainConfig = new MainConfig();

        listeners();
        commands();

        // could not setup permissions.
        if (!setupPermissions()) {
            Bukkit.getServer().getLogger().log(Level.SEVERE, "EvenMoreFish couldn't hook into Vault permissions. Disabling to prevent serious problems.");
            getServer().getPluginManager().disablePlugin(this);
        }

        if (!setupEconomy()) {
            Bukkit.getLogger().log(Level.SEVERE, "EvenMoreFish couldn't hook into Vault economy. Disabling to prevent serious problems.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // async check for updates on the spigot page
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            checkUpdate();
            checkConfigVers();
        });

        Names names = new Names();
        names.loadRarities();

        LoadRewards.load();

        getConfig().options().copyDefaults();
        saveDefaultConfig();

        Help.loadValues();

        AutoRunner.init();

        guis = new ArrayList<>();

        if (EvenMoreFish.mainConfig.isDatabaseOnline()) {

            // Attempts to connect to the database if enabled
            try {
                if (!Database.dbExists()) {
                    Database.createDatabase();
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }

        }

        getServer().getLogger().log(Level.INFO, "EvenMoreFish by Oheers : Enabled");

    }

    public void onDisable() {

        terminateSellGUIS();
        getServer().getLogger().log(Level.INFO, "EvenMoreFish by Oheers : Disabled");

    }

    private void listeners() {

        getServer().getPluginManager().registerEvents(new FishEvent(), this);
        getServer().getPluginManager().registerEvents(new JoinChecker(), this);
        getServer().getPluginManager().registerEvents(new InteractHandler(), this);
        getServer().getPluginManager().registerEvents(new UpdateNotify(), this);

    }

    private void commands() {
        getCommand("evenmorefish").setExecutor(new CommandCentre(this));
        CommandCentre.loadTabCompletes();
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        permission = rsp.getProvider();
        return permission != null;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    // gets called on server shutdown to simulate all player's closing their /emf shop GUIs
    private void terminateSellGUIS() {
        for (SellGUI gui : guis) {
            System.out.println(gui);
            GUICache.attemptPop(gui.getPlayer(), true);
        }
        guis.clear();
    }

    public void reload() {

        terminateSellGUIS();

        fish = new HashMap<>();
        fishCollection = new HashMap<>();
        rewards = new HashMap<>();

        reloadConfig();
        saveDefaultConfig();

        Names names = new Names();
        names.loadRarities();

        LoadRewards.load();
        AutoRunner.init();

        msgs = new Messages();
        mainConfig = new MainConfig();

        guis = new ArrayList<>();
    }

    // Checks for updates, surprisingly
    private void checkUpdate() {
        if (!getDescription().getVersion().equals(new UpdateChecker(this, 91310).getVersion())) {
            isUpdateAvailable = true;
        }
    }

    private void checkConfigVers() {
        if (msgs.configVersion() != MSG_CONFIG_VERSION) {
            getLogger().log(Level.SEVERE, "Your messages.yml config is not up to date. This will cause certain values to default to be potentially null." +
                    "If you wish to update, go to the \"Technical Stuff\" part of https://www.spigotmc.org/resources/evenmorefish.91310/ and copy the messages.yml" +
                    " from there, or locate changes and add them manually to preserve current changes");
        }

        if (mainConfig.configVersion() != MAIN_CONFIG_VERSION) {
            getLogger().log(Level.SEVERE, "Your config.yml config is not up to date. This will cause certain values to default to be potentially null." +
                    "If you wish to update, go to the \"Technical Stuff\" part of https://www.spigotmc.org/resources/evenmorefish.91310/ and copy the messages.yml" +
                    " from there, or locate changes and add them manually to preserve current changes");
        }
    }
}
