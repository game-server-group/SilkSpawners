package de.dustplanet.silkspawners.listeners;

import de.dustplanet.silkspawners.SilkSpawners;
import de.dustplanet.silkspawners.events.SilkSpawnersSpawnerBreakEvent;
import de.dustplanet.silkspawners.events.SilkSpawnersSpawnerPlaceEvent;
import de.dustplanet.util.SilkUtil;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handle the placement and breaking of a spawner.
 *
 * @author (former) mushroomhostage
 * @author xGhOsTkiLLeRx
 */

public class SilkSpawnersBlockListener implements Listener {

    private SilkSpawners plugin;
    private SilkUtil silkUtil;
    private ThreadLocalRandom random;

    public SilkSpawnersBlockListener(SilkSpawners plugin, SilkUtil silkUtil) {
        this.plugin = plugin;
        this.silkUtil = silkUtil;
        this.random = ThreadLocalRandom.current();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }

        boolean isFakeEvent = !BlockBreakEvent.class.equals(event.getClass());
        if (isFakeEvent) {
            return;
        }

        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block.getType() != silkUtil.nmsProvider.getSpawnerMaterial()) {
            return;
        }

        if (!silkUtil.canBuildHere(player, block.getLocation())) {
            return;
        }

        String entityID = silkUtil.getSpawnerEntityID(block);

        SilkSpawnersSpawnerBreakEvent breakEvent = new SilkSpawnersSpawnerBreakEvent(player, block, entityID);
        plugin.getServer().getPluginManager().callEvent(breakEvent);
        if (breakEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        entityID = silkUtil.getDisplayNameToMobID().get(breakEvent.getEntityID());

        plugin.informPlayer(player, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("spawnerBroken"))
            .replace("%creature%", silkUtil.getCreatureName(entityID)));

        ItemStack tool = silkUtil.nmsProvider.getItemInHand(player);
        boolean validToolAndSilkTouch = silkUtil.isValidItemAndHasSilkTouch(tool);

        World world = player.getWorld();

        String mobName = silkUtil.getCreatureName(entityID).toLowerCase().replace(" ", "");

        if (plugin.config.getBoolean("noDropsCreative", true) && player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        event.setExpToDrop(0);
        boolean mined = false;
        boolean dropXPOnlyOnDestroy = plugin.config.getBoolean("dropXPOnlyOnDestroy", false);

        if (plugin.config.getBoolean("preventXPFarming", true) && block.hasMetadata("mined")) {
            mined = block.getMetadata("mined").get(0).asBoolean();
        }

        if (player.hasPermission("silkspawners.silkdrop." + mobName) || player.hasPermission("silkspawners.destroydrop." + mobName)) {
            int addXP = plugin.config.getInt("destroyDropXP");
            // If we have more than 0 XP, drop them
            // either we drop XP for destroy and silktouch or only when
            // destroyed and we have no silktouch
            if (!mined && addXP != 0 && (!dropXPOnlyOnDestroy || !validToolAndSilkTouch)) {
                event.setExpToDrop(addXP);
                // check if we should flag spawners
                if (plugin.config.getBoolean("preventXPFarming", true)) {
                    block.setMetadata("mined", new FixedMetadataValue(plugin, true));
                }
            }
        }

        int randomNumber = random.nextInt(100);
        int dropChance = 0;

        if (validToolAndSilkTouch && player.hasPermission("silkspawners.silkdrop." + mobName)) {
            if (plugin.mobs.contains("creatures." + entityID + ".silkDropChance")) {
                dropChance = plugin.mobs.getInt("creatures." + entityID + ".silkDropChance", 100);
            } else {
                dropChance = plugin.config.getInt("silkDropChance", 100);
            }

            if (randomNumber < dropChance) {
                ItemStack breakEventDrop = breakEvent.getDrop();
                ItemStack spawnerItemStack = null;
                if (breakEventDrop != null) {
                    spawnerItemStack = breakEventDrop;
                } else {
                    int amount = 1;
                    if (plugin.mobs.contains("creatures." + entityID + ".dropAmount")) {
                        amount = plugin.mobs.getInt("creatures." + entityID + ".dropAmount", 1);
                    } else {
                        amount = plugin.config.getInt("dropAmount", 1);
                    }
                    spawnerItemStack = silkUtil.newSpawnerItem(entityID, silkUtil.getCustomSpawnerName(entityID), amount, false);
                }
                if (spawnerItemStack == null) {
                    plugin.getLogger().warning("Skipping dropping of spawner, since item is null");
                    return;
                }
                if (plugin.getConfig().getBoolean("dropSpawnerToInventory", false)) {
                    HashMap<Integer, ItemStack> additionalItems = player.getInventory().addItem(spawnerItemStack);
                    if (!additionalItems.isEmpty()) {
                        for (ItemStack itemStack : additionalItems.values()) {
                            world.dropItemNaturally(block.getLocation(), itemStack);
                        }
                    }
                } else {
                    world.dropItemNaturally(block.getLocation(), spawnerItemStack);
                }
            }
            return;
        }

        if (player.hasPermission("silkspawners.destroydrop." + mobName)) {
            if (plugin.config.getBoolean("destroyDropEgg", false)) {
                randomNumber = random.nextInt(100);
                if (plugin.mobs.contains("creatures." + entityID + ".eggDropChance")) {
                    dropChance = plugin.mobs.getInt("creatures." + entityID + ".eggDropChance", 100);
                } else {
                    dropChance = plugin.config.getInt("eggDropChance", 100);
                }
                if (randomNumber < dropChance) {
                    world.dropItemNaturally(block.getLocation(), silkUtil.newEggItem(entityID, 1));
                }
            }

            int dropBars = plugin.config.getInt("destroyDropBars", 0);
            if (dropBars != 0) {
                randomNumber = random.nextInt(100);
                if (plugin.mobs.contains("creatures." + entityID + ".destroyDropChance")) {
                    dropChance = plugin.mobs.getInt("creatures." + entityID + ".destroyDropChance", 100);
                } else {
                    dropChance = plugin.config.getInt("destroyDropChance", 100);
                }
                if (randomNumber < dropChance) {
                    world.dropItem(block.getLocation(), new ItemStack(silkUtil.nmsProvider.getIronFenceMaterial(), dropBars));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }

        boolean isFakeEvent = !BlockPlaceEvent.class.equals(event.getClass());
        if (isFakeEvent) {
            return;
        }

        Block blockPlaced = event.getBlockPlaced();
        if (blockPlaced.getType() != silkUtil.nmsProvider.getSpawnerMaterial()) {
            return;
        }
        Player player = event.getPlayer();
        if (!silkUtil.canBuildHere(player, blockPlaced.getLocation())) {
            return;
        }
        ItemStack item = event.getItemInHand();
        String entityID = silkUtil.getStoredSpawnerItemEntityID(item);
        boolean defaultID = false;
        if (entityID == null) {
            defaultID = true;
            entityID = silkUtil.getDefaultEntityID();
        }

        SilkSpawnersSpawnerPlaceEvent placeEvent = new SilkSpawnersSpawnerPlaceEvent(player, blockPlaced, entityID);
        plugin.getServer().getPluginManager().callEvent(placeEvent);

        if (placeEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        entityID = placeEvent.getEntityID();

        String creatureName = silkUtil.getCreatureName(entityID);
        String spawnerName = creatureName.toLowerCase().replace(" ", "");

        if (!player.hasPermission("silkspawners.place." + spawnerName)) {
            event.setCancelled(true);
            silkUtil.sendMessage(player,
                ChatColor
                    .translateAlternateColorCodes('\u0026',
                        plugin.localization.getString("noPermissionPlace").replace("%ID%", entityID))
                    .replace("%creature%", creatureName));
            return;
        }

        if (defaultID) {
            plugin.informPlayer(player, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("placingDefault")));
        } else {
            plugin.informPlayer(player, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("spawnerPlaced"))
                .replace("%creature%", silkUtil.getCreatureName(entityID)));
        }

        silkUtil.setSpawnerEntityID(blockPlaced, entityID);
    }
}
