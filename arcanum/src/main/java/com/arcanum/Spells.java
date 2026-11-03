package com.arcanum;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 주문 구현부. 모든 주문은 서버에서만 실행된다. */
final class Spells {

	private Spells() {
	}

	// ------------------------------------------------------------------
	// 비전 화살: 시선 방향으로 빔을 쏘아 처음 맞은 대상에게 피해
	// ------------------------------------------------------------------
	static boolean arcaneBolt(ServerLevel level, ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		double range = 32.0;
		Vec3 end = eye.add(look.scale(range));

		BlockHitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		double maxDist = blockHit.getType() == HitResult.Type.MISS ? range : blockHit.getLocation().distanceTo(eye);
		Vec3 limit = eye.add(look.scale(maxDist));

		LivingEntity target = null;
		double best = maxDist;
		AABB search = player.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(1.5);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, search, e -> validTarget(player, e))) {
			Optional<Vec3> hit = entity.getBoundingBox().inflate(0.3).clip(eye, limit);
			if (hit.isPresent()) {
				double d = eye.distanceTo(hit.get());
				if (d < best) {
					best = d;
					target = entity;
				}
			}
		}

		// 빔 이펙트
		for (double d = 1.2; d < best; d += 0.35) {
			Vec3 p = eye.add(look.scale(d)).add(0, -0.15, 0);
			level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
			if (((int) (d / 0.35)) % 3 == 0) {
				level.sendParticles(ParticleTypes.WITCH, p.x, p.y, p.z, 2, 0.08, 0.08, 0.08, 0.0);
			}
		}

		Vec3 impact = eye.add(look.scale(best));
		burst(level, ParticleTypes.END_ROD, impact, 18, 0.25, 0.12);
		burst(level, ParticleTypes.ELECTRIC_SPARK, impact, 12, 0.3, 0.2);

		if (target != null) {
			target.hurtServer(level, level.damageSources().indirectMagic(player, player), 8.0F);
			target.knockback(0.5, -look.x, -look.z);
		}

		sound(level, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.5F);
		sound(level, BlockPos.containing(impact), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 1.2F);
		return true;
	}

	// ------------------------------------------------------------------
	// 서리 폭발: 주변 적에게 피해, 구속, 빙결
	// ------------------------------------------------------------------
	static boolean frostNova(ServerLevel level, ServerPlayer player) {
		double radius = 6.0;
		AABB area = player.getBoundingBox().inflate(radius);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, e -> validTarget(player, e))) {
			if (entity.distanceTo(player) > radius) {
				continue;
			}
			entity.hurtServer(level, level.damageSources().indirectMagic(player, player), 5.0F);
			entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2));
			entity.setTicksFrozen(entity.getTicksRequiredToFreeze() + 40);
		}

		Vec3 c = player.position();
		for (int r = 1; r <= (int) radius; r++) {
			int points = 10 * r;
			for (int i = 0; i < points; i++) {
				double a = (Math.PI * 2.0) * i / points;
				double x = c.x + Math.cos(a) * r;
				double z = c.z + Math.sin(a) * r;
				level.sendParticles(ParticleTypes.SNOWFLAKE, x, c.y + 0.2, z, 1, 0.0, 0.05, 0.0, 0.02);
				if (i % 4 == 0) {
					level.sendParticles(ParticleTypes.CLOUD, x, c.y + 0.1, z, 1, 0.0, 0.0, 0.0, 0.03);
				}
			}
		}
		burst(level, ParticleTypes.SNOWFLAKE, c.add(0, 1.0, 0), 40, 0.6, 0.15);

		sound(level, player.blockPosition(), SoundEvents.GLASS_BREAK, 1.3F, 0.7F);
		sound(level, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5F, 0.6F);
		return true;
	}

	// ------------------------------------------------------------------
	// 점멸: 바라보는 방향으로 순간이동 (최대 12칸, 막히면 가능한 곳까지)
	// ------------------------------------------------------------------
	static boolean blink(ServerLevel level, ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		double max = 12.0;

		BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(max)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		double dist = hit.getType() == HitResult.Type.MISS ? max : hit.getLocation().distanceTo(eye) - 0.6;

		Vec3 from = player.position();
		for (double d = dist; d >= 1.0; d -= 0.5) {
			Vec3 offset = look.scale(d);
			if (level.noCollision(player, player.getBoundingBox().move(offset))) {
				Vec3 dest = from.add(offset);

				burst(level, ParticleTypes.PORTAL, from.add(0, 1.0, 0), 40, 0.4, 0.6);
				sound(level, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.3F);

				player.connection.teleport(dest.x, dest.y, dest.z, player.getYRot(), player.getXRot());
				player.resetFallDistance();

				burst(level, ParticleTypes.PORTAL, dest.add(0, 1.0, 0), 50, 0.4, 0.8);
				burst(level, ParticleTypes.REVERSE_PORTAL, dest.add(0, 1.0, 0), 20, 0.3, 0.1);
				sound(level, BlockPos.containing(dest), SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.6F);
				return true;
			}
		}

		player.sendSystemMessage(Component.translatable("message." + ArcanumMod.MOD_ID + ".blocked"), true);
		return false;
	}

	// ------------------------------------------------------------------
	// 찬란한 치유: 주변 아군 회복 + 재생, 언데드에게는 피해
	// ------------------------------------------------------------------
	static boolean radiantHeal(ServerLevel level, ServerPlayer player) {
		double radius = 6.0;
		AABB area = player.getBoundingBox().inflate(radius);

		for (Player ally : level.getEntitiesOfClass(Player.class, area, p -> p.isAlive() && !p.isSpectator())) {
			if (ally.distanceTo(player) > radius) {
				continue;
			}
			ally.heal(8.0F);
			ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
			burst(level, ParticleTypes.HEART, ally.position().add(0, 1.6, 0), 4, 0.4, 0.0);
			burst(level, ParticleTypes.HAPPY_VILLAGER, ally.position().add(0, 1.0, 0), 12, 0.5, 0.0);
		}

		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, e -> validTarget(player, e))) {
			if (entity.distanceTo(player) <= radius && entity.getType().is(EntityTypeTags.UNDEAD)) {
				entity.hurtServer(level, level.damageSources().indirectMagic(player, player), 10.0F);
				burst(level, ParticleTypes.END_ROD, entity.position().add(0, 1.0, 0), 10, 0.3, 0.1);
			}
		}

		Vec3 c = player.position();
		for (int i = 0; i < 48; i++) {
			double a = (Math.PI * 2.0) * i / 48.0;
			double x = c.x + Math.cos(a) * 1.5;
			double z = c.z + Math.sin(a) * 1.5;
			level.sendParticles(ParticleTypes.END_ROD, x, c.y + 0.1, z, 1, 0.0, 0.0, 0.0, 0.0);
			level.sendParticles(ParticleTypes.GLOW, x, c.y + 1.2, z, 1, 0.0, 0.1, 0.0, 0.0);
		}

		sound(level, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, 0.9F, 1.4F);
		sound(level, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F, 1.0F);
		return true;
	}

	// ------------------------------------------------------------------
	// 공통 유틸
	// ------------------------------------------------------------------

	/** 공격 대상 필터: 시전자, 플레이어, 시전자의 길들인 동물은 제외한다. */
	static boolean validTarget(ServerPlayer caster, LivingEntity entity) {
		if (entity == caster || !entity.isAlive() || entity.isSpectator()) {
			return false;
		}
		if (entity instanceof Player) {
			return false;
		}
		return !(entity instanceof TamableAnimal pet && pet.isOwnedBy(caster));
	}

	static void burst(ServerLevel level, ParticleOptions type, Vec3 pos, int count, double spread, double speed) {
		level.sendParticles(type, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
	}

	static void sound(ServerLevel level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, pos, sound, SoundSource.PLAYERS, volume, pitch);
	}
}
