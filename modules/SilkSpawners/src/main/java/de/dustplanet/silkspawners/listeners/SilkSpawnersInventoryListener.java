package de.dustplanet.silkspawners.listeners;

import de.dustplanet.silkspawners.SilkSpawners;
import de.dustplanet.util.SilkUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

/**
 * To show a chat message that a player clicked on an mob spawner.
 *
 * @author (former) mushroomhostage
 * @author xGhOsTkiLLeRx
 */

public class SilkSpawnersInventoryListener implements Listener {

    private SilkSpawners plugin;
    private SilkUtil silkUtil;

    public SilkSpawnersInventoryListener(SilkSpawners plugin, SilkUtil silkUtil) {
        this.plugin = plugin;
        this.silkUtil = silkUtil;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrepareItemCraftEvent(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null || event.getRecipe().getResult() == null) {
            return;
        }

        if (event.getRecipe().getResult().getType() != silkUtil.nmsProvider.getSpawnerMaterial()) {
            return;
        }

        ItemStack result = event.getRecipe().getResult();

        for (ItemStack itemStack : event.getInventory().getContents()) {
            if (itemStack.getType() == silkUtil.nmsProvider.getSpawnEggMaterial() && itemStack.getDurability() == 0) {
                String entityID = silkUtil.getStoredEggEntityID(itemStack);
                result = silkUtil.newSpawnerItem(entityID, silkUtil.getCustomSpawnerName(entityID), result.getAmount(), true);
                event.getInventory().setResult(result);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemCraft(CraftItemEvent event) {
        if (event == null || event.getCurrentItem() == null || event.getWhoClicked() == null) {
            return;
        }

        if (event.getCurrentItem().getType() != silkUtil.nmsProvider.getSpawnerMaterial()) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        String entityID = silkUtil.getStoredSpawnerItemEntityID(event.getCurrentItem());
        if (entityID == null) {
            entityID = silkUtil.getDefaultEntityID();
        }
        String creatureName = silkUtil.getCreatureName(entityID);

        String spawnerName = creatureName.toLowerCase().replace(" ", "");
        if (!player.hasPermission("silkspawners.craft." + spawnerName)) {
            event.setCancelled(true);
            silkUtil.sendMessage(player,
                ChatColor
                    .translateAlternateColorCodes('\u0026',
                        plugin.localization.getString("noPermissionCraft").replace("%ID%", entityID))
                    .replace("%creature%", spawnerName));
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event == null || event.getCurrentItem() == null || event.getWhoClicked() == null) {
            return;
        }

        if (event.getCurrentItem().getType() != silkUtil.nmsProvider.getSpawnerMaterial()) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        String entityID = silkUtil.getStoredSpawnerItemEntityID(event.getCurrentItem());

        if (entityID == null) {
            entityID = silkUtil.getDefaultEntityID();
        }
        String creatureName = silkUtil.getCreatureName(entityID);

        if (plugin.config.getBoolean("notifyOnClick") && player.hasPermission("silkspawners.info")) {
            silkUtil.notify(player, creatureName);
        }
    }
}
