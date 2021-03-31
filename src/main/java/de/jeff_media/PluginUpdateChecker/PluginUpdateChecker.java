package de.jeff_media.PluginUpdateChecker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import de.jeff_media.PluginUpdateChecker.events.UpdateCheckFinishedEvent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
public final class PluginUpdateChecker implements Listener {

    private static final String VERSION = "1.5.4";

    @NotNull
    private final Plugin plugin;

    @NotNull
    private final String latestVersionLink;

    @Nullable
    private final String downloadLink;

    @Nullable
    private String downloadLinkFree;

    @Nullable
    private final String changelogLink;

    @Nullable
    private final String donateLink;

    @NotNull
    private final String mcVersion;

    @NotNull
    private String currentVersion = "undefined";

    @NotNull
    private String latestVersion = "undefined";

    @NotNull
    private String parsnip = "%%__USER__%%";

    private boolean usingPaidVersion;

    private int taskId;

    /**
     * Creates a new UpdateChecker.
     *
     * @param plugin The main class of your plugin
     * @param latestVersionLink URL of a text file containing the latest version
     * @param downloadLink URL to the download link of your plugin
     * @param changelogLink URL to the changelog of your plugin
     * @param donateLink URL to a donate link
     */
    public PluginUpdateChecker(@NotNull final Plugin plugin, @NotNull final String latestVersionLink,
                               @Nullable final String downloadLink, @Nullable String downloadLinkFree, @Nullable final String changelogLink,
                               @Nullable final String donateLink, boolean usingPaidVersion) {
        this.plugin = plugin;
        this.latestVersionLink = latestVersionLink;
        this.downloadLink = downloadLink;
        this.downloadLinkFree = downloadLinkFree;
        this.changelogLink = changelogLink;
        this.donateLink = donateLink;
        this.usingPaidVersion = usingPaidVersion;

        final String tmpVersion = plugin.getServer().getClass().getPackage().getName();
        this.mcVersion = tmpVersion.substring(tmpVersion.lastIndexOf('.') + 1);
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @NotNull
    private static TextComponent createLink(@NotNull final String text, @NotNull final String link) {
        final ComponentBuilder builder = new ComponentBuilder(text + " Link: ")
            .bold(true)
            .append(link)
            .bold(false);
        final TextComponent component = new TextComponent(text);
        component.setBold(true);
        // TODO: Make color configurable
        component.setColor(net.md_5.bungee.api.ChatColor.GOLD);
        component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, builder.create()));
        return component;
    }

    /**
     * Checks for an update
     */
    public void check(CommandSender requester) {
        this.checkForUpdateAsync(requester);
    }

    /**
     * Checks for an update now and each checkInterval seconds
     *
     * @param checkInterval Amount of seconds to wait between each update check
     * @return Task id number (-1 if scheduling failed)
     */
    public int check(final long checkInterval, CommandSender requester) {
        this.taskId = Bukkit.getScheduler()
            .scheduleSyncRepeatingTask(this.plugin, () -> checkForUpdateAsync(requester), 0L, checkInterval * 20L);
        return this.taskId;
    }

    /**
     * Stops the update checker.
     */
    public void stop() {
        Bukkit.getScheduler().cancelTask(this.taskId);
    }

    /**
     * Forces a synced update check. You should only use this if you need a check result immediately (e.g. because you want to disable your plugin if there is a new version released) because it will block the main thread until the update check has completed or a timeout occured.
     *
     * @return true if a new update is available, false if update check failed of this is the current version
     */
    public boolean forceSyncedCheck(CommandSender requester) {
        this.checkForUpdate(requester);
        return this.isNewVersionAvailable();
    }

    @EventHandler
    public void onOperatorJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (!player.isOp()) {
            return;
        }
        sendUpdateMessageToPlayer(player);
    }

    private void sendUpdateMessageToPlayer(Player player) {
        if ("undefined".equals(this.latestVersion) ||
            this.currentVersion.equals(this.latestVersion)) {
            return;
        }
        player.sendMessage(ChatColor.GRAY + "There is a new version of " + ChatColor.GOLD + this.plugin.getName()
            + ChatColor.GRAY + " available.");
        this.sendLinks(player);
        player.sendMessage(ChatColor.DARK_GRAY + "Your version: " + this.currentVersion + " | Latest version: " + this.latestVersion);
        player.sendMessage("");
    }

    private boolean isNewVersionAvailable() {
        return !"undefined".equals(this.latestVersion) ||
            !this.latestVersion.equals(this.currentVersion);
    }

    private void sendLinks(@NotNull final Player player) {
        final TextComponent text = new TextComponent("");
        final TextComponent download;
        final TextComponent downloadFree;
        if(downloadLinkFree==null || usingPaidVersion) {
            download = null;
            downloadFree = PluginUpdateChecker.createLink("Download", this.downloadLink);
        } else {
            download = PluginUpdateChecker.createLink("Download (Plus)",this.downloadLink);
            downloadFree = PluginUpdateChecker.createLink("Download (Free)",this.downloadLinkFree);
        }
        final TextComponent donate = PluginUpdateChecker.createLink("Donate", this.donateLink);
        final TextComponent changelog = PluginUpdateChecker.createLink("Changelog", this.changelogLink);
        final TextComponent placeholder = new TextComponent(" | ");
        placeholder.setColor(net.md_5.bungee.api.ChatColor.GRAY);
        int components = 0;
        if (this.downloadLink != null) {
            components++;
        }
        if (this.downloadLinkFree != null) {
            components++;
        }
        if (this.donateLink != null) {
            components++;
        }
        if (this.changelogLink != null) {
            components++;
        }
        if (this.downloadLink != null && download != null) {
            text.addExtra(download);
            if (components > 1) {
                text.addExtra(placeholder);
            }
            components--;
        }
        if (this.downloadLinkFree != null) {
            text.addExtra(downloadFree);
            if (components > 1) {
                text.addExtra(placeholder);
            }
            components--;
        }
        if (this.donateLink != null) {
            text.addExtra(donate);
            if (components > 1) {
                text.addExtra(placeholder);
            }
            components--;
        }
        if (this.changelogLink != null) {
            text.addExtra(changelog);
        }
        player.spigot().sendMessage(text);
    }

    @NotNull
    private String getUserId() {
        return StringUtils.isNumeric(parsnip) ? String.valueOf(Integer.valueOf(parsnip)) : "none";
    }

    @NotNull
    private String getUserAgent() {
        return "JEFF-Media-GbR-PluginUpdateChecker/" + PluginUpdateChecker.VERSION + " (" + this.plugin.getName() +
            '/' + this.plugin.getDescription().getVersion()
                + ", MC/" + this.mcVersion
                + ", Online/" + this.plugin.getServer().getOnlinePlayers().size()
                + ", Players/" + this.plugin.getServer().getOfflinePlayers().length
                + ", Plus/" + (usingPaidVersion ? "yes" : "no")
                + ", UserID/" + getUserId() + ')';
    }

    private void printCheckResult() {
        if (this.latestVersion.equals(this.currentVersion)) {
            this.plugin.getLogger().info(String.format("You are using the latest version of %s.", this.plugin.getName()));
            return;
        }
        this.plugin.getLogger().warning("=================================================");
        this.plugin.getLogger().warning(String.format("There is a new version of %s available!", this.plugin.getName()));
        this.plugin.getLogger().warning("Latest : " + this.latestVersion);
        this.plugin.getLogger().warning("Current: " + this.currentVersion);
        if (this.downloadLink != null) {
            this.plugin.getLogger().warning("Please update to the newest version.");
            this.plugin.getLogger().warning(" ");
            if(usingPaidVersion || downloadLinkFree==null) {
                this.plugin.getLogger().warning("Download:");
                this.plugin.getLogger().warning(this.downloadLink);
            } else {
                this.plugin.getLogger().warning("Download (Plus):");
                this.plugin.getLogger().warning(this.downloadLink);
                this.plugin.getLogger().warning(" ");
                this.plugin.getLogger().warning("Download (Free):");
                this.plugin.getLogger().warning(this.downloadLinkFree);
            }

        }
        this.plugin.getLogger().warning("=================================================");
    }

    private void checkForUpdateAsync(@Nullable CommandSender requester) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> checkForUpdate(requester));
    }

    public void checkForUpdate(@Nullable CommandSender requester) {
        try {
            final HttpURLConnection httpcon = (HttpURLConnection) new URL(this.latestVersionLink).openConnection();
            httpcon.addRequestProperty("User-Agent", this.getUserAgent());
            final InputStreamReader input = new InputStreamReader(httpcon.getInputStream());
            final BufferedReader reader = new BufferedReader(input);
            this.latestVersion = reader.readLine().trim();
            this.currentVersion = this.plugin.getDescription().getVersion().trim();
            Bukkit.getScheduler().runTask(this.plugin, this::printCheckResult);
            //printCheckResult();
            reader.close();
            UpdateCheckFinishedEvent updateCheckFinishedEvent = new UpdateCheckFinishedEvent(!latestVersion.equals(currentVersion), latestVersion, requester);
        } catch (final IOException ioException) {
            this.plugin.getLogger().warning("Could not check for updates.");
        }
    }

}
