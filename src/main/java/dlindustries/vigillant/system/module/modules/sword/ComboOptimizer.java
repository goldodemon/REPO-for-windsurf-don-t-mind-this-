package dlindustries.vigillant.system.module.modules.sword;

import dlindustries.vigillant.system.event.events.AttackListener;
import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;

import java.util.Random;

public final class ComboOptimizer extends Module implements TickListener, AttackListener {
	private final NumberSetting wtapChance = new NumberSetting(EncryptedString.of("W-Tap Chance"), 0, 100, 70, 5)
			.setDescription(EncryptedString.of("Percentage chance to W-tap after each hit"));
	private final NumberSetting releaseLength = new NumberSetting(EncryptedString.of("Release Ticks"), 1, 5, 2, 1)
			.setDescription(EncryptedString.of("How many ticks to release W during a W-tap"));
	private final BooleanSetting adaptiveCombo = new BooleanSetting(EncryptedString.of("Adaptive"), true)
			.setDescription(EncryptedString.of("Increases W-tap rate when landing consecutive hits"));
	private final BooleanSetting sprintOptimize = new BooleanSetting(EncryptedString.of("Sprint Optimize"), true)
			.setDescription(EncryptedString.of("Ensures sprint is reset before each hit for max knockback"));
	private final NumberSetting comboThreshold = new NumberSetting(EncryptedString.of("Combo Threshold"), 1, 10, 3, 1)
			.setDescription(EncryptedString.of("Consecutive hits before increasing W-tap rate"));

	private final Random random = new Random();
	private int consecutiveHits;
	private int releaseTicks;
	private boolean releasing;
	private long lastHitTime;

	public ComboOptimizer() {
		super(EncryptedString.of("Combo Optimizer"),
				EncryptedString.of("Optimizes sprint resets and W-tapping for maximum combo potential"),
				-1,
				Category.sword);
		addSettings(wtapChance, releaseLength, adaptiveCombo, sprintOptimize, comboThreshold);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		eventManager.add(AttackListener.class, this);
		consecutiveHits = 0;
		releaseTicks = 0;
		releasing = false;
		lastHitTime = 0;
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		eventManager.remove(AttackListener.class, this);
		super.onDisable();
	}

	@Override
	public void onAttack(AttackEvent event) {
		if (mc.player == null) return;

		if (!(mc.crosshairTarget instanceof EntityHitResult ehr)) return;
		if (!(ehr.getEntity() instanceof LivingEntity)) return;

		long now = System.currentTimeMillis();
		if (now - lastHitTime < 2000) {
			consecutiveHits++;
		} else {
			consecutiveHits = 1;
		}
		lastHitTime = now;

		double chance = wtapChance.getValue();
		if (adaptiveCombo.getValue() && consecutiveHits >= comboThreshold.getValueInt()) {
			chance = Math.min(chance + (consecutiveHits - comboThreshold.getValueInt()) * 10, 100);
		}

		if (random.nextInt(100) < (int) chance) {
			releasing = true;
			releaseTicks = releaseLength.getValueInt();
		}

		if (sprintOptimize.getValue() && mc.player.isSprinting()) {
			mc.options.forwardKey.setPressed(false);
			releasing = true;
			releaseTicks = Math.max(releaseTicks, 1);
		}
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.currentScreen != null) return;

		if (releasing) {
			mc.options.forwardKey.setPressed(false);
			releaseTicks--;
			if (releaseTicks <= 0) {
				releasing = false;
				mc.options.forwardKey.setPressed(true);
			}
		}
	}
}
