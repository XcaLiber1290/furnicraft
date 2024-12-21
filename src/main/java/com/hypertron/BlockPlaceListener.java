package com.hypertron;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.Bukkit;

public class BlockPlaceListener implements Listener {

    private final FurniCraft plugin;

    public BlockPlaceListener(FurniCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        if (block != null) {
            String blockType = block.getType().toString();
            
            // Get the summon command for the block
            String summonCommand = plugin.getSummonCommandForBlock(blockType);

            if (summonCommand != null) {
                // Use Bukkit.dispatchCommand instead of World.getServer()
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), summonCommand);
                
                // Check and apply glow effect if enabled for this block
                boolean glowEffect = plugin.getGlowEffectForBlock(blockType);
                if (glowEffect) {
                    plugin.applyGlowEffect(player, block);
                }
            }
        }
    }
}