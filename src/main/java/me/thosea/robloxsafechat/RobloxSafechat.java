package me.thosea.robloxsafechat;

import me.thosea.robloxsafechat.config.DefaultChats;
import me.thosea.robloxsafechat.config.loader.ConfigHandler;
import me.thosea.robloxsafechat.element.GroupElement;
import me.thosea.robloxsafechat.mixin.IdentifierAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RobloxSafechat implements ClientModInitializer {
	public static final String MOD_ID = "robloxsafechat";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ResourceLocation ICON_KEY = IdentifierAccessor.safechat$of(MOD_ID,
			"textures/gui/sprites/chaticon/default.png");
	public static final ResourceLocation HOVERED_KEY = IdentifierAccessor.safechat$of(MOD_ID,
			"textures/gui/sprites/chaticon/hover.png");
	public static final ResourceLocation SELECTED_KEY = IdentifierAccessor.safechat$of(MOD_ID,
			"textures/gui/sprites/chaticon/selected.png");

	public static final ResourceLocation SETTINGS_ICON_KEY =
			IdentifierAccessor.safechat$of(MOD_ID, "textures/gui/sprites/settingsicon/default.png");
	public static final ResourceLocation SETTINGS_HOVERED_KEY =
			IdentifierAccessor.safechat$of(MOD_ID, "textures/gui/sprites/settingsicon/hover.png");
	public static final ResourceLocation SETTINGS_SELECTED_KEY =
			IdentifierAccessor.safechat$of(MOD_ID, "textures/gui/sprites/settingsicon/selected.png");

	public static GroupElement ROOT = DefaultChats.DEFAULT.root();

	@Override
	public void onInitializeClient() {
		ConfigHandler.reload();
	}
}