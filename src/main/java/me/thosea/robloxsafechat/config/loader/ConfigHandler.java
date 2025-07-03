package me.thosea.robloxsafechat.config.loader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import me.thosea.robloxsafechat.RobloxSafechat;
import me.thosea.robloxsafechat.config.ConfigFiles;
import me.thosea.robloxsafechat.config.DefaultChats;
import me.thosea.robloxsafechat.config.SafechatConfig;
import me.thosea.robloxsafechat.config.SafechatPreset;
import me.thosea.robloxsafechat.config.loader.updater.ConfigUpdater;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static me.thosea.robloxsafechat.RobloxSafechat.LOGGER;

@SuppressWarnings("StringConcatenationArgumentToLogCall") // exceptions won't print stacktrace
public final class ConfigHandler {
	private ConfigHandler() {}

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();

	private static final int CONFIG_VERSION = 2;

	private static boolean isConfigError;
	private static String jsonMessageError;

	public static void reload() {
		LOGGER.info("[RobloxSafechat] Reloading config");

		jsonMessageError = null;
		isConfigError = false;
		SafechatConfig.PRESETS.clear();

		if(ConfigFiles.CONFIG_FILE.exists()) {
			try(FileReader reader = new FileReader(ConfigFiles.CONFIG_FILE)) {
				JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
				loadConfig(obj);
			} catch(Exception e) {
				LOGGER.error("[RobloxSafechat] Failed to read config file from " + ConfigFiles.CONFIG_FILE, e);
				ConfigOption.CONFIG_OPTIONS.values().forEach(ConfigOption::resetRaw);
				isConfigError = true;
			}
		} else {
			ConfigOption.CONFIG_OPTIONS.values().forEach(ConfigOption::resetRaw);
			writeConfig();
		}

		if(ConfigFiles.MESSAGES_FOLDER.isDirectory()) {
			loadMessages(ConfigFiles.MESSAGES_FOLDER);
		} else {
			writeDefaultMessages();
			SafechatConfig.SELECTED_PRESET.set("Default");
		}

		ConfigOption.CONFIG_OPTIONS.values().forEach(ConfigOption::triggerChange);
		sendErrorMessagesInChat();
	}

	private static void loadMessages(File folder) {
		// noinspection DataFlowIssue - checked by isDirectory
		for(File file : folder.listFiles()) {
			if(file.isDirectory()) {
				loadMessages(file);
			} else if(file.getName().toLowerCase().endsWith(".json")) {
				SafechatPreset preset = SafechatPreset.deserialize(file);
				if(preset.root() == null) { // error logged by parser
					jsonMessageError = preset.name();
					RobloxSafechat.ROOT = DefaultChats.ROOT;
					SafechatConfig.PRESETS.clear();
					return;
				} else {
					SafechatConfig.PRESETS.put(preset.name(), preset);
				}
			} else {
				LOGGER.warn("[RobloxSafechat] Non-json in messages folder: {}", file.getName());
			}
		}
	}

	public static void writeDefaultMessages() {
		ModContainer container = FabricLoader.getInstance().getModContainer("robloxsafechat")
				.orElseThrow(() -> new IllegalStateException("No mod container?"));
		List<String> presets = container.findPath("default_chats/Presets.txt")
				.map(path -> {
					try {
						return Files.readAllLines(path);
					} catch(IOException e) {
						throw new RuntimeException(e);
					}
				})
				.orElseThrow(() -> new IllegalStateException("No presets file in jar?"));
		for(String presetFile : presets) {
			File target = new File(ConfigFiles.MESSAGES_FOLDER, presetFile);
			if(target.exists()) {
				continue;
			}

			Path path = container.findPath("default_chats/" + presetFile)
					.orElseThrow(() -> new IllegalStateException("Missing preset in jar: " + presetFile));

			SafechatPreset preset = SafechatPreset.deserialize(presetFile.substring(0, presetFile.lastIndexOf('.')), path);
			if(preset.root() == null) {
				throw new IllegalStateException("Broken built-in preset " + presetFile);
			}
			SafechatConfig.PRESETS.put(preset.name(), preset);

			writeJson(target, "preset " + presetFile, preset.root().serialize());
		}
	}

