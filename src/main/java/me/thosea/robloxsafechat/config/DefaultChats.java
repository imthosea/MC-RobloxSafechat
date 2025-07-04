package me.thosea.robloxsafechat.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class DefaultChats {
	private DefaultChats() {}

	public static final Map<String, SafechatPreset> PRESETS;
	public static final SafechatPreset DEFAULT;

	static {
		Builder<String, SafechatPreset> builderman = ImmutableMap.builder();

		ModContainer container = FabricLoader.getInstance().getModContainer("robloxsafechat")
				.orElseThrow(() -> new IllegalStateException("no mod container"));
		List<String> presets = container.findPath("default_chats/Presets.txt")
				.map(DefaultChats::getLines)
				.orElseThrow();

		for(String presetFile : presets) {
			Path path = container.findPath("default_chats/" + presetFile)
					.orElseThrow(() -> new IllegalStateException("Missing preset in jar: " + presetFile));

			String name = presetFile.substring(0, presetFile.lastIndexOf('.'));
			SafechatPreset preset = SafechatPreset.deserialize(name, path);
			if(preset.root() == null) {
				throw new IllegalStateException("Broken built-in preset " + presetFile);
			}
			builderman.put(preset.name(), preset);
		}

		PRESETS = builderman.build();
		DEFAULT = PRESETS.get("Default");
	}

	private static List<String> getLines(Path path) {
		try {
			return Files.readAllLines(path);
		} catch(Exception e) {
			throw new RuntimeException("Failed to read lines from jar", e);
		}
	}
}