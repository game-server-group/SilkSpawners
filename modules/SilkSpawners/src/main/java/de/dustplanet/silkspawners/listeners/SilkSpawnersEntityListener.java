package de.dustplanet.silkspawners.listeners;

import de.dustplanet.silkspawners.SilkSpawners;
import de.dustplanet.util.SilkUtil;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handle the explosion of a spawner.
 *
 * @author (former) mushroomhostage
 * @author xGhOsTkiLLeRx
 */

public class SilkSpawnersEntityListener implements Listener {

    private SilkSpawners plugin;
    private SilkUtil silkUtil;
    private Random random;

    public SilkSpawnersEntityListener(SilkSpawners plugin, SilkUtil silkUtil) {
        this.plugin = plugin;
        this.silkUtil = silkUtil;
        this.random = ThreadLocalRandom.current();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntiyExplode(EntityExplodeEvent event) {
        /*
         * Skip if event is cancelled entity is not known or null EnderDragon calls this event explosionChance is 0
         */
        Entity entity = event.getEntity();
        if (event.isCancelled() || event.getEntity() == null || entity instanceof EnderDragon
            || plugin.config.getInt("explosionDropChance", 30) == 0) {
            return;
        }

        boolean drop = true;
        if (plugin.config.getBoolean("permissionExplode", false) && entity instanceof TNTPrimed) {
            Entity igniter = ((TNTPrimed) entity).getSource();
            if (igniter != null && igniter instanceof Player) {
                Player sourcePlayer = (Player) igniter;
                drop = sourcePlayer.hasPermission("silkspawners.explodedrop");
            }
        }

        // Check if a spawner block is on the list
        if (drop) {
            for (Block block : event.blockList()) {
                // We have a spawner
                if (block.getType() == silkUtil.nmsProvider.getSpawnerMaterial()) {
                    // Roll the dice
                    int randomNumber = random.nextInt(100);
                    String entityID = silkUtil.getSpawnerEntityID(block);
                    // Check if we should drop a block
                    int dropChance = 0;
                    if (plugin.mobs.contains("creatures." + entityID + ".explosionDropChance")) {
                        dropChance = plugin.mobs.getInt("creatures." + entityID + ".explosionDropChance", 100);
                    } else {
                        dropChance = plugin.config.getInt("explosionDropChance", 100);
                    }
                    if (randomNumber < dropChance) {
                        World world = block.getWorld();
                        world.dropItemNaturally(block.getLocation(),
                            silkUtil.newSpawnerItem(entityID, silkUtil.getCustomSpawnerName(entityID), 1, false));
                    }
                }
            }
        }
    }
}
