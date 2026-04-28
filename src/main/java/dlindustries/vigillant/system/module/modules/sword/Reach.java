package dlindustries.vigillant.system.module.modules.sword;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;
import java.util.function.Predicate;

public final class Reach extends Module implements TickListener {
	private final NumberSetting attackRange = new NumberSetting(EncryptedString.of("Attack Range"), 3.0, 6.0, 3.5, 0.1)
			.setDescription(EncryptedString.of("Maximum attack reach distance"));
	private final NumberSetting interactRange = new NumberSetting(EncryptedString.of("Interact Range"), 3.0, 6.0, 4.5, 0.1)
			.setDescription(EncryptedString.of("Maximum interact reach distance"));

	private Entity reachTarget;

	public Reach() {
		super(EncryptedString.of("Reach"),
				EncryptedString.of("Extends your attack and interaction reach"),
				-1,
				Category.sword);
		addSettings(attackRange, interactRange);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		reachTarget = null;
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		reachTarget = null;
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.world == null) return;

		double reach = attackRange.getValue();
		Vec3d eyePos = mc.player.getEyePos();
		Vec3d lookVec = mc.player.getRotationVec(1.0f);
		Vec3d endVec = eyePos.add(lookVec.multiply(reach));

		Box searchBox = mc.player.getBoundingBox().stretch(lookVec.multiply(reach)).expand(1.0);
		Predicate<Entity> filter = e -> e instanceof LivingEntity && e != mc.player && !e.isSpectator() && e.canHit();

		EntityHitResult result = raycastEntities(eyePos, endVec, searchBox, filter, reach * reach);
		reachTarget = result != null ? result.getEntity() : null;
	}

	private EntityHitResult raycastEntities(Vec3d start, Vec3d end, Box box, Predicate<Entity> filter, double maxDistSq) {
		if (mc.world == null) return null;

		Entity closest = null;
		double closestDist = maxDistSq;

		for (Entity entity : mc.world.getOtherEntities(mc.player, box, filter)) {
			Box entityBox = entity.getBoundingBox().expand(entity.getTargetingMargin());
			Optional<Vec3d> hit = entityBox.raycast(start, end);

			if (hit.isPresent()) {
				double dist = start.squaredDistanceTo(hit.get());
				if (dist < closestDist) {
					closest = entity;
					closestDist = dist;
				}
			} else if (entityBox.contains(start)) {
				if (closestDist >= 0.0) {
					closest = entity;
					closestDist = 0.0;
				}
			}
		}

		return closest != null ? new EntityHitResult(closest) : null;
	}

	public double getAttackRange() {
		return isEnabled() ? attackRange.getValue() : 3.0;
	}

	public double getInteractRange() {
		return isEnabled() ? interactRange.getValue() : 4.5;
	}

	public Entity getReachTarget() {
		return reachTarget;
	}
}
