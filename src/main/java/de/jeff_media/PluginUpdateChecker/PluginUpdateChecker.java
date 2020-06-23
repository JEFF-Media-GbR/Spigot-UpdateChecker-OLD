package de.jeff_media.PluginUpdateChecker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;


import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PluginUpdateChecker {
    /**
     * Automatic Update-Checker for Bukkit-Plugins.
     * This class downloads a text file from a given URL, and then
     * checks if the current plugin version string equals the
     * content of that file. If not, it will display a link in the
     * console to the latest version. It will also notify any
     * OP as they join the server whether a new version is
     * available. When the update checker could not complete its
     * request, e.g. when you have no internet connection, a
     * warning will be printed in the console.
     * @author mfnalex
     * @author https://www.jeff-media.de
     * @author https://github.com/JEFF-Media-GbR
     * @version 1.0
     */

    private Plugin plugin;
    private String latestVersionLink;
    private String downloadLink = "";
    private String changelogLink = "";
    private String donateLink = "";
    private String currentVersion = "undefined";
    private String latestVersion = "undefined";
    private String mcVersion;
    private int taskId;

    /**
     * Creates a new UpdateChecker.
     * @param plugin The main class of your plugin
     * @param latestVersionLink URL of a text file containing the latest version
     * @param downloadLink URL to the download link of your plugin
     * @param changelogLink URL to the changelog of your plugin
     * @param donateLink URL to a donate link
     */
    public PluginUpdateChecker(@NotNull Plugin plugin, @NotNull String latestVersionLink, @Nullable String downloadLink, @Nullable String changelogLink, @Nullable String donateLink) {
        this.plugin = plugin;
        this.latestVersionLink = latestVersionLink;
        this.downloadLink = downloadLink;
        this.changelogLink = changelogLink;
        this.donateLink = donateLink;

        String tmpVersion = plugin.getServer().getClass().getPackage().getName();
        mcVersion = tmpVersion.substring(tmpVersion.lastIndexOf('.') + 1);
    }

    /**
     * Checks for an update
     */
    public void check() {
        checkForUpdate();
    }

    /**
     * Checks for an update now and each checkInterval seconds
     * @param checkInterval Amount of seconds to wait between each update check
     * @return Task id number (-1 if scheduling failed)
     */
    public int check(long checkInterval) {
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                checkForUpdate();
            }
        }, 0L, checkInterval * 20);
        return taskId;
    }

    /**
     * Stops the update checker.
     */
    public void stop() {
        Bukkit.getScheduler().cancelTask(taskId);
    }

    private TextComponent createLink(String text, String link, net.md_5.bungee.api.ChatColor color) {
        ComponentBuilder hoverCB = new ComponentBuilder(
                text + " Link: ").bold(true)
                .append(link).bold(false);
        TextComponent tc = new TextComponent(text);
        tc.setBold(true);
        tc.setColor(color);
        tc.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link));
        tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverCB.create()));
        return tc;
    }

    private void sendLinks(Player p) {
        TextComponent text = new TextComponent("");

        TextComponent download = createLink("Download", downloadLink, net.md_5.bungee.api.ChatColor.GOLD);
        TextComponent donate = createLink("Donate", donateLink, net.md_5.bungee.api.ChatColor.GOLD);
        TextComponent changelog = createLink("Changelog", changelogLink, net.md_5.bungee.api.ChatColor.GOLD);

        TextComponent placeholder = new TextComponent(" | ");
        placeholder.setColor(net.md_5.bungee.api.ChatColor.GRAY);

        int components = 0;
        if(downloadLink != null) components++;
        if(donateLink != null) components++;
        if(changelogLink != null) components++;

        if(downloadLink != null && components>0) {
            text.addExtra(download);
            if(components>1) {
                text.addExtra(placeholder);
            }
            components--;
        }

        if(donateLink != null && components>0) {
            text.addExtra(donate);
            if(components>1) {
                text.addExtra(placeholder);
            }
            components--;
        }

        if(changelogLink != null) {
            text.addExtra(changelog);
        }


        p.spigot().sendMessage(text);
    }

    public void sendUpdateMessage(Player p) {
        if (!latestVersion.equals("undefined")) {
            if (!currentVersion.equals(latestVersion)) {
                p.sendMessage(ChatColor.GRAY + "There is a new version of " + ChatColor.GOLD + plugin.getName()
                        + ChatColor.GRAY + " available.");
                sendLinks(p);
                p.sendMessage(ChatColor.DARK_GRAY + "Your version: " + currentVersion + " | Latest version: " + latestVersion);
                p.sendMessage("");
            }
        }
    }

    private void printCheckResult() {
        String n = System.lineSeparator();
        if (latestVersion.equals(currentVersion)) {
            plugin.getLogger().info(String.format("You are using the latest version of %s.",plugin.getName()));
        } else {
            plugin.getLogger().warning("=================================================");
            plugin.getLogger().warning(String.format("There is a new version of %s available!",plugin.getName()));
            plugin.getLogger().warning("Latest : " + latestVersion);
            plugin.getLogger().warning("Current: " + currentVersion);
            if(downloadLink != null) {
                plugin.getLogger().warning("Please update to the newest version. Download:");
                plugin.getLogger().warning(downloadLink);
            }
            plugin.getLogger().warning("=================================================");
        }
    }

    private void checkForUpdate() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    String userAgent = plugin.getName() + "/" + plugin.getDescription().getVersion() + " (MC " + mcVersion + ", " + plugin.getServer().getOnlinePlayers().size() + "/" + plugin.getServer().getOfflinePlayers().length + ")";
                    HttpURLConnection httpcon = (HttpURLConnection) new URL(latestVersionLink).openConnection();
                    httpcon.addRequestProperty("User-Agent", userAgent);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
                    String inputLine = reader.readLine().trim();
                    latestVersion = inputLine;
                    currentVersion = plugin.getDescription().getVersion().trim();
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                       @Override
                       public void run() {
                           printCheckResult();
                       }
                    });
                    //printCheckResult();
                    reader.close();
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not check for updates.");
                }
            }
        });
    }
}


