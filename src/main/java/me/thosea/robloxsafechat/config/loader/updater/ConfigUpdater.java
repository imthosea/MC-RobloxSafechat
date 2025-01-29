package me.thosea.robloxsafechat.config.loader.updater;

import com.google.gson.JsonObject;

import java.io.IOException;

/**
 * Updates a config from version the source version to one above that.
 */
public final class ConfigUpdater {
	private ConfigUpdater() {}

	public static final UpdateHandler[] UPDATERS = new UpdateHandler[2];

	static {
		UPDATERS[0] = new Update0To1();
		UPDATERS[1] = new Update1To2();
	}

	@FunctionalInterface
	public interface UpdateHandler {
		void transform(JsonObject root) throws IOException;
	}
}