	private static void loadConfig(JsonObject obj) {
		int configVer;
		if(obj.has("config_version")) {
			configVer = obj.get("config_version").getAsInt();
		} else {
			configVer = 0;
		}

		if(configVer > CONFIG_VERSION) {
			LOGGER.warn("[RobloxSafechat] Config version {} is above supported version {}", configVer, CONFIG_VERSION);
		} else if(configVer < CONFIG_VERSION) {
			LOGGER.info("[RobloxSafechat] Updating config from version {} to supported version {}", configVer, CONFIG_VERSION);
			if(configVer < 0) {
				throw new IllegalStateException("Config has negative config version " + configVer);
			}

			obj.addProperty("config_version", CONFIG_VERSION);
			for(int i = configVer; i < CONFIG_VERSION; i++) {
				LOGGER.info("[RobloxSafechat] Updating from {} to {}", configVer, configVer + 1);
				try {
					ConfigUpdater.UPDATERS[i].transform(obj);
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
			}

			writeJson(ConfigFiles.CONFIG_FILE, "config", obj);
		}

		LOGGER.info("[RobloxSafechat] Loading config with version {}", CONFIG_VERSION);

		ConfigOption.CONFIG_OPTIONS.forEach((name, option) -> {
			@SuppressWarnings({"UtilityClassWithoutPrivateConstructor", "NonFinalUtilityClass"})
			class Cast {
				static <T> T cast(Object obj) {return (T) obj;}
			}
			option.setRaw(Cast.cast(option.getType().deserialize(obj.get(name))));
		});
	}

	private static long lastWriteAttemptTime = -1;

	public static void writeConfig() {
		if(isConfigError) {
			ChatComponent chat = getChatHud();
			if(chat == null) return;

			if(System.currentTimeMillis() - lastWriteAttemptTime > 1000L) {
				lastWriteAttemptTime = System.currentTimeMillis();
				chat.addMessage(Component.literal(
								"[RobloxSafechat] Are you sure you want to overwrite your invalid config?" +
										" Click again to confirm.")
						.withStyle(ChatFormatting.RED));
				return;
			} else {
				lastWriteAttemptTime = -1;
				isConfigError = false;
				chat.addMessage(Component.literal("[RobloxSafechat] Your invalid config was overwritten.")
						.withStyle(ChatFormatting.GOLD));
			}
		}

		writeJson(ConfigFiles.CONFIG_FILE, "config", makeConfigJson());
	}

	private static JsonObject makeConfigJson() {
		JsonObject obj = new JsonObject();
		obj.addProperty("config_version", CONFIG_VERSION);
		ConfigOption.CONFIG_OPTIONS.forEach((name, option) -> {
			obj.add(name, option.getType().serialize(option.getCast()));
		});
		return obj;
	}

	private static void writeJson(File file, String name, JsonObject object) {
		File dir = file.getParentFile();
		try {
			if(!dir.exists()) Files.createDirectories(dir.toPath());
		} catch(Exception e) {
			LOGGER.error("[RobloxSafechat] Failed to create directory at {}", dir);
			return;
		}

		try {
			if(!file.exists()) Files.createFile(file.toPath());
		} catch(Exception e) {
			LOGGER.error("[RobloxSafechat] Failed to create " + name + " file at " + file, e);
			return;
		}

		try(FileOutputStream stream = new FileOutputStream(file)) {
			try(JsonWriter writer = new JsonWriter(new OutputStreamWriter(stream))) {
				writer.setIndent("\t");
				writer.jsonValue(GSON.toJson(object));
			}
		} catch(Exception e) {
			LOGGER.error("[RobloxSafechat] Failed to write " + name + " file at " + file, e);
		}
	}

	public static void sendErrorMessagesInChat() {
		if(!isConfigError && jsonMessageError == null) return;

		ChatComponent chat = getChatHud();
		if(chat == null) return;

		Component logText = Component.literal("read the log for details.").withStyle(style -> {
			Path path = FabricLoader.getInstance().getGameDir().resolve("logs/latest.log");

			return style
					.withUnderlined(true)
					.withColor(ChatFormatting.GREEN)
					.withHoverEvent(new HoverEvent.ShowText(
							Component.literal("Click to open the log file.")
									.withStyle(ChatFormatting.GREEN)))
					.withClickEvent(new ClickEvent.OpenFile(path.toString()));
		});

		if(isConfigError) {
			chat.addMessage(Component
					.literal("RobloxSafechat failed to read the config file, and is currently using the default settings, ")
					.append(logText));
		}

		if(jsonMessageError != null) {
			chat.addMessage(Component
					.literal("RobloxSafechat failed to read the message file at " +
							"/" + jsonMessageError + ".json, " +
							"and is using placeholder messages, ")
					.append(logText));
		}
	}

	private static ChatComponent getChatHud() {
		try {
			return Minecraft.getInstance().gui.getChat();
		} catch(NullPointerException e) {
			return null;
		}
	}
}