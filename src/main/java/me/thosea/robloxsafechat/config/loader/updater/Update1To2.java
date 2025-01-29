package me.thosea.robloxsafechat.config.loader.updater;

import com.google.gson.JsonObject;
import me.thosea.robloxsafechat.RobloxSafechat;
import me.thosea.robloxsafechat.config.ConfigFiles;
import me.thosea.robloxsafechat.config.DefaultChats;
import me.thosea.robloxsafechat.config.SafechatConfig;
import me.thosea.robloxsafechat.config.SafechatPreset;
import me.thosea.robloxsafechat.config.loader.ConfigHandler;
import me.thosea.robloxsafechat.config.loader.updater.ConfigUpdater.UpdateHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * From config version 1 (v1.1.0) to config version 2 (v2.2.0),
 * where messages are now stored in the "Presets" folder instead of one file
 */
public class Update1To2 implements UpdateHandler {
	@Override
	public void transform(JsonObject root) throws IOException {
		root.addProperty("selected_preset", "Default");

		// copy old messages file as "default"
		File messagesFile = new File(ConfigFiles.CONFIG_FOLDER, "messages.json");
		if(messagesFile.exists()) {
			File target = new File(ConfigFiles.MESSAGES_FOLDER, "Default.json");
			Files.createDirectories(target.getParentFile().toPath());
			Files.move(messagesFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);

			SafechatPreset preset = SafechatPreset.deserialize("Default", target.toPath());
			if(preset.root() == null) {
				RobloxSafechat.ROOT = DefaultChats.ROOT;
			} else {
				SafechatConfig.PRESETS.put("Default", preset);
				RobloxSafechat.ROOT = preset.root();
			}
			ConfigHandler.writeDefaultMessages();
		}
	}
}