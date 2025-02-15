package fr.aerwyn81.headblocks.services;

import fr.aerwyn81.headblocks.HeadBlocks;
import fr.aerwyn81.headblocks.data.HeadLocation;
import fr.aerwyn81.headblocks.utils.message.MessageUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlaceholdersService {

    public static String parse(String pName, UUID pUuid, String message) {
        return parse(pName, pUuid, null, message);
    }

    public static String parse(String pName, UUID pUuid, HeadLocation headLocation, String message) {
        message = message.replaceAll("%player%", pName)
                .replaceAll("%prefix%", LanguageService.getPrefix());

        if (message.contains("%progress%") || message.contains("%current%") || message.contains("%max%") || message.contains("%left%") || message.contains("%headName%")) {
            message = parseInternal(pUuid, message, headLocation);
        } else {
            message = MessageUtils.colorize(message);
        }

        if (HeadBlocks.isPlaceholderApiActive)
            return PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(pUuid), message);

        return MessageUtils.centerMessage(message);
    }

    public static String[] parse(Player player, HeadLocation headLocation, List<String> messages) {
        List<String> msgs = new ArrayList<>();

        messages.forEach(message -> msgs.add(parse(player.getName(), player.getUniqueId(), headLocation, message)));

        return msgs.toArray(new String[0]);
    }

    public static String parseInternal(UUID pUuid, String message, HeadLocation headLocation) {
        String progress;

        var future = StorageService.getHeadsPlayer(pUuid).asFuture();

        try {
            var current = future.get().size();

            int total = StorageService.getHeads().size();

            message = message.replaceAll("%current%", String.valueOf(current))
                    .replaceAll("%max%", String.valueOf(total));

            if (message.contains("%progress%")) {
                progress = MessageUtils.createProgressBar(current, total,
                        ConfigService.getProgressBarBars(),
                        ConfigService.getProgressBarSymbol(),
                        ConfigService.getProgressBarCompletedColor(),
                        ConfigService.getProgressBarNotCompletedColor());

                message = message.replaceAll("%progress%", progress);
            }

            if (message.contains("%left%")) {
                message = message.replaceAll("%left%", String.valueOf(total - current));
            }
        } catch (Exception ignored) {
            HeadBlocks.log.sendMessage(MessageUtils.colorize("&cError retrieving heads from storage, cannot parse all HeadBlocks placeholders"));
        }

        if (message.contains("%headName%")) {
            String headName;
            if (headLocation == null) {
                headName = LanguageService.getMessage("Other.NameNotSet");
            } else {
                headName = headLocation.getName().isEmpty() ? headLocation.getUuid().toString() : headLocation.getName();
            }

            message = message.replaceAll("%headName%", headName);
        }

        return MessageUtils.colorize(message);
    }
}
