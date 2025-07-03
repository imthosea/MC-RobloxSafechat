package me.thosea.robloxsafechat.mixin;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import me.thosea.robloxsafechat.RobloxSafechat;
import me.thosea.robloxsafechat.config.SafechatConfig;
import me.thosea.robloxsafechat.element.SettingsElement;
import me.thosea.robloxsafechat.other.ChatScreenContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class MixinChatScreen extends Screen {
	@Unique private ImageButton safechat$chatsButton;
	@Unique private ImageButton safechat$settingsButton;

	@Unique private GuiGraphics safechat$graphics;

	@Shadow protected EditBox input;

	@Inject(method = "init", at = @At("TAIL"))
	private void afterInit(CallbackInfo ci) {
		safechat$chatsButton = safechat$makeButton(width - 20,
				RobloxSafechat.ICON_KEY,
				RobloxSafechat.HOVERED_KEY,
				RobloxSafechat.SELECTED_KEY);
		safechat$settingsButton = safechat$makeButton(width - 34,
				RobloxSafechat.SETTINGS_ICON_KEY,
				RobloxSafechat.SETTINGS_HOVERED_KEY,
				RobloxSafechat.SETTINGS_SELECTED_KEY);

		addRenderableWidget(safechat$chatsButton);
		addRenderableWidget(safechat$settingsButton);
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void afterRender(GuiGraphics graphics, int mouseX, int mouseY, float f, CallbackInfo ci) {
		this.safechat$graphics = graphics;

		if(ChatScreenContext.isMenuOpen()) {
			float scale = SafechatConfig.SCALE.get();

			ChatScreenContext.getRenderGroup().renderGroup(
					graphics,
					(int) (ChatScreenContext.getOpenButton().getX() - (80 * scale)),
					ChatScreenContext.renderY = (int) (ChatScreenContext.getOpenButton().getY() - (20 * scale)),
					mouseX, mouseY);
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void onMouseClicked(double x, double y, int type, CallbackInfoReturnable<Boolean> cir) {
		if(ChatScreenContext.isMenuOpen() && safechat$graphics != null) {
			if(!ChatScreenContext.clickRenderGroup((int) x, (int) y, type)) {
				ChatScreenContext.closeMenu();
			}
			cir.setReturnValue(true);
		} else if(safechat$chatsButton.mouseClicked(x, y, type)) {
			ChatScreenContext.open(RobloxSafechat.ROOT, safechat$chatsButton);
			cir.setReturnValue(true);
		} else if(safechat$settingsButton.mouseClicked(x, y, type)) {
			ChatScreenContext.open(SettingsElement.INSTANCE, safechat$settingsButton);
			SettingsElement.INSTANCE.init();
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "resize", at = @At("HEAD"))
	private void onResize(Minecraft minecraft, int i, int j, CallbackInfo ci) {
		ChatScreenContext.closeMenu();
	}

	@Inject(method = "removed", at = @At("TAIL"))
	private void onRemove(CallbackInfo ci) {
		ChatScreenContext.closeMenu();
	}

	private ImageButton safechat$makeButton(int x,
	                                        ResourceLocation normal, ResourceLocation hovered,
	                                        ResourceLocation selected) {
		return new ImageButton(
				x, height - 14, // x/y
				12, 12, // width/height
				null,
				ignored -> {},
				Component.empty()
		) {
			@Override
			public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float f) {
				ResourceLocation icon;

				if(ChatScreenContext.getOpenButton() == this) {
					icon = selected;
				} else if(this.isHovered()) {
					icon = hovered;
				} else {
					icon = normal;
				}

				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();
				RenderSystem.setShaderColor(
						1.0f, 1.0f, 1.0f,
						getOpacity(mouseX, mouseY) * SafechatConfig.OPACITY_MULTIPLIER.get());

				graphics.blitSprite(icon,
						this.getX(), this.getY(),
						this.width, this.height);

				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
				RenderSystem.disableBlend();
			}

			private int lastMouseX;
			private int lastMouseY;
			private long lastMouseMoveTime; // milliseconds

			private float getOpacity(int mouseX, int mouseY) {
				if(this.isHovered() || ChatScreenContext.getOpenButton() == this) {
					return 1f;
				} else if(safechat$chatsButton.isHovered()
						|| safechat$settingsButton.isHovered()
						|| ChatScreenContext.isMenuOpen()
						|| !SafechatConfig.MOUSE_AFFECTS_OPACITY.get()) {
					return Math.max(0.6f, getChatFillOpacity());
				} else {
					Window window = Minecraft.getInstance().getWindow();
					float xDelta = (float) (getX() - mouseX) / window.getScreenWidth();
					float yDelta = (float) (getY() - mouseY) / window.getScreenHeight();

					double dist = 1f - (Math.sqrt((xDelta * xDelta) + (yDelta * yDelta)) + 0.5);

					long timeSinceMouseMove;
					if(lastMouseX != mouseX || lastMouseY != mouseY) {
						lastMouseX = mouseX;
						lastMouseY = mouseY;
						lastMouseMoveTime = System.currentTimeMillis();
						timeSinceMouseMove = 0;
					} else {
						timeSinceMouseMove = System.currentTimeMillis() - lastMouseMoveTime;
					}
					timeSinceMouseMove -= 800;

					float opacityBonus;
					if(timeSinceMouseMove <= 0) {
						opacityBonus = 0.2f;
					} else if(timeSinceMouseMove >= 1200) {
						opacityBonus = -0.1f;
					} else {
						opacityBonus = Mth.lerp(
								(float) timeSinceMouseMove / 1200f,
								0.2f,
								-0.1f
						);
					}

					if(!SafechatConfig.CHAT_FIELD_FILL_AFFECTS_OPACITY.get()) {
						return (float) dist * 0.9f + opacityBonus;
					} else {
						return Math.max((float) dist * 0.9f, getChatFillOpacity()) + opacityBonus;
					}
				}
			}

			private float getChatFillOpacity() {
				if(!SafechatConfig.CHAT_FIELD_FILL_AFFECTS_OPACITY.get()) {
					return 1f;
				}
				int displayPos = ((EditBoxAccessor) input).safechat$getDisplayPos();
				int displayedWidth = font.width(input.getValue().substring(displayPos));
				if(displayedWidth == 0) return 1f;

				float cover = ((float) (displayedWidth - 90) / (float) (input.getInnerWidth() - 80));
				return 1f - Math.min(1f, cover);
			}

			// Don't allow arrow keys
			@Override
			public ComponentPath nextFocusPath(FocusNavigationEvent event) {
				return null;
			}
		};
	}

	protected MixinChatScreen(Component title) {
		super(title);
		throw new AssertionError("nuh uh");
	}
}