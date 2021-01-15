package studio.thevipershow.safechat.api.checks;

import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import studio.thevipershow.safechat.SafeChat;
import studio.thevipershow.safechat.api.events.CheckRegisterEvent;
import studio.thevipershow.safechat.api.events.CheckUnregisterEvent;
import studio.thevipershow.safechat.SafeChatUtils;

public final class ChecksContainer {

    private static ChecksContainer instance = null;
    public static final Comparator<Check> CHECK_PRIORITY_COMPARATOR = Comparator.comparing(a -> a.getCheckPriority().v);
    private final SafeChat safeChat;

    public ChecksContainer(@NotNull SafeChat safeChat) {
        this.safeChat = safeChat;
        this.registeredChecks = new EnumMap<>(CheckPriority.Priority.class);
        for (CheckPriority.Priority value : CheckPriority.Priority.values()) {
            this.registeredChecks.put(value, new LinkedList<>());
        }
    }

    private final Map<CheckPriority.Priority, LinkedList<Check>> registeredChecks;

    public static ChecksContainer getInstance(SafeChat safeChat) {
        if (instance == null) {
            instance = new ChecksContainer(safeChat);
        }
        return instance;
    }

    /**
     * Get the instance of the ChecksContainer.
     * This method should have rigid checks before being called,
     * The SafeChat plugin DEMANDS to be loaded in order for this method
     * to return the instance succesfully.
     *
     * @return The ChecksContainer if available.
     * @throws UnsupportedOperationException If SafeChat wasn't loaded yet.
     */
    public static ChecksContainer getInstance() throws UnsupportedOperationException {
        if (instance == null) {
            throw new UnsupportedOperationException("SafeChat wasn't enabled yet, you cannot call this method before that.");
        } else {
            return instance;
        }
    }

    private void logCheckRegistration(@NotNull Check check) {
        ConsoleCommandSender console = safeChat.getServer().getConsoleSender();
        console.sendMessage(SafeChatUtils.color(String.format("%s &7registered new check &e%s", SafeChat.PREFIX, check.getName())));
    }

    /**
     * Register a check if it wasn't.
     *
     * @param check The check.
     * @return True if the check was registered, false otherwise.
     */
    public final boolean register(@NotNull Check check) {
        if (check == null) {
            return false;
        }

        LinkedList<Check> checks = registeredChecks.get(check.getCheckPriority());

        if (checks.contains(check)) {
            return false;
        }

        boolean added = checks.add(check);

        if (added) {
            PluginManager manager = safeChat.getServer().getPluginManager();
            manager.callEvent(new CheckRegisterEvent(check));
            logCheckRegistration(check);
        }
        return added;
    }

    /**
     * Unregister a check if it is currently loaded.
     *
     * @param check The check.
     * @return True if it has been unregistered, false otherwise.
     */
    public final boolean unregister(@NotNull Check check) {
        if (check == null) {
            return false;
        }

        LinkedList<Check> checks = registeredChecks.get(check.getCheckPriority());

        boolean removed = checks.remove(check);

        if (removed) {
            PluginManager manager = safeChat.getServer().getPluginManager();
            manager.callEvent(new CheckUnregisterEvent(check));
        }
        return removed;
    }

    /**
     * Get all of the active checks.
     * The checks are returned ordered by their priority.
     *
     * @return The active checks.
     */
    @NotNull
    public Collection<Check> getActiveChecks() {
        Deque<Check> orderedChecks = new LinkedList<>();
        for (CheckPriority.Priority priority : CheckPriority.Priority.values()) {
            registeredChecks.get(priority).forEach(orderedChecks::offerLast);
        }

        return orderedChecks;
    }
}
