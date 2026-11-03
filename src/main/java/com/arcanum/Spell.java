package com.arcanum;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public enum Spell {
	ARCANE_BOLT("arcane_bolt", 8, 12, 0xB388FF, Spells::arcaneBolt),
	FROST_NOVA("frost_nova", 25, 40, 0x80DEEA, Spells::frostNova),
	BLINK("blink", 15, 30, 0xEA80FC, Spells::blink),
	RADIANT_HEAL("radiant_heal", 30, 60, 0xFFF59D, Spells::radiantHeal);

	@FunctionalInterface
	public interface Caster {
		/** 시전에 성공하면 true. false면 마나와 쿨다운이 소모되지 않는다. */
		boolean cast(ServerLevel level, ServerPlayer player);
	}

	private static final Spell[] VALUES = values();

	public final String id;
	public final int cost;
	public final int cooldownTicks;
	public final int color;
	private final Caster caster;

	Spell(String id, int cost, int cooldownTicks, int color, Caster caster) {
		this.id = id;
		this.cost = cost;
		this.cooldownTicks = cooldownTicks;
		this.color = color;
		this.caster = caster;
	}

	public boolean cast(ServerLevel level, ServerPlayer player) {
		return caster.cast(level, player);
	}

	public MutableComponent displayName() {
		return Component.translatable("spell." + ArcanumMod.MOD_ID + "." + id)
			.withStyle(Style.EMPTY.withColor(color));
	}

	public static Spell byIndex(int index) {
		return VALUES[Math.floorMod(index, VALUES.length)];
	}
}
