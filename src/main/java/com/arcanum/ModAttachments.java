package com.arcanum;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public final class ModAttachments {

	public static final AttachmentType<MagicData> MAGIC = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(ArcanumMod.MOD_ID, "magic"),
		builder -> builder
			.persistent(MagicData.CODEC)
			.initializer(MagicData::initial)
			.copyOnDeath()
	);

	private ModAttachments() {
	}

	/** 클래스 로딩을 강제해서 어태치먼트를 등록한다. */
	public static void init() {
	}
}
