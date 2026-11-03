package com.arcanum;

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

/**
 * 비전의 지팡이.
 *  - 우클릭: 선택한 주문 시전
 *  - 웅크리기 + 우클릭: 주문 변경
 */
public class ArcaneWandItem extends Item {

	public ArcaneWandItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		ItemStack stack = player.getItemInHand(hand);
		MagicData data = serverPlayer.getAttachedOrCreate(ModAttachments.MAGIC);

		// 주문 변경
		if (player.isShiftKeyDown()) {
			MagicData next = data.withSpell(data.spell() + 1);
			serverPlayer.setAttached(ModAttachments.MAGIC, next);
			Spells.sound(serverLevel, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.0F + 0.15F * next.currentSpell().ordinal());
			ArcanumMod.showStatus(serverPlayer);
			return InteractionResult.CONSUME;
		}

		Spell spell = data.currentSpell();
		if (data.mana() < spell.cost) {
			serverPlayer.sendSystemMessage(Component.translatable("message." + ArcanumMod.MOD_ID + ".no_mana"), true);
			Spells.sound(serverLevel, player.blockPosition(), SoundEvents.DISPENSER_FAIL, 0.8F, 1.0F);
			return InteractionResult.FAIL;
		}

		if (!spell.cast(serverLevel, serverPlayer)) {
			return InteractionResult.FAIL;
		}

		MagicData after = serverPlayer.getAttachedOrCreate(ModAttachments.MAGIC);
		serverPlayer.setAttached(ModAttachments.MAGIC, after.withMana(after.mana() - spell.cost));
		serverPlayer.getCooldowns().addCooldown(stack, spell.cooldownTicks);
		ArcanumMod.showStatus(serverPlayer);
		return InteractionResult.SUCCESS;
	}
}
