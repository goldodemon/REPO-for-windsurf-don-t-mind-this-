package dlindustries.vigillant.system.utils;

import net.fabricmc.loader.api.FabricLoader;

public final class VulkanCompat {
	private static final String VULKAN_MOD_ID = "vulkanmod";
	private static final boolean VULKAN_LOADED =
			FabricLoader.getInstance().isModLoaded(VULKAN_MOD_ID);

	private VulkanCompat() {
	}

	public static boolean isVulkanLoaded() {
		return VULKAN_LOADED;
	}
}
