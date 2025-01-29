package me.thosea.robloxsafechat.other;

import me.thosea.robloxsafechat.element.GroupElement;
import net.minecraft.client.gui.components.ImageButton;

public final class ChatScreenContext {
	private ChatScreenContext() {}

	private static GroupElement renderGroup = null;
	private static ImageButton openedButton = null;

	public static boolean isMenuOpen() {
		return openedButton != null && renderGroup != null;
	}

	public static void open(GroupElement group, ImageButton button) {
		renderGroup = group;
		openedButton = button;
	}

	public static void closeMenu() {
		renderGroup = null;
		openedButton = null;
	}

	public static GroupElement getRenderGroup() {
		return renderGroup;
	}

	public static ImageButton getOpenButton() {
		return openedButton;
	}

	public static boolean clickRenderGroup(int x, int y, int type) {
		return renderGroup.mouseClicked(x, y, type);
	}
}