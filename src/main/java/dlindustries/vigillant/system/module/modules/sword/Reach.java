package dlindustries.vigillant.system.module.modules.sword;

import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;

public final class Reach extends Module {
	private final NumberSetting attackRange = new NumberSetting(EncryptedString.of("Attack Range"), 3.0, 6.0, 3.5, 0.1)
			.setDescription(EncryptedString.of("Maximum attack reach distance"));
	private final NumberSetting interactRange = new NumberSetting(EncryptedString.of("Interact Range"), 3.0, 6.0, 4.5, 0.1)
			.setDescription(EncryptedString.of("Maximum interact reach distance"));

	public Reach() {
		super(EncryptedString.of("Reach"),
				EncryptedString.of("Extends your attack and interaction reach"),
				-1,
				Category.BLATANT);
		addSettings(attackRange, interactRange);
	}

	public double getAttackRange() {
		return attackRange.getValue();
	}

	public double getInteractRange() {
		return interactRange.getValue();
	}
}
