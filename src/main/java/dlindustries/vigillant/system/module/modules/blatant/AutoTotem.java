package dlindustries.vigillant.system.module.modules.blatant;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.InventoryUtils;
import dlindustries.vigillant.system.utils.TimerUtils;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public final class AutoTotem extends Module implements TickListener {
	private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 0, 10, 0, 1)
			.setDescription(EncryptedString.of("Ticks between totem swaps"));
	private final BooleanSetting alwaysReplace = new BooleanSetting(EncryptedString.of("Always Replace"), true)
			.setDescription(EncryptedString.of("Replace offhand even if holding another item"));
	private final BooleanSetting mainHand = new BooleanSetting(EncryptedString.of("Main Hand"), false)
			.setDescription(EncryptedString.of("Also keep a totem in your selected hotbar slot"));

	private int tickCounter;

	public AutoTotem() {
		super(EncryptedString.of("Auto Totem"),
				EncryptedString.of("Automatically moves totems to your offhand"),
				-1,
				Category.BLATANT);
		addSettings(delay, alwaysReplace, mainHand);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		tickCounter = 0;
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

		if (tickCounter < delay.getValueInt()) {
			tickCounter++;
			return;
		}

		if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
			if (!alwaysReplace.getValue() && !mc.player.getOffHandStack().isEmpty()) return;

			int totemSlot = findTotemInInventory();
			if (totemSlot != -1) {
				mc.interactionManager.clickSlot(
						mc.player.playerScreenHandler.syncId,
						totemSlot, 40, SlotActionType.SWAP, mc.player);
				tickCounter = 0;
				return;
			}
		}

		if (mainHand.getValue() && mc.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
			int totemSlot = findTotemInInventory();
			if (totemSlot != -1) {
				int selected = mc.player.getInventory().getSelectedSlot();
				mc.interactionManager.clickSlot(
						mc.player.playerScreenHandler.syncId,
						totemSlot, selected, SlotActionType.SWAP, mc.player);
				tickCounter = 0;
			}
		}
	}

	private int findTotemInInventory() {
		for (int i = 9; i < 45; i++) {
			if (mc.player.playerScreenHandler.getSlot(i).getStack().getItem() == Items.TOTEM_OF_UNDYING) {
				return i;
			}
		}
		for (int i = 0; i < 9; i++) {
			if (i == mc.player.getInventory().getSelectedSlot()) continue;
			if (mc.player.playerScreenHandler.getSlot(36 + i).getStack().getItem() == Items.TOTEM_OF_UNDYING) {
				return 36 + i;
			}
		}
		return -1;
	}
}
