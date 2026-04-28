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

public final class CPSRandomizer extends Module implements TickListener {
	private final NumberSetting minCPS = new NumberSetting(EncryptedString.of("Min CPS"), 1, 20, 8, 1)
			.setDescription(EncryptedString.of("Minimum clicks per second"));
	private final NumberSetting maxCPS = new NumberSetting(EncryptedString.of("Max CPS"), 1, 20, 12, 1)
			.setDescription(EncryptedString.of("Maximum clicks per second"));
	private final BooleanSetting onlyInCombat = new BooleanSetting(EncryptedString.of("Only In Combat"), true)
			.setDescription(EncryptedString.of("Only clicks when looking at a living entity"));
	private final BooleanSetting burstMode = new BooleanSetting(EncryptedString.of("Burst Mode"), false)
			.setDescription(EncryptedString.of("Occasionally bursts to higher CPS then drops, mimicking human patterns"));
	private final NumberSetting jitter = new NumberSetting(EncryptedString.of("Jitter"), 0, 50, 15, 1)
			.setDescription(EncryptedString.of("Random variance in ms between clicks (higher = more human-like)"));

	private final Random random = new Random();
	private long nextClickTime;
	private int burstCounter;
	private boolean inBurst;

	public CPSRandomizer() {
		super(EncryptedString.of("CPS Randomizer"),
				EncryptedString.of("Randomizes click timing with human-like patterns to bypass autoclicker detection"),
				-1,
				Category.sword);
		addSettings(minCPS, maxCPS, onlyInCombat, burstMode, jitter);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		nextClickTime = System.currentTimeMillis();
		burstCounter = 0;
		inBurst = false;
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

		if (onlyInCombat.getValue()) {
			HitResult target = mc.crosshairTarget;
			if (!(target instanceof EntityHitResult ehr)) return;
			Entity entity = ehr.getEntity();
			if (!(entity instanceof LivingEntity)) return;
		}

		long now = System.currentTimeMillis();
		if (now < nextClickTime) return;

		dlindustries.vigillant.system.utils.MouseSimulation.mouseClick(org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT);

		double cpsMin = Math.min(minCPS.getValue(), maxCPS.getValue());
		double cpsMax = Math.max(minCPS.getValue(), maxCPS.getValue());

		if (burstMode.getValue()) {
			if (!inBurst && random.nextInt(100) < 8) {
				inBurst = true;
				burstCounter = 3 + random.nextInt(5);
			}
			if (inBurst) {
				cpsMax = Math.min(cpsMax + 2, 20);
				burstCounter--;
				if (burstCounter <= 0) inBurst = false;
			}
		}

		double targetCPS = cpsMin + random.nextDouble() * (cpsMax - cpsMin);
		long baseDelay = (long) (1000.0 / targetCPS);
		long jitterMs = (long) (jitter.getValue());
		long variance = jitterMs > 0 ? (random.nextLong(jitterMs * 2 + 1) - jitterMs) : 0;

		nextClickTime = now + Math.max(baseDelay + variance, 30);
	}
}
