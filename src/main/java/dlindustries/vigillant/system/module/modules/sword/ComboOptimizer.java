package dlindustries.vigillant.system.module.modules.sword;

import dlindustries.vigillant.system.event.events.AttackListener;
import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.ModeSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;

import java.util.Random;

public final class ComboOptimizer extends Module implements TickListener, AttackListener {
	public enum TapMode {
		W_TAP("W-Tap"),
		S_TAP("S-Tap"),
		BOTH("Both");
		private final String name;
		TapMode(String name) { this.name = name; }
		@Override
		public String toString() { return name; }
	}

	private final ModeSetting<TapMode> tapMode = new ModeSetting<>(EncryptedString.of("Tap Mode"), TapMode.W_TAP, TapMode.class)
			.setDescription(EncryptedString.of("W-Tap releases W, S-Tap presses S, Both alternates"));
	private final NumberSetting tapChance = new NumberSetting(EncryptedString.of("Tap Chance"), 0, 100, 70, 5)
			.setDescription(EncryptedString.of("Percentage chance to tap after each hit"));
	private final NumberSetting releaseLength = new NumberSetting(EncryptedString.of("Release Ticks"), 1, 5, 2, 1)
			.setDescription(EncryptedString.of("How many ticks to release W or press S"));
	private final BooleanSetting adaptiveCombo = new BooleanSetting(EncryptedString.of("Adaptive"), true)
			.setDescription(EncryptedString.of("Increases tap rate when landing consecutive hits"));
	private final BooleanSetting sprintOptimize = new BooleanSetting(EncryptedString.of("Sprint Optimize"), true)
			.setDescription(EncryptedString.of("Ensures sprint is reset before each hit for max knockback"));
	private final NumberSetting comboThreshold = new NumberSetting(EncryptedString.of("Combo Threshold"), 1, 10, 3, 1)
			.setDescription(EncryptedString.of("Consecutive hits before increasing tap rate"));
	private final BooleanSetting onlyOnGround = new BooleanSetting(EncryptedString.of("Only Ground"), true)
			.setDescription(EncryptedString.of("Only tap while on ground"));

	private final Random random = new Random();
	private int consecutiveHits;
	private int releaseTicks;
	private boolean tapping;
	private boolean doingSTap;
	private long lastHitTime;
	private boolean lastWasSTap;

	public ComboOptimizer() {
		super(EncryptedString.of("Combo Optimizer"),
				EncryptedString.of("Optimizes sprint resets with W-tap and S-tap for maximum combo potential"),
				-1,
				Category.sword);
		addSettings(tapMode, tapChance, releaseLength, adaptiveCombo, sprintOptimize, comboThreshold, onlyOnGround);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		eventManager.add(AttackListener.class, this);
		consecutiveHits = 0;
		releaseTicks = 0;
		tapping = false;
		doingSTap = false;
		lastHitTime = 0;
		lastWasSTap = false;
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		eventManager.remove(AttackListener.class, this);
		if (mc.options != null) {
			if (tapping && doingSTap) {
				mc.options.backKey.setPressed(false);
			}
			// Always restore forward key in case disable happened mid W-tap.
			mc.options.forwardKey.setPressed(false);
		}
		tapping = false;
		doingSTap = false;
		releaseTicks = 0;
		super.onDisable();
	}

	@Override
	public void onAttack(AttackEvent event) {
		if (mc.player == null || mc.options == null) return;
		if (tapping) return;

		if (!(mc.crosshairTarget instanceof EntityHitResult ehr)) return;
		if (!(ehr.getEntity() instanceof LivingEntity)) return;
		if (onlyOnGround.getValue() && !mc.player.isOnGround()) return;
		if (!mc.player.isSprinting()) return;

		long now = System.currentTimeMillis();
		if (now - lastHitTime < 2000) {
			consecutiveHits++;
		} else {
			consecutiveHits = 1;
		}
		lastHitTime = now;

		double chance = tapChance.getValue();
		if (adaptiveCombo.getValue() && consecutiveHits >= comboThreshold.getValueInt()) {
			chance = Math.min(chance + (consecutiveHits - comboThreshold.getValueInt()) * 10, 100);
		}

		if (random.nextInt(100) < (int) chance) {
			tapping = true;
			releaseTicks = releaseLength.getValueInt();

			TapMode mode = tapMode.getMode();
			if (mode == TapMode.W_TAP) {
				doingSTap = false;
			} else if (mode == TapMode.S_TAP) {
				doingSTap = true;
			} else {
				doingSTap = !lastWasSTap;
				lastWasSTap = doingSTap;
			}

			if (doingSTap) {
				mc.options.backKey.setPressed(true);
			} else {
				mc.options.forwardKey.setPressed(false);
			}
		} else if (sprintOptimize.getValue()) {
			mc.options.forwardKey.setPressed(false);
			tapping = true;
			doingSTap = false;
			releaseTicks = 1;
		}
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.currentScreen != null || mc.options == null) return;

		if (tapping) {
			if (doingSTap) {
				mc.options.backKey.setPressed(true);
			} else {
				mc.options.forwardKey.setPressed(false);
			}
			releaseTicks--;
			if (releaseTicks <= 0) {
				tapping = false;
				if (doingSTap) {
					mc.options.backKey.setPressed(false);
				} else {
					mc.options.forwardKey.setPressed(false);
				}
			}
		}
	}
}
