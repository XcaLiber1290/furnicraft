package com.hypertron;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceListener implements Listener {
    private final FurniCraft plugin;

    public BlockPlaceListener(FurniCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.GLASS) {
            event.setCancelled(true); // Cancel the block placement

            Player player = event.getPlayer();
            String summonCommand = plugin.getSummonCommand("default_block");
            if (summonCommand != null) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), summonCommand);
                player.sendMessage("§aCustom block placed successfully!");
            } else {
                player.sendMessage("§cNo model found for this block!");
            }
        }
    }
}
