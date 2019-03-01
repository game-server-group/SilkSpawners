package de.dustplanet.silkspawners.listeners;

import de.dustplanet.silkspawners.SilkSpawners;
import de.dustplanet.util.SilkUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;

/**
 * To show a chat message that a player is holding a mob spawner and it's type.
 *
 * @author (former) mushroomhostage
 * @author xGhOsTkiLLeRx
 */

public class SilkSpawnersPlayerListener implements Listener {

    private SilkSpawners plugin;
    private SilkUtil su;

    public SilkSpawnersPlayerListener(SilkSpawners instance, SilkUtil util) {
        plugin = instance;
        su = util;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerHoldItem(PlayerItemHeldEvent event) {
        // Check if we should notify the player. The second condition is the
        // permission and that the slot isn't null and the item is a mob spawner
        if (event.getPlayer().getInventory().getItem(event.getNewSlot()) != null
            && event.getPlayer().getInventory().getItem(event.getNewSlot()).getType() == su.nmsProvider.getSpawnerMaterial()
            && plugin.config.getBoolean("notifyOnHold") && event.getPlayer().hasPermission("silkspawners.info")) {

            // Get ID
            String entityID = su.getStoredSpawnerItemEntityID(event.getPlayer().getInventory().getItem(event.getNewSlot()));
            // Check for unknown/invalid ID
            if (entityID == null) {
                entityID = su.getDefaultEntityID();
            }
            // Get the name from the entityID
            String spawnerName = su.getCreatureName(entityID);
            Player player = event.getPlayer();
            su.notify(player, spawnerName);
        }
    }
}
