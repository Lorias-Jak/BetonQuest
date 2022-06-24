package org.betonquest.betonquest.modules.updater;

import lombok.CustomLog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.config.ConfigurationFile;
import org.betonquest.betonquest.exceptions.QuestRuntimeException;
import org.betonquest.betonquest.modules.versioning.Version;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.time.temporal.TemporalAmount;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This {@link Updater} checks for new versions on the BetonQuest page and on the GitHub page and download them if wanted.
 */
@CustomLog
public class Updater {
    /**
     * The minimum delay when checking for updates, this prevents too many api requests when reloading the plugin often.
     */
    private static final TemporalAmount CHECK_DELAY = Duration.ofMinutes(10);
    /**
     * The minimum delay when we send a player a notification about a new update.
     */
    private static final TemporalAmount NOTIFICATION_DELAY = Duration.ofHours(20);
    /**
     * The indicator for dev versions.
     */
    private static final String DEV_INDICATOR = "DEV";
    /**
     * The plugins {@link ConfigurationFile} for the debugging settings.
     */
    private final UpdaterConfig config;
    /**
     * The file name of the plugin in the plugin's folder.
     */
    private final String fileName;
    private final File updateFolderFile;
    private final UpdateSourceHandler updateSourceHandler;
    private final BetonQuest plugin;
    private final BukkitScheduler scheduler;
    private final InstantSource instantSource;
    /**
     * The last timestamp, when a player was notified.
     */
    private final Map<UUID, Instant> lastNotification;
    /**
     * The latest version, the key is a {@link Version} and the value is the URL for the download.
     * If the URL is empty, the version is the current installed one or the already downloaded one.
     */
    private Pair<Version, String> latest;
    /**
     * The last timestamp, when an update was searched.
     */
    private Instant lastCheck;
    /**
     * The update notification
     */
    private String updateNotification;

    /**
     * Create a new Updater instance.
     *
     * @param config         The {@link ConfigurationSection} that contains the updater section.
     * @param fileName       The fileName of the plugin in the plugin's folder.
     * @param currentVersion The current plugin version.
     */
    public Updater(final ConfigurationFile config, final String fileName, final File updateFolderFile,
                   final Version currentVersion, final UpdateSourceHandler updateSourceHandler,
                   final BetonQuest plugin, final BukkitScheduler scheduler, final InstantSource instantSource) {
        this.latest = Pair.of(currentVersion, null);
        this.config = new UpdaterConfig(config, latest.getKey(), DEV_INDICATOR);
        this.fileName = fileName;
        this.updateFolderFile = updateFolderFile;
        this.updateSourceHandler = updateSourceHandler;
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.instantSource = instantSource;
        this.lastNotification = new HashMap<>();

        search();
    }

    private String getUpdateNotification(final boolean automaticDownload) {
        final String version = "Found newer version '" + latest.getKey().getVersion() + "', ";
        final String automatic = " automatically installed on the next restart!";
        final String automaticProgress = "it will be downloaded and" + automatic;
        final String automaticDone = "it was downloaded and will be " + automatic;
        final String command = "it will be installed, if you execute '/q update'!";

        if (config.isIngameNotification()) {
            updateNotification = version + (automaticDownload ? automaticDone : command);
        } else {
            updateNotification = null;
        }
        return version + (automaticDownload ? automaticProgress : command);
    }

    /**
     * Starts an asynchronous search for updates.
     */
    public final void search() {
        config.reloadFromConfig();
        if (!config.isEnabled() || !shouldCheckVersion()) {
            return;
        }

        scheduler.runTaskAsynchronously(plugin, () -> {
            if (searchUpdate()) {
                final boolean automatic = config.isAutomatic();
                LOG.info(getUpdateNotification(automatic));
                if (automatic) {
                    update(null);
                }
            }
        });
    }

    private boolean searchUpdate() {
        final Pair<Version, String> newLatest = updateSourceHandler.searchUpdate(config, latest.getKey(), DEV_INDICATOR);
        if (latest.getValue() == null) {
            return false;
        }
        latest = newLatest;
        return true;
    }

    private boolean shouldCheckVersion() {
        final Instant currentTime = instantSource.instant();
        if (lastCheck != null && lastCheck.plus(CHECK_DELAY).isAfter(currentTime)) {
            return false;
        }
        lastCheck = currentTime;
        return true;
    }


