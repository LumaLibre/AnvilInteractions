package dev.lumas.interactions;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Folia schedulers
 * @author Mitality
 */
public class FoliaUtils {

    private static final boolean IS_FOLIA;

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        IS_FOLIA = folia;
    }

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public static void runOnEntity(Plugin plugin, Entity entity, Runnable task) {
        if (IS_FOLIA) {
            entity.getScheduler().run(plugin, scheduledTask -> task.run(), null);
        } else {
            if (Bukkit.isPrimaryThread()) {
                task.run();
            } else {
                Bukkit.getScheduler().runTask(plugin, task);
            }
        }
    }

    public static void runOnEntityLater(Plugin plugin, Entity entity, Runnable task, long delayTicks) {
        if (IS_FOLIA) {
            entity.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), null, delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static ScheduledTask runOnEntityTimer(Plugin plugin, Entity entity, Consumer<ScheduledTask> task, long delayTicks, long periodTicks) {
        if (IS_FOLIA) {
            return entity.getScheduler().runAtFixedRate(plugin, task, null, delayTicks <= 0 ? 1 : delayTicks, periodTicks);
        } else {
            int[] taskId = new int[1];
            taskId[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                task.accept(new BukkitScheduledTaskWrapper(taskId[0]));
            }, delayTicks, periodTicks).getTaskId();
            return new BukkitScheduledTaskWrapper(taskId[0]);
        }
    }

    public static void runOnRegion(Plugin plugin, Location location, Runnable task) {
        if (IS_FOLIA) {
            Bukkit.getRegionScheduler().execute(plugin, location, task);
        } else {
            if (Bukkit.isPrimaryThread()) {
                task.run();
            } else {
                Bukkit.getScheduler().runTask(plugin, task);
            }
        }
    }

    public static void runGlobal(Plugin plugin, Runnable task) {
        if (IS_FOLIA) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, task);
        } else {
            if (Bukkit.isPrimaryThread()) {
                task.run();
            } else {
                Bukkit.getScheduler().runTask(plugin, task);
            }
        }
    }

    public static ScheduledTask runGlobalTimer(Plugin plugin, Consumer<ScheduledTask> task, long delayTicks, long periodTicks) {
        if (IS_FOLIA) {
            return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task, delayTicks <= 0 ? 1 : delayTicks, periodTicks);
        } else {
            int[] taskId = new int[1];
            taskId[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                task.accept(new BukkitScheduledTaskWrapper(taskId[0]));
            }, delayTicks, periodTicks).getTaskId();
            return new BukkitScheduledTaskWrapper(taskId[0]);
        }
    }

    public static void runAsync(Plugin plugin, Runnable task) {
        if (IS_FOLIA) {
            Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public static void runAsyncLater(Plugin plugin, Runnable task, long delayTicks) {
        if (IS_FOLIA) {
            long delayMs = delayTicks * 50L;
            Bukkit.getAsyncScheduler().runDelayed(plugin, scheduledTask -> task.run(), delayMs, TimeUnit.MILLISECONDS);
        } else {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
        }
    }

    public static ScheduledTask runAsyncTimer(Plugin plugin, Consumer<ScheduledTask> task, long delayTicks, long periodTicks) {
        if (IS_FOLIA) {
            long delayMs = delayTicks * 50L;
            long periodMs = periodTicks * 50L;
            return Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task, delayMs, periodMs, TimeUnit.MILLISECONDS);
        } else {
            int[] taskId = new int[1];
            taskId[0] = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                task.accept(new BukkitScheduledTaskWrapper(taskId[0]));
            }, delayTicks, periodTicks).getTaskId();
            return new BukkitScheduledTaskWrapper(taskId[0]);
        }
    }

    public static void cancelTask(ScheduledTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Wrapper implementing ScheduledTask interface for Bukkit's task ID system.
     * Used on non-Folia servers to provide a unified cancellation API.
     */
    public static class BukkitScheduledTaskWrapper implements ScheduledTask {
        private final int taskId;

        public BukkitScheduledTaskWrapper(int taskId) {
            this.taskId = taskId;
        }

        @Override
        public Plugin getOwningPlugin() {
            return null;
        }

        @Override
        public boolean isRepeatingTask() {
            return false;
        }

        @Override
        public CancelledState cancel() {
            Bukkit.getScheduler().cancelTask(taskId);
            return CancelledState.CANCELLED_BY_CALLER;
        }

        @Override
        public ExecutionState getExecutionState() {
            return ExecutionState.IDLE;
        }

        @Override
        public boolean isCancelled() {
            return !Bukkit.getScheduler().isCurrentlyRunning(taskId) && !Bukkit.getScheduler().isQueued(taskId);
        }
    }
}