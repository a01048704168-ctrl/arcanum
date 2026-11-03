package com.arcanum;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 우클릭하면 마나를 회복하고 소모되는 수정. */
public class ManaCrystalItem extends Item {

	public static final int RESTORE_AMOUNT = 35;

	public ManaCrystalItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		MagicData data = serverPlayer.getAttachedOrCreate(ModAttachments.MAGIC);
		if (data.mana() >= MagicData.MAX_MANA) {
			serverPlayer.sendSystemMessage(Component.translatable("message." + ArcanumMod.MOD_ID + ".mana_full"), true);
			return InteractionResult.FAIL;
		}

		ItemStack stack = player.getItemInHand(hand);
		serverPlayer.setAttached(ModAttachments.MAGIC, data.withMana(data.mana() + RESTORE_AMOUNT));
		stack.consume(1, player);

		Vec3 pos = player.position().add(0, 1.0, 0);
		Spells.burst(serverLevel, ParticleTypes.ENCHANT, pos, 30, 0.6, 0.5);
		Spells.burst(serverLevel, ParticleTypes.END_ROD, pos, 8, 0.4, 0.05);
		Spells.sound(serverLevel, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 1.4F);
		Spells.sound(serverLevel, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, 0.5F, 1.8F);

		ArcanumMod.showStatus(serverPlayer);
		return InteractionResult.SUCCESS;
	}
}
