package com.arcanum;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArcanumMod implements ModInitializer {

	public static final String MOD_ID = "arcanum";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** 마나 1 회복에 걸리는 틱 수 (6틱 = 초당 약 3.3, 가득 차는 데 약 30초) */
	private static final int REGEN_INTERVAL_TICKS = 6;

	@Override
	public void onInitialize() {
		ModAttachments.init();
		ModItems.init();

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int tick = server.getTickCount();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (!player.isAlive()) {
					continue;
				}

				if (tick % REGEN_INTERVAL_TICKS == 0) {
					MagicData data = player.getAttachedOrCreate(ModAttachments.MAGIC);
					if (data.mana() < MagicData.MAX_MANA) {
						player.setAttached(ModAttachments.MAGIC, data.withMana(data.mana() + 1));
					}
				}

				if (tick % 4 == 0 && holdingMagicItem(player)) {
					showStatus(player);
				}
			}
		});

		LOGGER.info("Arcanum loaded.");
	}

	private static boolean holdingMagicItem(ServerPlayer player) {
		return player.getMainHandItem().is(ModItems.ARCANE_WAND)
			|| player.getOffhandItem().is(ModItems.ARCANE_WAND)
			|| player.getMainHandItem().is(ModItems.MANA_CRYSTAL)
			|| player.getOffhandItem().is(ModItems.MANA_CRYSTAL);
	}

	/** 액션바에 현재 주문과 마나 게이지를 표시한다. */
	public static void showStatus(ServerPlayer player) {
		MagicData data = player.getAttachedOrCreate(ModAttachments.MAGIC);
		Spell spell = data.currentSpell();

		int filled = Math.round(data.mana() * 10.0F / MagicData.MAX_MANA);
		boolean affordable = data.mana() >= spell.cost;

		MutableComponent bar = Component.literal("");
		bar.append(Component.literal("✦ ").withStyle(ChatFormatting.LIGHT_PURPLE));
		bar.append(spell.displayName());
		bar.append(Component.literal(" (" + spell.cost + ")  ")
			.withStyle(affordable ? ChatFormatting.GRAY : ChatFormatting.RED));
		bar.append(Component.literal("■".repeat(filled)).withStyle(ChatFormatting.AQUA));
		bar.append(Component.literal("■".repeat(10 - filled)).withStyle(ChatFormatting.DARK_GRAY));
		bar.append(Component.literal(" " + data.mana() + "/" + MagicData.MAX_MANA).withStyle(ChatFormatting.GRAY));

		player.sendSystemMessage(bar, true);
	}
}
