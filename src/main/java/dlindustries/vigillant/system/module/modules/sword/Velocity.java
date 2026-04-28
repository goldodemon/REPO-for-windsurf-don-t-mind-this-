package dlindustries.vigillant.system.module.modules.sword;

import dlindustries.vigillant.system.event.events.PacketReceiveListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;

public final class Velocity extends Module implements PacketReceiveListener {
	private final NumberSetting horizontal = new NumberSetting(EncryptedString.of("Horizontal"), 0, 100, 0, 1)
			.setDescription(EncryptedString.of("Horizontal knockback percentage (0 = none)"));
	private final NumberSetting vertical = new NumberSetting(EncryptedString.of("Vertical"), 0, 100, 0, 1)
			.setDescription(EncryptedString.of("Vertical knockback percentage (0 = none)"));

	public Velocity() {
		super(EncryptedString.of("Velocity"),
				EncryptedString.of("Reduces or cancels incoming knockback"),
				-1,
				Category.sword);
		addSettings(horizontal, vertical);
	}

	@Override
	public void onEnable() {
		eventManager.add(PacketReceiveListener.class, this);
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(PacketReceiveListener.class, this);
		super.onDisable();
	}

	@Override
	public void onPacketReceive(PacketReceiveEvent event) {
		if (mc.player == null) return;

		if (event.packet instanceof EntityVelocityUpdateS2CPacket packet) {
			if (packet.getEntityId() == mc.player.getId()) {
				double hPercent = horizontal.getValue() / 100.0;
				double vPercent = vertical.getValue() / 100.0;

				if (hPercent == 0 && vPercent == 0) {
					event.cancel();
					return;
				}

				mc.player.setVelocity(
						mc.player.getVelocity().x * hPercent,
						mc.player.getVelocity().y * vPercent,
						mc.player.getVelocity().z * hPercent
				);
				event.cancel();
			}
		}

		if (event.packet instanceof ExplosionS2CPacket) {
			double hPercent = horizontal.getValue() / 100.0;
			double vPercent = vertical.getValue() / 100.0;

			if (hPercent == 0 && vPercent == 0) {
				event.cancel();
				return;
			}

			mc.player.setVelocity(
					mc.player.getVelocity().x * hPercent,
					mc.player.getVelocity().y * vPercent,
					mc.player.getVelocity().z * hPercent
			);
			event.cancel();
		}
	}
}
