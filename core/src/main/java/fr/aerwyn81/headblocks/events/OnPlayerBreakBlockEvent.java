package fr.aerwyn81.headblocks.events;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.api.events.HeadDeletedEvent;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.services.ConfigService;
import fr.aerwyn81.headblocks.services.HeadService;
import fr.aerwyn81.headblocks.services.LanguageService;
import fr.aerwyn81.headblocks.services.StorageService;
import fr.aerwyn81.headblocks.utils.InternalException;
import fr.aerwyn81.headblocks.utils.MessageUtils;
import fr.aerwyn81.headblocks.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class OnPlayerBreakBlockEvent implements Listener {

    @EventHandler
    public void OnBlockBreakEvent(BlockBreakEvent e) {
        var player = e.getPlayer();
        var block = e.getBlock();

        // Check if block destroyed is a head
        if (block.getType() != Material.PLAYER_WALL_HEAD && block.getType() != Material.PLAYER_HEAD) {
            return;
        }

        Location blockLocation = block.getLocation();

        // Check if the head is a head of the plugin
        HeadLocation headLocation = HeadService.getHeadAt(blockLocation);
        if (headLocation == null) {
            return;
        }

        if (HeadBlocks.isReloadInProgress) {
            e.setCancelled(true);
            player.sendMessage(LanguageService.getMessage("Messages.PluginReloading"));
            return;
        }

        if (!PlayerUtils.hasPermission(player, "headblocks.admin")) {
            e.setCancelled(true);
            player.sendMessage(LanguageService.getMessage("Messages.NoPermissionBlock"));
            return;
        }

        // Destroying HeadBlock require creative gamemode and sneaking
        if (!player.isSneaking() || player.getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
            player.sendMessage(LanguageService.getMessage("Messages.CreativeSneakRemoveHead"));
            return;
        }

        // Check if there is a storage issue
        if (StorageService.hasStorageError()) {
            e.setCancelled(true);
            player.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            return;
        }

        // Remove the head
        try {
            HeadService.removeHeadLocation(headLocation, ConfigService.shouldResetPlayerData());
        } catch (InternalException ex) {
            player.sendMessage(LanguageService.getMessage("Messages.StorageError"));
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError while trying to remove a head (" + headLocation.getUuid() + ") from the storage: " + ex.getMessage()));
        }

        // Send player success message
        player.sendMessage(MessageUtils.parseLocationPlaceholders(LanguageService.getMessage("Messages.HeadRemoved"), blockLocation));

        // Trigger the event HeadDeleted
        Bukkit.getPluginManager().callEvent(new HeadDeletedEvent(headLocation.getUuid(), blockLocation));
    }
}
