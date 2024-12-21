package com.hypertron;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FurniCraft extends JavaPlugin {
    private final Map<String, String> models = new HashMap<>();

    @Override
    public void onEnable() {
        setupFilesAndFolders();
        loadModels();

        // Register the block place listener
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        getLogger().info("FurniCraft has been enabled!");
    }

    private void setupFilesAndFolders() {
        File modelsFolder = new File(getDataFolder(), "models");
        if (!modelsFolder.exists()) {
            modelsFolder.mkdirs();
            getLogger().info("Created models folder.");
        }

        File modelsFile = new File(getDataFolder(), "models.yml");
        if (!modelsFile.exists()) {
            saveResource("models.yml", false);
            getLogger().info("Created models.yml file.");
        }

        File defaultModel = new File(modelsFolder, "default_block.json");
        if (!defaultModel.exists()) {
            saveResource("models/default_block.json", false);
            getLogger().info("Created default_block.json file.");
        }
    }

    private void loadModels() {
        File modelsFolder = new File(getDataFolder(), "models");
        File[] files = modelsFolder.listFiles((dir, name) -> name.endsWith(".json"));

        if (files != null) {
            Gson gson = new Gson();
            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    JsonObject model = gson.fromJson(reader, JsonObject.class);
                    String summonCommand = model.get("summon_command").getAsString();
                    models.put(file.getName().replace(".json", ""), summonCommand);
                    getLogger().info("Loaded model: " + file.getName());
                } catch (IOException e) {
                    getLogger().severe("Failed to load model from " + file.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    public String getSummonCommand(String modelName) {
        return models.get(modelName);
    }
}
