package me.thosea.robloxsafechat.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.thosea.robloxsafechat.RobloxSafechat;
import me.thosea.robloxsafechat.element.GroupElement;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static me.thosea.robloxsafechat.RobloxSafechat.LOGGER;

public record SafechatPreset(String name, GroupElement root) {
	static {
		SafechatConfig.SELECTED_PRESET.changeListeners.add(name -> {
			SafechatPreset preset = SafechatConfig.PRESETS.get(name);
			if(preset == null) {
				LOGGER.warn("[RobloxSafechat] Unknown preset {}", name);
				if(SafechatConfig.PRESETS.isEmpty()) {
					LOGGER.warn("[RobloxSafechat] No presets loaded, using default");
					RobloxSafechat.ROOT = DefaultChats.DEFAULT.root();
				} else {
					preset = SafechatConfig.PRESETS.values().iterator().next();
					SafechatConfig.SELECTED_PRESET.set(preset.name());
				}
				return;
			}
			preset.apply();
		});
	}

	public void apply() {
		LOGGER.info("[RobloxSafechat] Applying preset {}", name);
		RobloxSafechat.ROOT = root;
	}

	public static SafechatPreset deserialize(File file) {
		String name = file.getAbsolutePath().substring(ConfigFiles.MESSAGES_FOLDER.getAbsolutePath().length() + 1);
		name = name.substring(0, name.lastIndexOf('.'));
		return deserialize(name, file.toPath());
	}

	public static SafechatPreset deserialize(String name, Path path) {
		try(BufferedReader reader = Files.newBufferedReader(path)) {
			JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
			GroupElement root = GroupElement.deserializeRoot(object);
			return new SafechatPreset(name, root);
		} catch(Exception e) {
			LOGGER.error("[RobloxSafechat] Failed to read messages files from {}", path.toAbsolutePath());
			LOGGER.error("[RobloxSafechat] Default messages will be used instead.", e);
			return new SafechatPreset(name, null);
		}
	}
}