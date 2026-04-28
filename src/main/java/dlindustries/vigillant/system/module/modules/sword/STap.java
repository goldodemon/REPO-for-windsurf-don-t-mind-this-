package dlindustries.vigillant.system.module.modules.sword;

import dlindustries.vigillant.system.event.events.HudListener;
import dlindustries.vigillant.system.event.events.PacketSendListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.MinMaxSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.TimerUtils;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class STap extends Module implements PacketSendListener, HudListener {
	private final MinMaxSetting releaseDelay = new MinMaxSetting(EncryptedString.of("Release Delay"), 0, 500, 1, 40, 80);
	private final MinMaxSetting repressDelay = new MinMaxSetting(EncryptedString.of("Repress Delay"), 0, 500, 1, 40, 80);
	private final BooleanSetting onlyWhileSprinting = new BooleanSetting(EncryptedString.of("Only Sprinting"), true)
			.setDescription(EncryptedString.of("Only S-tap while sprinting"));
	private final BooleanSetting onlyOnGround = new BooleanSetting(EncryptedString.of("Only Ground"), true)
			.setDescription(EncryptedString.of("Only S-tap while on ground"));

	private final TimerUtils tapTimer = new TimerUtils();
	private boolean releasing;
	private boolean repressing;
	private int currentReleaseDelay;
	private int currentRepressDelay;

	public STap() {
		super(EncryptedString.of("S-Tap"),
				EncryptedString.of("Releases and re-presses S to reset sprint for extra knockback"),
				-1,
				Category.sword);
		addSettings(releaseDelay, repressDelay, onlyWhileSprinting, onlyOnGround);
	}

	@Override
	public void onEnable() {
		eventManager.add(PacketSendListener.class, this);
		eventManager.add(HudListener.class, this);
		releasing = false;
		repressing = false;
		currentReleaseDelay = releaseDelay.getRandomValueInt();
		currentRepressDelay = repressDelay.getRandomValueInt();
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(PacketSendListener.class, this);
		eventManager.remove(HudListener.class, this);
		if (releasing || repressing) {
			mc.options.backKey.setPressed(false);
		}
		releasing = false;
		repressing = false;
		super.onDisable();
	}

	@Override
	public void onRenderHud(HudEvent event) {
		if (mc.player == null) return;
		if (mc.currentScreen != null) return;

		if (releasing && tapTimer.delay(currentReleaseDelay)) {
			mc.options.backKey.setPressed(false);
			repressing = true;
			releasing = false;
			tapTimer.reset();
		}

		if (repressing && tapTimer.delay(currentRepressDelay)) {
			repressing = false;
			currentReleaseDelay = releaseDelay.getRandomValueInt();
			currentRepressDelay = repressDelay.getRandomValueInt();
		}
	}

	@Override
	public void onPacketSend(PacketSendEvent event) {
		if (!(event.packet instanceof PlayerInteractEntityC2SPacket packet))
			return;

		packet.handle(new PlayerInteractEntityC2SPacket.Handler() {
			@Override
			public void interact(Hand hand) {}

			@Override
			public void interactAt(Hand hand, Vec3d pos) {}

			@Override
			public void attack() {
				if (mc.player == null) return;
				if (onlyOnGround.getValue() && !mc.player.isOnGround()) return;
				if (onlyWhileSprinting.getValue() && !mc.player.isSprinting()) return;
				if (releasing || repressing) return;

				if (GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_W) == 1) {
					mc.options.backKey.setPressed(true);
					releasing = true;
					tapTimer.reset();
				}
			}
		});
	}
}
