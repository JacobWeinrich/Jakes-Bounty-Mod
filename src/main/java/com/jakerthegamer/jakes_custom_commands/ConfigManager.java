package com.jakerthegamer.jakes_custom_commands;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

import static net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR;
import static net.minecraftforge.fml.loading.FMLPaths.setup;

/* [Guide: ConfigManager.java handles your mod’s configuration settings.
   - It uses ForgeConfigSpec to create a configuration file that users can modify.
   - The static block initializes the configuration options.
   - "TESTING" is an example boolean option. You can add more options following this pattern.
   - The constructor loads the configuration for the mod using the setup method.
   - IMPORTANT: Do not modify the builder(), push(), pop(), and build() steps as they are crucial for proper configuration initialization.
] */

@Mod.EventBusSubscriber
public class ConfigManager {

  public static ForgeConfigSpec CONFIG;
  public static BooleanValue TESTING;
  private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
  static {

    BUILDER.push("Config");
    // [Guide: Define a new configuration category for your mod settings using your mod ID.]
    BUILDER.comment("Mod settings").push(ModMain.MODID);
    // [Guide: Define a boolean configuration option named "doesNothing". Its default value is true.]
    TESTING = BUILDER.comment("Testing boolean config").define("doesNothing", true);
    // [Guide: End the configuration category. Every push must be followed by a pop.]
    BUILDER.pop(); // one pop for every push
    // [Guide: Build the complete configuration specification.]
    CONFIG = BUILDER.build();
  }

  public static void loadConfig(ForgeConfigSpec config, String path) {
    final Path configPath = CONFIGDIR.get().resolve(path);
    final CommentedFileConfig file = CommentedFileConfig.builder(configPath)
            .sync()
            .autosave()
            .preserveInsertionOrder()
            .build();

    file.load();
    config.setConfig(file);
  }
}
