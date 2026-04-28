package dlindustries.vigillant.system.module.modules.crystal;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.BlockUtils;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.InventoryUtils;
import dlindustries.vigillant.system.utils.KeyUtils;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public final class AnchorOptimizer extends Module implements TickListener {
	private final NumberSetting placeDelay = new NumberSetting(EncryptedString.of("Place Delay"), 0, 5, 0, 1)
			.setDescription(EncryptedString.of("Ticks between anchor placement"));
	private final NumberSetting chargeDelay = new NumberSetting(EncryptedString.of("Charge Delay"), 0, 5, 0, 1)
			.setDescription(EncryptedString.of("Ticks between glowstone charging"));
	private final NumberSetting detonateDelay = new NumberSetting(EncryptedString.of("Detonate Delay"), 0, 5, 0, 1)
			.setDescription(EncryptedString.of("Ticks between detonation"));
	private final BooleanSetting autoSwitch = new BooleanSetting(EncryptedString.of("Auto Switch"), true)
			.setDescription(EncryptedString.of("Automatically switch between anchor and glowstone"));
	private final BooleanSetting instantDetonate = new BooleanSetting(EncryptedString.of("Instant Detonate"), true)
			.setDescription(EncryptedString.of("Detonate anchor immediately after charging"));
	private final BooleanSetting antiWaste = new BooleanSetting(EncryptedString.of("Anti Waste"), true)
			.setDescription(EncryptedString.of("Don't place if already a charged anchor at crosshair"));
	private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true)
			.setDescription(EncryptedString.of("Switch back to original slot after cycle"));

	private int placeTicks;
	private int chargeTicks;
	private int detonateTicks;
	private int originalSlot = -1;

	private enum State {
		IDLE, PLACED, CHARGED
	}

	private State state = State.IDLE;

	public AnchorOptimizer() {
		super(EncryptedString.of("Anchor Optimizer"),
				EncryptedString.of("Optimizes anchor PvP for instant-feel placement and detonation"),
				-1,
				Category.optimizer);
		addSettings(placeDelay, chargeDelay, detonateDelay, autoSwitch, instantDetonate, antiWaste, switchBack);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		state = State.IDLE;
		placeTicks = 0;
		chargeTicks = 0;
		detonateTicks = 0;
		originalSlot = -1;
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		if (switchBack.getValue() && originalSlot != -1 && mc.player != null) {
			mc.player.getInventory().setSelectedSlot(originalSlot);
		}
		state = State.IDLE;
		originalSlot = -1;
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
		if (!KeyUtils.isKeyPressed(GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
			state = State.IDLE;
			if (switchBack.getValue() && originalSlot != -1) {
				mc.player.getInventory().setSelectedSlot(originalSlot);
				originalSlot = -1;
			}
			return;
		}
		if (!(mc.crosshairTarget instanceof BlockHitResult hit)) return;
		if (hit.getType() == HitResult.Type.MISS) return;

		BlockPos pos = hit.getBlockPos();
		boolean isAnchor = BlockUtils.isBlock(pos, Blocks.RESPAWN_ANCHOR);

		switch (state) {
			case IDLE -> {
				if (isAnchor) {
					if (antiWaste.getValue() && !BlockUtils.isAnchorNotCharged(pos)) {
						handleDetonate(pos);
						return;
					}
					handleCharge(pos);
				} else {
					handlePlace();
				}
			}
			case PLACED -> handleCharge(pos);
			case CHARGED -> handleDetonate(pos);
		}
	}

	private void handlePlace() {
		if (placeTicks < placeDelay.getValueInt()) {
			placeTicks++;
			return;
		}
		placeTicks = 0;

		if (autoSwitch.getValue()) {
			if (originalSlot == -1) {
				originalSlot = mc.player.getInventory().getSelectedSlot();
			}
			if (!mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) {
				if (!InventoryUtils.selectItemFromHotbar(Items.RESPAWN_ANCHOR)) return;
			}
		}

		state = State.PLACED;
	}

	private void handleCharge(BlockPos pos) {
		if (!BlockUtils.isBlock(pos, Blocks.RESPAWN_ANCHOR)) return;
		if (!BlockUtils.isAnchorNotCharged(pos)) {
			state = State.CHARGED;
			if (instantDetonate.getValue()) {
				handleDetonate(pos);
			}
			return;
		}

		if (chargeTicks < chargeDelay.getValueInt()) {
			chargeTicks++;
			return;
		}
		chargeTicks = 0;

		if (autoSwitch.getValue()) {
			if (originalSlot == -1) {
				originalSlot = mc.player.getInventory().getSelectedSlot();
			}
			if (!mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
				if (!InventoryUtils.selectItemFromHotbar(Items.GLOWSTONE)) return;
			}
		}
	}

	private void handleDetonate(BlockPos pos) {
		if (!BlockUtils.isBlock(pos, Blocks.RESPAWN_ANCHOR)) return;
		if (BlockUtils.isAnchorNotCharged(pos)) return;

		if (detonateTicks < detonateDelay.getValueInt()) {
			detonateTicks++;
			return;
		}
		detonateTicks = 0;

		if (autoSwitch.getValue()) {
			if (originalSlot == -1) {
				originalSlot = mc.player.getInventory().getSelectedSlot();
			}
			if (mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
				int anchorSlot = findNonGlowstoneSlot();
				if (anchorSlot != -1) {
					mc.player.getInventory().setSelectedSlot(anchorSlot);
				}
			}
		}

		state = State.IDLE;

		if (switchBack.getValue() && originalSlot != -1) {
			mc.player.getInventory().setSelectedSlot(originalSlot);
			originalSlot = -1;
		}
	}

	private int findNonGlowstoneSlot() {
		for (int i = 0; i < 9; i++) {
			if (!mc.player.getInventory().getStack(i).isOf(Items.GLOWSTONE)
					&& !mc.player.getInventory().getStack(i).isOf(Items.RESPAWN_ANCHOR)) {
				return i;
			}
		}
		return mc.player.getInventory().getSelectedSlot();
	}
}
