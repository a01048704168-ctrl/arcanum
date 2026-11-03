package com.arcanum;

import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public final class ModItems {

	public static final Item ARCANE_WAND = register(
		"arcane_wand",
		ArcaneWandItem::new,
		new Item.Properties()
			.stacksTo(1)
			.rarity(Rarity.EPIC)
			.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
	);

	public static final Item MANA_CRYSTAL = register(
		"mana_crystal",
		ManaCrystalItem::new,
		new Item.Properties()
			.stacksTo(16)
			.rarity(Rarity.UNCOMMON)
	);

	public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(
		Registries.CREATIVE_MODE_TAB,
		Identifier.fromNamespaceAndPath(ArcanumMod.MOD_ID, "main")
	);

	private ModItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ArcanumMod.MOD_ID, name));
		Item item = factory.apply(properties.setId(key));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static void init() {
		Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			TAB_KEY,
			FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup." + ArcanumMod.MOD_ID))
				.icon(() -> new ItemStack(ARCANE_WAND))
				.displayItems((parameters, output) -> {
					output.accept(ARCANE_WAND);
					output.accept(MANA_CRYSTAL);
				})
				.build()
		);
	}
}
