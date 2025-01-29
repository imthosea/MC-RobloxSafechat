package me.thosea.robloxsafechat.element;

import me.thosea.robloxsafechat.config.ConfigFiles;
import me.thosea.robloxsafechat.config.SafechatConfig;
import me.thosea.robloxsafechat.config.SafechatPreset;
import me.thosea.robloxsafechat.config.loader.ConfigHandler;
import me.thosea.robloxsafechat.other.ChatScreenContext;
import net.minecraft.Util;

import java.util.Comparator;
import java.util.List;

public class SettingsElement extends GroupElement {
	public static final SettingsElement INSTANCE = new SettingsElement();

	private SettingsElement() {
		super(null);
	}

	private SettingsElement(String name) {
		super(name);
	}

	public void init() {
		list.clear();

		add(new SettingsElement("Preset") {
			{
				if(SafechatConfig.PRESETS.isEmpty()) {
					ButtonElement element;
					add(element = new ButtonElement("No presets found", ChatScreenContext::closeMenu));
					// disable sound
					element.getButton().setIsSettingsButton(false);
				} else {
					SafechatConfig.PRESETS.values()
							.stream().sorted(Comparator.comparing(SafechatPreset::name))
							.forEach(preset -> {
								String buttonName = preset.name();
								if(SafechatConfig.SELECTED_PRESET.get().equals(buttonName)) {
									buttonName = "> " + buttonName;
								}
								add(new ButtonElement(buttonName, () -> {
									preset.apply();
									SafechatConfig.SELECTED_PRESET.set(preset.name());
									ChatScreenContext.closeMenu();
									ConfigHandler.writeConfig();
								}));
							});
				}

			}
		});
		add(SafechatConfig.SCALE.makeButton());
		add(SafechatConfig.INSTANTLY_SEND.makeButton());
		add(SafechatConfig.CLOSE_AFTER_SEND.makeButton());
		add(SafechatConfig.TEXT_SCALE_THRESHOLD.makeButton());

		add(new SettingsElement("Group Settings") {
			{
				add(SafechatConfig.FLIP_GROUPS.makeButton());
				add(SafechatConfig.GROUPS_ARE_ALSO_TEXTS.makeButton());
				add(SafechatConfig.SHOW_ARROWS_NEXT_TO_GROUPS.makeButton());
			}
		});
		add(new SettingsElement("Opacity Settings") {
			{
				add(SafechatConfig.MOUSE_AFFECTS_OPACITY.makeButton());
				add(SafechatConfig.CHAT_FIELD_FILL_AFFECTS_OPACITY.makeButton());
				add(SafechatConfig.OPACITY_MULTIPLIER.makeButton());
			}
		});

		add(new ButtonElement("Open config folder", () -> {
			Util.getPlatform().openUri(ConfigFiles.CONFIG_FOLDER.toURI());
			ChatScreenContext.closeMenu();
		}));
		add(new ButtonElement("Open messages folder", () -> {
			Util.getPlatform().openUri(ConfigFiles.MESSAGES_FOLDER.toURI());
			ChatScreenContext.closeMenu();
		}));
		add(new ButtonElement("Reload messages & config", () -> {
			ConfigHandler.reload();
			ChatScreenContext.closeMenu();
			init();
		}));
	}

	@Override
	public GroupElement add(SafechatElement element) {
		element.getButton().setIsSettingsButton(true);
		return super.add(element);
	}

	@Override
	protected Runnable makeAction() {
		return () -> {};
	}

	@Override
	protected List<SafechatElement> listView() {
		return list;
	}
}