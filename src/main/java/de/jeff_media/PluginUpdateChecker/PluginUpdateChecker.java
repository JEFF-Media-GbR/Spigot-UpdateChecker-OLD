package de.jeff_media.PluginUpdateChecker;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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
 *
 * @author mfnalex
 * @author https://www.jeff-media.de
 * @author https://github.com/JEFF-Media-GbR
 * @version 1.0
 */
public class PluginUpdateChecker implements Listener {


    private static final String VERSION = "1.3.2";

    private final Plugin plugin;
    private final String latestVersionLink;
    private final String downloadLink;
    private final String changelogLink;
    private final String donateLink;
    private final String mcVersion;
    private String currentVersion = "undefined";
    private String latestVersion = "undefined";
    private int taskId;

    /**
     * Creates a new UpdateChecker.
     *
     * @param plugin            The main class of your plugin
     * @param latestVersionLink URL of a text file containing the latest version
     * @param downloadLink      URL to the download link of your plugin
     * @param changelogLink     URL to the changelog of your plugin
     * @param donateLink        URL to a donate link
     */
    public PluginUpdateChecker(@NotNull Plugin plugin, @NotNull String latestVersionLink, @Nullable String downloadLink, @Nullable String changelogLink, @Nullable String donateLink) {
        this.plugin = plugin;
        this.latestVersionLink = latestVersionLink;
        this.downloadLink = downloadLink;
        this.changelogLink = changelogLink;
        this.donateLink = donateLink;

        String tmpVersion = plugin.getServer().getClass().getPackage().getName();
        mcVersion = tmpVersion.substring(tmpVersion.lastIndexOf('.') + 1);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Checks for an update
     */
    public void check() {
        checkForUpdateAsync();
    }

    /**
     * Checks for an update now and each checkInterval seconds
     *
     * @param checkInterval Amount of seconds to wait between each update check
     * @return Task id number (-1 if scheduling failed)
     */
    public int check(long checkInterval) {
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::checkForUpdateAsync, 0L, checkInterval * 20);
        return taskId;
    }

    /**
     * Stops the update checker.
     */
    public void stop() {
        Bukkit.getScheduler().cancelTask(taskId);
    }

    /**
     * Forces a synced update check. You should only use this if you need a check result immediately (e.g. because you want to disable your plugin if there is a new version released) because it will block the main thread until the update check has completed or a timeout occured.
     * @return true if a new update is available, false if update check failed of this is the current version
     */
    public boolean forceSyncedCheck() {
        checkForUpdate();
        return isNewVersionAvailable();
    }

    private boolean isNewVersionAvailable() {
        if(latestVersion.equals("undefined")) return false;
        return !latestVersion.equals(currentVersion);
    }

    private TextComponent createLink(String text, String link) {
        ComponentBuilder hoverCB = new ComponentBuilder(
                text + " Link: ").bold(true)
                .append(link).bold(false);
        TextComponent tc = new TextComponent(text);
        tc.setBold(true);
        // TODO: Make color configurable
        tc.setColor(net.md_5.bungee.api.ChatColor.GOLD);
        tc.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link));
        tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverCB.create()));
        return tc;
    }

    private void sendLinks(Player p) {
        TextComponent text = new TextComponent("");

        TextComponent download = createLink("Download", downloadLink);
        TextComponent donate = createLink("Donate", donateLink);
        TextComponent changelog = createLink("Changelog", changelogLink);

        TextComponent placeholder = new TextComponent(" | ");
        placeholder.setColor(net.md_5.bungee.api.ChatColor.GRAY);

        int components = 0;
        if (downloadLink != null) components++;
        if (donateLink != null) components++;
        if (changelogLink != null) components++;

        if (downloadLink != null) {
            text.addExtra(download);
            if (components > 1) {
                text.addExtra(placeholder);
            }
            components--;
        }

        if (donateLink != null) {
            text.addExtra(donate);
            if (components > 1) {
                text.addExtra(placeholder);
            }
            components--;
        }

        if (changelogLink != null) {
            text.addExtra(changelog);
        }

        p.spigot().sendMessage(text);
    }

    @EventHandler
    public void onOperatorJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (!p.isOp()) return;

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

    private String getUserAgent() {
        return "JEFF-Media-GbR-PluginUpdateChecker/" + VERSION + " (" + plugin.getName() + "/" + plugin.getDescription().getVersion() + ", MC/" + mcVersion + ", Online/" + plugin.getServer().getOnlinePlayers().size() + ", Players/" + plugin.getServer().getOfflinePlayers().length + ")";
    }

    private void printCheckResult() {
        if (latestVersion.equals(currentVersion)) {
            plugin.getLogger().info(String.format("You are using the latest version of %s.", plugin.getName()));
        } else {
            plugin.getLogger().warning("=================================================");
            plugin.getLogger().warning(String.format("There is a new version of %s available!", plugin.getName()));
            plugin.getLogger().warning("Latest : " + latestVersion);
            plugin.getLogger().warning("Current: " + currentVersion);
            if (downloadLink != null) {
                plugin.getLogger().warning("Please update to the newest version. Download:");
                plugin.getLogger().warning(downloadLink);
            }
            plugin.getLogger().warning("=================================================");
        }
    }

    private void checkForUpdateAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            checkForUpdate();
        });
    }

    private void checkForUpdate() {
            try {
                HttpURLConnection httpcon = (HttpURLConnection) new URL(latestVersionLink).openConnection();
                httpcon.addRequestProperty("User-Agent", getUserAgent());
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
                latestVersion = reader.readLine().trim();
                currentVersion = plugin.getDescription().getVersion().trim();
                Bukkit.getScheduler().runTask(plugin, this::printCheckResult);
                //printCheckResult();
                reader.close();
            } catch (Exception e) {
                plugin.getLogger().warning("Could not check for updates.");
            }
    }
}


