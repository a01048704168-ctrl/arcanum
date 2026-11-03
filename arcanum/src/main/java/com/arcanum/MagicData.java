package com.arcanum;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 플레이어에게 붙는 마법 데이터. 마나와 현재 선택된 주문 번호를 저장한다.
 * 저장(persistent)되며 죽어도 유지된다.
 */
public record MagicData(int mana, int spell) {

	public static final int MAX_MANA = 100;

	public static final Codec<MagicData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.optionalFieldOf("mana", MAX_MANA).forGetter(MagicData::mana),
		Codec.INT.optionalFieldOf("spell", 0).forGetter(MagicData::spell)
	).apply(instance, MagicData::new));

	public static MagicData initial() {
		return new MagicData(MAX_MANA, 0);
	}

	public MagicData withMana(int newMana) {
		return new MagicData(Math.max(0, Math.min(MAX_MANA, newMana)), spell);
	}

	public MagicData withSpell(int newSpell) {
		return new MagicData(mana, newSpell);
	}

	public Spell currentSpell() {
		return Spell.byIndex(spell);
	}
}
