package de.jeff_media.PluginUpdateChecker.events;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UpdateCheckFinishedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final boolean newVersionAvailable;
    private final String newestVersion;
    private final @Nullable CommandSender requester;

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public boolean isNewVersionAvailable() {
        return newVersionAvailable;
    }

    public String getNewestVersion() {
        return newestVersion;
    }

    public @Nullable CommandSender getRequester() {
        return requester;
    }

    public UpdateCheckFinishedEvent(boolean newVersionAvailable, String newestVersion, @Nullable CommandSender requester) {
        this.newestVersion = newestVersion;
        this.newVersionAvailable = newVersionAvailable;
        this.requester = requester;
    }


}
