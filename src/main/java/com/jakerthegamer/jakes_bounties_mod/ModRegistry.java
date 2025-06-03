package com.jakerthegamer.jakes_bounties_mod;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/* [Guide: ModRegistry.java is responsible for registering your mod’s custom elements.
   - It uses DeferredRegister to safely register items, blocks, and tile entities during mod initialization.
   - The provided examples are commented out. Uncomment and modify them to add your own mod elements.
   - Always use unique, all-lowercase names for your registrations.
] */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModRegistry {

  // [Guide: DeferredRegister for custom items. Add your item registrations here.]
  public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ModMain.MODID);
  // [Guide: DeferredRegister for custom blocks. Add your block registrations here.]
  public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ModMain.MODID);
  // [Guide: DeferredRegister for tile entities (block entities). Add your tile entity registrations here.]
  public static final DeferredRegister<BlockEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ModMain.MODID);

  // [Guide: Example registrations for items and blocks. Uncomment and edit these lines to add custom mod elements.]
  // public static final RegistryObject<Item> OVERWORLD_KEY = ITEMS.register("whatever", () -> new Item(new Item.Properties().group(ItemGroup.MISC)));
  // public static final RegistryObject<Block> STUFF_BLOCK = BLOCKS.register("stuff", () -> new Block(Block.Properties.of(Material.STONE)));
  // public static final RegistryObject<Item> STUFF_ITEM = ITEMS.register("stuff", () -> new BlockItem(STUFF_BLOCK.get(), new Item.Properties()));

  // [Guide: Example container registration. Uncomment and implement if your mod uses custom container types.]
  // @SubscribeEvent
  // public static void onContainerRegistry(final RegistryEvent.Register<ContainerType<?>> event) {
  //   // IForgeRegistry<ContainerType<?>> r = event.getRegistry();
  // }
}
