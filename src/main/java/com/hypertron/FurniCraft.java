package com.hypertron;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FurniCraft extends JavaPlugin {

    private List<JsonObject> loadedModels = new ArrayList<>();
    private Map<String, Boolean> glowEffects = new HashMap<>();

    @Override
    public void onEnable() {
        // Create plugin directory if it doesn't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        // Create models directory if it doesn't exist
        File modelsDir = new File(getDataFolder(), "models");
        if (!modelsDir.exists()) {
            modelsDir.mkdir();
        }

        // Register event listeners and load config
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        this.getCommand("furnicraft").setExecutor(this);
        loadModelsConfig();

        getLogger().info("FurniCraft has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("FurniCraft has been disabled!");
    }

    public void loadModelsConfig() {
        File modelsYml = new File(getDataFolder(), "models.yml");
        if (!modelsYml.exists()) {
            saveResource("models.yml", false);
        }

        try {
            List<String> modelFiles = new ArrayList<>();
            modelFiles.addAll(getConfig().getStringList("models"));

            for (String modelFile : modelFiles) {
                File modelJsonFile = new File(getDataFolder(), "models/" + modelFile);
                if (modelJsonFile.exists()) {
                    String content = new String(Files.readAllBytes(modelJsonFile.toPath()), StandardCharsets.UTF_8);
                    JsonObject modelData = JsonParser.parseString(content).getAsJsonObject();
                    loadedModels.add(modelData);
                    
                    String boundedBlock = modelData.get("bounded_block").getAsString();
                    boolean glowEffect = modelData.has("glow_effect") && modelData.get("glow_effect").getAsBoolean();
                    glowEffects.put(boundedBlock, glowEffect);
                    
                    getLogger().info("Loaded model: " + modelFile + " (Glow Effect: " + glowEffect + ")");
                } else {
                    getLogger().warning("Model file not found: " + modelFile);
                }
            }
        } catch (IOException e) {
            getLogger().severe("Error reading models configuration!");
            e.printStackTrace();
        }
    }

    public String getSummonCommandForBlock(String boundedBlock) {
        for (JsonObject modelData : loadedModels) {
            if (modelData.get("bounded_block").getAsString().equals(boundedBlock)) {
                return modelData.get("summon_command").getAsString();
            }
        }
        return null;
    }

    public boolean getGlowEffectForBlock(String boundedBlock) {
        return glowEffects.getOrDefault(boundedBlock, false);
    }

    public void applyGlowEffect(Player player, Block block) {
        // Apply glow effect to the player for 3 seconds (60 ticks)
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 1));
        
        // Apply glow effect only to LivingEntity instances
        block.getWorld().getNearbyEntities(block.getLocation(), 5, 5, 5).forEach(entity -> {
            if (entity instanceof LivingEntity && entity != player) {
                ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 1));
            }
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("furnicraft")) {
            if (args.length < 1) {
                sendHelpMessage(player);
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "get":
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.RED + "Usage: /furnicraft get <furniture_name>");
                        return true;
                    }
                    giveFurniture(player, args[1]);
                    break;
                case "list":
                    listAvailableFurniture(player);
                    break;
                case "reload":
                    if (player.hasPermission("furnicraft.admin")) {
                        loadModelsConfig();
                        player.sendMessage(ChatColor.GREEN + "FurniCraft configuration reloaded!");
                    } else {
                        player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                    }
                    break;
                default:
                    sendHelpMessage(player);
                    break;
            }
            return true;
        }
        return false;
    }

    private void giveFurniture(Player player, String furnitureName) {
        for (JsonObject modelData : loadedModels) {
            String blockType = modelData.get("bounded_block").getAsString();
            JsonObject blockInfo = modelData.getAsJsonArray("block_info").get(0).getAsJsonObject();
            String name = blockInfo.get("name").getAsString();
            
            if (name.toLowerCase().replace(" ", "_").equals(furnitureName.toLowerCase())) {
                Material material = Material.valueOf(blockType.replace("minecraft:", "").toUpperCase());
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                
                // Set custom name and lore
                meta.setDisplayName(ChatColor.RESET + name);
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + blockInfo.get("description").getAsString());
                if (getGlowEffectForBlock(blockType)) {
                    lore.add(ChatColor.AQUA + "✨ Glowing Effect");
                }
                meta.setLore(lore);
                
                item.setItemMeta(meta);
                player.getInventory().addItem(item);
                player.sendMessage(ChatColor.GREEN + "You received " + name + "!");
                return;
            }
        }
        player.sendMessage(ChatColor.RED + "Furniture '" + furnitureName + "' not found!");
    }

    private void listAvailableFurniture(Player player) {
        player.sendMessage(ChatColor.GREEN + "Available Furniture:");
        for (JsonObject modelData : loadedModels) {
            JsonObject blockInfo = modelData.getAsJsonArray("block_info").get(0).getAsJsonObject();
            String name = blockInfo.get("name").getAsString();
            boolean hasGlow = getGlowEffectForBlock(modelData.get("bounded_block").getAsString());
            player.sendMessage(ChatColor.YELLOW + "- " + name + 
                (hasGlow ? ChatColor.AQUA + " ✨" : ""));
        }
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GREEN + "=== FurniCraft Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/furnicraft get <furniture_name>" + ChatColor.WHITE + " - Get a furniture item");
        player.sendMessage(ChatColor.YELLOW + "/furnicraft list" + ChatColor.WHITE + " - List all available furniture");
        if (player.hasPermission("furnicraft.admin")) {
            player.sendMessage(ChatColor.YELLOW + "/furnicraft reload" + ChatColor.WHITE + " - Reload the configuration");
        }
    }
}