    /**
     * Return if a new version is available.
     *
     * @return True, if a update is available.
     */
    public boolean isUpdateAvailable() {
        return latest.getValue() != null;
    }

    /**
     * Return the new version string.
     *
     * @return The version string or null if there is no newer version.
     */
    public String getUpdateVersion() {
        if (latest.getValue() != null) {
            return latest.getKey().getVersion();
        }
        return null;
    }

    /**
     * Sends an update notification to a player.
     *
     * @param player The player, that should receive the message.
     */
    public void sendUpdateNotification(final Player player) {
        if (updateNotification != null) {
            final Instant currentTime = instantSource.instant();
            if (lastNotification.containsKey(player.getUniqueId()) && lastNotification.get(player.getUniqueId()).plus(NOTIFICATION_DELAY).isAfter(currentTime)) {
                return;
            }
            lastNotification.put(player.getUniqueId(), currentTime);

            player.sendMessage(plugin.getPluginTag() + ChatColor.DARK_GREEN + updateNotification);
        }
    }

    /**
     * Download the newest version of the plugin and store it in the /plugins/update/ folder.
     *
     * @param sender The {@link CommandSender} that should receive the update related messages.
     */
    public void update(final CommandSender sender) {
        scheduler.runTaskAsynchronously(plugin, () -> {
            try {
                config.reloadFromConfig();
                if (!config.isEnabled()) {
                    throw new QuestRuntimeException("The updater is disabled! Change config entry 'update.enabled' to 'true' to enable it.");
                }
                if (searchUpdate()) {
                    getUpdateNotification(config.isAutomatic());
                    throw new QuestRuntimeException("Update aborted! A newer version was found. New version '"
                            + getUpdateVersion() + "'! You can execute '/q update' again to update.");
                }

                if (latest.getValue() == null) {
                    throw new QuestRuntimeException("The updater did not find an update!"
                            + " This can depend on your update_strategy, check config entry 'update.update_strategy'.");
                }
                sendMessage(sender, ChatColor.DARK_GREEN + "Started update to version '" + latest.getKey().getVersion() + "'...");
                if (!updateFolderFile.exists() && !updateFolderFile.mkdirs()) {
                    throw new QuestRuntimeException("The updater could not create the folder '" + updateFolderFile.getName() + "'!");
                }

                updateDownloadToFile(updateFolderFile);
                sendMessage(sender, ChatColor.DARK_GREEN + "...download finished. Restart the server to update the plugin.");
            } catch (final QuestRuntimeException e) {
                sendMessage(sender, ChatColor.RED + e.getMessage());
                LOG.debug("Error while performing update!", e);
            }
        });
    }

    private void sendMessage(final CommandSender sender, final String message) {
        LOG.info(message);
        if (sender != null) {
            sender.sendMessage(plugin.getPluginTag() + message);
        }
    }

    private void updateDownloadToFile(final File folder) throws QuestRuntimeException {
        final File file = new File(folder, fileName + ".tmp");
        file.deleteOnExit();
        try {
            if (file.exists()) {
                throw new QuestRuntimeException("The file '" + file.getName() + "' already exists!" +
                        " Please wait for the currently running update to finish. If no update is running delete the file manually.");
            }
            if (!file.createNewFile()) {
                throw new QuestRuntimeException("The updater could not create the file '" + file.getName() + "'!");
            }
            downloadToFileFromURL(latest.getValue(), file);
            if (!file.renameTo(new File(folder, fileName))) {
                throw new QuestRuntimeException("Could not rename the downloaded file."
                        + " Try running '/q update' again. If it still does not work use a manual download.");
            }
            latest = Pair.of(latest.getKey(), null);
        } catch (final IOException e) {
            if (file.exists()) {
                final boolean deleted = file.delete();
                if (!deleted) {
                    throw new QuestRuntimeException("Download was interrupted! A broken file is in '/plugins/update'."
                            + " Delete this file or the updater will not work anymore. Afterwards you can try running"
                            + " '/q update' again. If it still does not work use a manual download.", e);
                }
            }
            throw new QuestRuntimeException("Could not download the file. Try running '/q update' again."
                    + " If it still does not work use a manual download.", e);
        }
    }

    private void downloadToFileFromURL(final String url, final File file) throws IOException {
        FileUtils.copyURLToFile(new URL(url), file, 5000, 5000);
    }
}