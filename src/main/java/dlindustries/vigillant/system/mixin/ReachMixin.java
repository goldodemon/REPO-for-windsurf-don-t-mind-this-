package dlindustries.vigillant.system.mixin;

import dlindustries.vigillant.system.module.modules.sword.Reach;
import dlindustries.vigillant.system.system;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class ReachMixin {

	@Inject(method = "getEntityInteractionRange", at = @At("RETURN"), cancellable = true)
	private void onGetEntityInteractionRange(CallbackInfoReturnable<Double> cir) {
		if (system.INSTANCE == null || system.INSTANCE.getModuleManager() == null) return;
		Reach reach = system.INSTANCE.getModuleManager().getModule(Reach.class);
		if (reach != null && reach.isEnabled()) {
			cir.setReturnValue(reach.getAttackRange());
		}
	}

	@Inject(method = "getBlockInteractionRange", at = @At("RETURN"), cancellable = true)
	private void onGetBlockInteractionRange(CallbackInfoReturnable<Double> cir) {
		if (system.INSTANCE == null || system.INSTANCE.getModuleManager() == null) return;
		Reach reach = system.INSTANCE.getModuleManager().getModule(Reach.class);
		if (reach != null && reach.isEnabled()) {
			cir.setReturnValue(reach.getInteractRange());
		}
	}
}
