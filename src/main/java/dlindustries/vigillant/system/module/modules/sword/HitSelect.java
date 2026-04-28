package dlindustries.vigillant.system.module.modules.sword;

import dlindustries.vigillant.system.event.events.AttackListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;

public final class HitSelect extends Module implements AttackListener {
	private final NumberSetting minCooldown = new NumberSetting(EncryptedString.of("Min Cooldown"), 0.5, 1.0, 0.9, 0.05)
			.setDescription(EncryptedString.of("Minimum attack cooldown progress to allow a hit (1.0 = full)"));
	private final BooleanSetting onlyCrit = new BooleanSetting(EncryptedString.of("Only Crits"), false)
			.setDescription(EncryptedString.of("Only allows hits that would be critical (falling, not on ground)"));
	private final BooleanSetting antiWaste = new BooleanSetting(EncryptedString.of("Anti Waste"), true)
			.setDescription(EncryptedString.of("Cancels swings that would deal reduced damage"));

	public HitSelect() {
		super(EncryptedString.of("Hit Select"),
				EncryptedString.of("Only allows attacks at optimal cooldown timing for maximum damage"),
				-1,
				Category.sword);
		addSettings(minCooldown, onlyCrit, antiWaste);
	}

	@Override
	public void onEnable() {
		eventManager.add(AttackListener.class, this);
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(AttackListener.class, this);
		super.onDisable();
	}

	@Override
	public void onAttack(AttackEvent event) {
		if (mc.player == null) return;

		if (!(mc.crosshairTarget instanceof EntityHitResult ehr)) return;
		if (!(ehr.getEntity() instanceof LivingEntity)) return;

		float cooldown = mc.player.getAttackCooldownProgress(0.5f);

		if (antiWaste.getValue() && cooldown < minCooldown.getValue()) {
			event.cancel();
			return;
		}

		if (onlyCrit.getValue()) {
			boolean canCrit = !mc.player.isOnGround()
					&& mc.player.getVelocity().y < 0
					&& !mc.player.isClimbing()
					&& !mc.player.isTouchingWater()
					&& !mc.player.hasVehicle()
					&& cooldown >= 0.9f;
			if (!canCrit) {
				event.cancel();
			}
		}
	}
}
