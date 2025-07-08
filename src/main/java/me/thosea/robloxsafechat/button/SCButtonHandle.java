package me.thosea.robloxsafechat.button;

import me.thosea.robloxsafechat.config.SafechatConfig;
import me.thosea.robloxsafechat.mixin.WidgetAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fStack;

import java.util.function.Supplier;

final class SCButtonHandle extends Button {
	@SuppressWarnings("DataFlowIssue")
	private final WidgetTooltipHolder tooltip = ((WidgetAccessor) (Object) this).safechat$getTooltipHolder();

	boolean isSettingsButton = false;

	private float scale;
	private int renderX;
	private int renderY;

	SCButtonHandle(Component text, Runnable onPress) {
		super(0, 0, // x/y set at render
				1, 1,  // width/height set at render
				text,
				ignored -> onPress.run(),
				Supplier::get); // narration
	}

	@Override
	public void setMessage(Component text) {
		super.setMessage(text);
		calculatePosAndScale();
	}

	@Override
	public void playDownSound(SoundManager soundManager) {
		if(isSettingsButton) {
			super.playDownSound(soundManager);
		}
	}

	void calculatePosAndScale() {
		int textWidth = Minecraft.getInstance().font.width(getMessage());

		if(textWidth > SCButton.BASE_WIDTH) {
			scale = ((float) (SCButton.BASE_WIDTH - 4) / (float) textWidth);

			if(!isSettingsButton || tooltip.get() == null) {
				if(scale <= SafechatConfig.TEXT_SCALE_THRESHOLD.get()) {
					tooltip.set(Tooltip.create(getMessage()));
				} else {
					tooltip.set(null);
				}
			}

			scale *= SafechatConfig.SCALE.get();
		} else {
			scale = SafechatConfig.SCALE.get();
		}

		int scaledWidth = Mth.ceil(textWidth * scale);
		this.renderX = (int) (((getX() + getWidth() / 2) - scaledWidth / 2) / scale) + 1;

		// for some reason scales slower than the global scale
		// cause a slight offset in height (it goes UP)
		int yOff = 5; // minimum of 5 for all scales

		if(scale < SafechatConfig.SCALE.get()) {
			// increase if needed
			yOff = (int) ((float) yOff * (SafechatConfig.SCALE.get() / scale) * 1.5);
		}

		this.renderY = (int) (getY() / scale) + yOff;
	}

	@Override
	protected void renderScrollingString(GuiGraphics graphics, Font font, int i, int color) {
		Matrix3x2fStack pose = graphics.pose();

		pose.pushMatrix();
		pose.scale(scale, scale);
		graphics.drawString(font, getMessage(), renderX, renderY, color);
		pose.popMatrix();
	}
}