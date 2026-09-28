package fr.mc.semirp.common;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

public class MessageHelper {

    private final FileConfiguration config;

    public MessageHelper(FileConfiguration config) {
        this.config = config;
    }

    public void send(CommandSender target, String messageKey, String colorKey, Map<String, String> placeholders) {
        String message = config.getString("messages." + messageKey, "Message introuvable : " + messageKey);

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        NamedTextColor color = getColor(colorKey);
        target.sendMessage(Component.text(message).color(color));
    }

    public void send(CommandSender target, String messageKey, String colorKey) {
        send(target, messageKey, colorKey, Map.of());
    }

    private NamedTextColor getColor(String colorKey) {
        String colorName = config.getString("colors." + colorKey, "WHITE");
        NamedTextColor color = NamedTextColor.NAMES.value(colorName.toLowerCase());
        return color != null ? color : NamedTextColor.WHITE;
    }
}