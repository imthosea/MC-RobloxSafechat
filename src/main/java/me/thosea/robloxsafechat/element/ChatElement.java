package me.thosea.robloxsafechat.element;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.thosea.robloxsafechat.config.SafechatConfig;
import me.thosea.robloxsafechat.other.ChatScreenContext;

public class ChatElement extends ButtonElement {
	public ChatElement(String name) {
		super(name, () -> {
			SEND_MESSAGE.accept(name);

			if(SafechatConfig.CLOSE_AFTER_SEND.get()) {
				ChatScreenContext.closeMenu();
			}
		});
	}

	@Override
	public JsonElement serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("type", "chat");
		object.addProperty("chat", name);
		return object;
	}
}