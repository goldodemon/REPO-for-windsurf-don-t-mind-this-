package dlindustries.vigillant.system.module.modules.sword;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import java.util.Random;

public final class TargetStrafe extends Module implements TickListener {
	private final NumberSetting speed = new NumberSetting(EncryptedString.of("Speed"), 0.1, 1.0, 0.4, 0.05)
			.setDescription(EncryptedString.of("Strafe intensity (lower = more subtle, less detectable)"));
	private final NumberSetting switchInterval = new NumberSetting(EncryptedString.of("Switch Interval"), 3, 30, 8, 1)
			.setDescription(EncryptedString.of("Ticks between direction changes"));
	private final NumberSetting range = new NumberSetting(EncryptedString.of("Range"), 2.0, 6.0, 3.5, 0.5)
			.setDescription(EncryptedString.of("Max distance from target to activate strafing"));
	private final BooleanSetting randomize = new BooleanSetting(EncryptedString.of("Randomize"), true)
			.setDescription(EncryptedString.of("Add random variance to direction switches"));
	private final BooleanSetting onlyWhileHolding = new BooleanSetting(EncryptedString.of("Only While Holding W"), true)
			.setDescription(EncryptedString.of("Only strafe when holding forward key"));
	private final BooleanSetting avoidWalls = new BooleanSetting(EncryptedString.of("Avoid Walls"), true)
			.setDescription(EncryptedString.of("Reverse direction when about to hit a wall"));

	private final Random random2 = new Random();
	private int tickCounter;
	private boolean strafeRight;
	private int currentInterval;

	public TargetStrafe() {
		super(EncryptedString.of("Target Strafe"),
				EncryptedString.of("Subtly strafes around targets during combat"),
				-1,
				Category.sword);
		addSettings(speed, switchInterval, range, randomize, onlyWhileHolding, avoidWalls);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		tickCounter = 0;
		strafeRight = true;
		currentInterval = switchInterval.getValueInt();
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.currentScreen != null) return;

		if (onlyWhileHolding.getValue() && !mc.options.forwardKey.isPressed()) return;

		HitResult target = mc.crosshairTarget;
		Entity targetEntity = null;
		if (target instanceof EntityHitResult ehr) {
			targetEntity = ehr.getEntity();
		}

		if (targetEntity == null || !(targetEntity instanceof LivingEntity living)) return;

		double dist = mc.player.distanceTo(living);
		if (dist > range.getValue()) return;

		if (avoidWalls.getValue() && mc.player.horizontalCollision) {
			strafeRight = !strafeRight;
			tickCounter = 0;
		}

		tickCounter++;
		if (tickCounter >= currentInterval) {
			strafeRight = !strafeRight;
			tickCounter = 0;
			if (randomize.getValue()) {
				int base = switchInterval.getValueInt();
				currentInterval = base + random2.nextInt(Math.max(1, base / 2)) - base / 4;
				currentInterval = Math.max(2, currentInterval);
			} else {
				currentInterval = switchInterval.getValueInt();
			}
		}

		if (strafeRight) {
			mc.options.rightKey.setPressed(true);
			mc.options.leftKey.setPressed(false);
		} else {
			mc.options.leftKey.setPressed(true);
			mc.options.rightKey.setPressed(false);
		}
	}
}
