package dlindustries.vigillant.system.gui;

import dlindustries.vigillant.system.system;
import dlindustries.vigillant.system.gui.render.GlassRenderer;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.modules.client.ClickGUI;
import dlindustries.vigillant.system.utils.ColorUtils;
import dlindustries.vigillant.system.utils.RenderUtils;
import dlindustries.vigillant.system.utils.TextRenderer;
import dlindustries.vigillant.system.utils.Utils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static dlindustries.vigillant.system.system.mc;

public final class ClickGui extends Screen {

	private static final int GUI_WIDTH = 520;
	private static final int GUI_HEIGHT = 380;
	private static final int SIDEBAR_WIDTH = 140;
	private static final int HEADER_HEIGHT = 50;
	private static final int CATEGORY_BUTTON_HEIGHT = 32;
	private static final int CATEGORY_PADDING = 4;
	private static final int MODULE_BUTTON_HEIGHT = 30;
	private static final int MODULE_PADDING = 3;
	private static final int CORNER_RADIUS = 12;
	private static final int SCROLL_SPEED = 20;

	private static final Color GOLD = new Color(255, 215, 0);
	private static final Color TEXT_WHITE = new Color(230, 230, 230);
	private static final Color TEXT_DIM = new Color(160, 160, 160);
	private static final Color SEPARATOR = new Color(255, 255, 255, 20);
	private static final Identifier LOGO_TEXTURE = Identifier.of("system", "icon.png");

	private Category activeCategory;
	private final List<ModuleEntry> moduleEntries = new ArrayList<>();
	private int guiX, guiY;
	private int scrollOffset;
	private int maxScroll;
	private boolean dragging;
	private int dragOffsetX, dragOffsetY;
	private float[] categoryHoverProgress;
	private float[] moduleHoverProgress;
	private long lastRenderTime;
	private TextFieldWidget searchField;

	public Color currentColor;
	public List<Window> windows = new ArrayList<>();

	public ClickGui() {
		super(Text.empty());
		activeCategory = Category.values()[0];
		categoryHoverProgress = new float[Category.values().length];
		rebuildModuleList();

		for (Category category : Category.values()) {
			windows.add(new Window(0, 0, 230, 30, category, this));
		}
	}

	@Override
	public void init() {
		guiX = (mc.getWindow().getScaledWidth() - GUI_WIDTH) / 2;
		guiY = (mc.getWindow().getScaledHeight() - GUI_HEIGHT) / 2;
		lastRenderTime = System.currentTimeMillis();
		scrollOffset = 0;

		int searchX = guiX + SIDEBAR_WIDTH + 10;
		int searchY = guiY + 8;
		int searchW = GUI_WIDTH - SIDEBAR_WIDTH - 20;
		searchField = new TextFieldWidget(
				mc.textRenderer, searchX, searchY, searchW, 14,
				Text.literal("Search modules...")
		);
		searchField.setMaxLength(50);
		searchField.setDrawsBackground(false);
		searchField.setPlaceholder(Text.literal("Search..."));
		addSelectableChild(searchField);
	}

	public String getSearchQuery() {
		return searchField != null ? searchField.getText() : "";
	}

	public boolean isDraggingAlready() {
		return dragging;
	}

	private void rebuildModuleList() {
		moduleEntries.clear();
		List<Module> modules = system.INSTANCE.getModuleManager().getModulesInCategory(activeCategory);
		for (Module module : modules) {
			moduleEntries.add(new ModuleEntry(module));
		}
		moduleHoverProgress = new float[moduleEntries.size()];
		scrollOffset = 0;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		if (mc.currentScreen != this) return;

		long now = System.currentTimeMillis();
		float dt = Math.min((now - lastRenderTime) / 1000.0f, 0.05f);
		lastRenderTime = now;

		mc.gameRenderer.renderBlur();

		float scaleFactor = (float) mc.getWindow().getScaleFactor();
		float invScale = 1.0f / scaleFactor;
		context.getMatrices().pushMatrix();
		context.getMatrices().scale(invScale, invScale);

		int pMouseX = (int) (mouseX * scaleFactor);
		int pMouseY = (int) (mouseY * scaleFactor);

		int pGuiX = (int) (guiX * scaleFactor);
		int pGuiY = (int) (guiY * scaleFactor);
		int pGuiW = (int) (GUI_WIDTH * scaleFactor);
		int pGuiH = (int) (GUI_HEIGHT * scaleFactor);
		int pSidebarW = (int) (SIDEBAR_WIDTH * scaleFactor);
		int pHeaderH = (int) (HEADER_HEIGHT * scaleFactor);
		int pCorner = (int) (CORNER_RADIUS * scaleFactor);

		context.fill(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight(), new Color(0, 0, 0, 120).getRGB());

		GlassRenderer.renderRoundedRect(context, pGuiX, pGuiY, pGuiW, pGuiH,
				new Color(10, 10, 10, 102), pCorner);

		GlassRenderer.renderRoundedBorder(context, pGuiX, pGuiY, pGuiW, pGuiH,
				new Color(255, 255, 255, 25), pCorner, 1.5);

		GlassRenderer.renderMouseGlow(context, pMouseX, pMouseY, pGuiX, pGuiY, pGuiW, pGuiH);

		renderSidebar(context, pGuiX, pGuiY, pSidebarW, pGuiH, pMouseX, pMouseY, dt, scaleFactor);

		renderModulePanel(context, pGuiX + pSidebarW, pGuiY, pGuiW - pSidebarW, pGuiH, pMouseX, pMouseY, dt, scaleFactor);

		renderCredits(context, pGuiX, pGuiY, pGuiW, pGuiH, scaleFactor);

		context.getMatrices().popMatrix();

		if (searchField != null) {
			searchField.setX(guiX + SIDEBAR_WIDTH + 10);
			searchField.setY(guiY + 8);
			searchField.render(context, mouseX, mouseY, 0);
		}
	}

	private void renderSidebar(DrawContext context, int x, int y, int w, int h, int mouseX, int mouseY, float dt, float scale) {
		context.fill(x, y, x + w, y + h, new Color(15, 15, 15, 140).getRGB());
		context.fill(x + w - 1, y, x + w, y + h, SEPARATOR.getRGB());

		int headerH = (int) (HEADER_HEIGHT * scale);
		int logoSize = (int) (24 * scale);
		int logoX = x + (int) (12 * scale);
		int logoY = y + (headerH - logoSize) / 2;

		context.fill(logoX, logoY, logoX + logoSize, logoY + logoSize, new Color(80, 80, 80, 100).getRGB());

		String title = "SYSTEM CLIENT";
		int titleX = logoX + logoSize + (int) (8 * scale);
		int titleY = y + (headerH / 2) - (mc.textRenderer.fontHeight);
		var matrices = context.getMatrices();
		matrices.pushMatrix();
		float textScale = 1.4f;
		matrices.translate(titleX, titleY);
		matrices.scale(textScale, textScale);
		context.drawText(mc.textRenderer, title, 0, 0, TEXT_WHITE.getRGB(), false);
		matrices.popMatrix();

		context.fill(x + (int) (10 * scale), y + headerH - 1, x + w - (int) (10 * scale), y + headerH, SEPARATOR.getRGB());

		Category[] categories = Category.values();
		int catStartY = y + headerH + (int) (10 * scale);
		int catBtnH = (int) (CATEGORY_BUTTON_HEIGHT * scale);
		int catPad = (int) (CATEGORY_PADDING * scale);
		int catMarginX = (int) (8 * scale);

		for (int i = 0; i < categories.length; i++) {
			Category cat = categories[i];
			int btnX = x + catMarginX;
			int btnY = catStartY + i * (catBtnH + catPad);
			int btnW = w - catMarginX * 2;

			boolean hovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + catBtnH;
			boolean active = cat == activeCategory;

			float targetHover = hovered ? 1.0f : 0.0f;
			categoryHoverProgress[i] += (targetHover - categoryHoverProgress[i]) * Math.min(1.0f, dt * 10.0f);

			if (active) {
				GlassRenderer.renderCategoryGlow(context, btnX, btnY, btnW, catBtnH);
				GlassRenderer.renderRoundedRect(context, btnX, btnY, btnW, catBtnH,
						new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 30), (int) (6 * scale));
				context.fill(btnX, btnY, btnX + (int) (3 * scale), btnY + catBtnH,
						GlassRenderer.getGoldWithAlpha(200).getRGB());
			} else if (categoryHoverProgress[i] > 0.01f) {
				int hoverAlpha = (int) (20 * categoryHoverProgress[i]);
				GlassRenderer.renderRoundedRect(context, btnX, btnY, btnW, catBtnH,
						new Color(255, 255, 255, hoverAlpha), (int) (6 * scale));
			}

			String catName = getCategoryDisplayName(cat);
			int textY = btnY + (catBtnH - mc.textRenderer.fontHeight * 2) / 2;
			int textColor = active ? GOLD.getRGB() : (hovered ? TEXT_WHITE.getRGB() : TEXT_DIM.getRGB());
			TextRenderer.drawString(catName, context, btnX + (int) (14 * scale), textY, textColor);
		}
	}

	private void renderModulePanel(DrawContext context, int x, int y, int w, int h, int mouseX, int mouseY, float dt, float scale) {
		int headerH = (int) (HEADER_HEIGHT * scale);
		int contentY = y + headerH;
		int contentH = h - headerH;

		context.fill(x, y + headerH - 1, x + w, y + headerH, SEPARATOR.getRGB());

		String catTitle = getCategoryDisplayName(activeCategory);
		int titleX = x + (int) (15 * scale);
		int titleY = y + (headerH / 2) - mc.textRenderer.fontHeight;
		var matrices = context.getMatrices();
		matrices.pushMatrix();
		matrices.translate(titleX, titleY);
		matrices.scale(1.8f, 1.8f);
		context.drawText(mc.textRenderer, catTitle, 0, 0, TEXT_WHITE.getRGB(), false);
		matrices.popMatrix();

		String query = getSearchQuery();
		List<ModuleEntry> visibleModules = new ArrayList<>();
		for (ModuleEntry entry : moduleEntries) {
			if (query.isEmpty() || entry.module.getName().toString().toLowerCase().contains(query.toLowerCase())) {
				visibleModules.add(entry);
			}
		}

		int moduleBtnH = (int) (MODULE_BUTTON_HEIGHT * scale);
		int modulePad = (int) (MODULE_PADDING * scale);
		int moduleMarginX = (int) (10 * scale);

		int totalContentHeight = visibleModules.size() * (moduleBtnH + modulePad);
		maxScroll = Math.max(0, totalContentHeight - contentH + (int) (10 * scale));
		int pScrollOffset = (int) (scrollOffset * scale);

		context.enableScissor(
				(int) (x / scale), (int) (contentY / scale),
				(int) ((x + w) / scale), (int) ((contentY + contentH) / scale)
		);

		for (int i = 0; i < visibleModules.size(); i++) {
			ModuleEntry entry = visibleModules.get(i);
			int btnX = x + moduleMarginX;
			int btnY = contentY + (int) (8 * scale) + i * (moduleBtnH + modulePad) - pScrollOffset;
			int btnW = w - moduleMarginX * 2;

			if (btnY + moduleBtnH < contentY || btnY > contentY + contentH) continue;

			boolean hovered = mouseX >= btnX && mouseX <= btnX + btnW
					&& mouseY >= btnY && mouseY <= btnY + moduleBtnH
					&& mouseY >= contentY && mouseY <= contentY + contentH;

			int entryIndex = moduleEntries.indexOf(entry);
			if (entryIndex >= 0 && entryIndex < moduleHoverProgress.length) {
				float targetHover = hovered ? 1.0f : 0.0f;
				moduleHoverProgress[entryIndex] += (targetHover - moduleHoverProgress[entryIndex]) * Math.min(1.0f, dt * 10.0f);
			}

			float hoverProg = (entryIndex >= 0 && entryIndex < moduleHoverProgress.length) ? moduleHoverProgress[entryIndex] : 0;

			Color btnBg;
			if (entry.module.isEnabled()) {
				btnBg = new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 35);
			} else {
				btnBg = new Color(255, 255, 255, 8);
			}

			float renderScale = 1.0f + 0.02f * hoverProg;
			int scaledBtnW = (int) (btnW * renderScale);
			int scaledBtnH = (int) (moduleBtnH * renderScale);
			int offsetX = (btnW - scaledBtnW) / 2;
			int offsetY = (moduleBtnH - scaledBtnH) / 2;

			GlassRenderer.renderRoundedRect(context, btnX + offsetX, btnY + offsetY, scaledBtnW, scaledBtnH,
					btnBg, (int) (6 * scale));

			if (hoverProg > 0.01f) {
				GlassRenderer.renderHoverOverlay(context, btnX + offsetX, btnY + offsetY, scaledBtnW, scaledBtnH, hoverProg);
			}

			if (entry.module.isEnabled()) {
				context.fill(btnX + offsetX, btnY + offsetY, btnX + offsetX + (int) (3 * scale), btnY + offsetY + scaledBtnH,
						GlassRenderer.getGoldWithAlpha(180).getRGB());
			}

			String moduleName = entry.module.getName().toString();
			int textY = btnY + (moduleBtnH - mc.textRenderer.fontHeight * 2) / 2;
			int textColor = entry.module.isEnabled() ? GOLD.getRGB() : TEXT_WHITE.getRGB();
			TextRenderer.drawString(moduleName, context, btnX + (int) (14 * scale), textY, textColor);

			if (hovered && entry.module.getDescription() != null) {
				renderModuleTooltip(context, mouseX, mouseY, entry.module.getDescription().toString(), scale);
			}
		}

		context.disableScissor();
	}

	private void renderModuleTooltip(DrawContext context, int mouseX, int mouseY, String description, float scale) {
		int tooltipW = mc.textRenderer.getWidth(description) * 2 + (int) (16 * scale);
		int tooltipH = (int) (22 * scale);
		int tooltipX = mouseX + (int) (10 * scale);
		int tooltipY = mouseY - tooltipH - (int) (5 * scale);

		GlassRenderer.renderRoundedRect(context, tooltipX, tooltipY, tooltipW, tooltipH,
				new Color(20, 20, 20, 220), (int) (4 * scale));
		GlassRenderer.renderRoundedBorder(context, tooltipX, tooltipY, tooltipW, tooltipH,
				new Color(255, 255, 255, 30), (int) (4 * scale), 1.0);
		TextRenderer.drawString(description, context, tooltipX + (int) (8 * scale),
				tooltipY + (tooltipH - mc.textRenderer.fontHeight * 2) / 2, TEXT_DIM.getRGB());
	}

	private void renderCredits(DrawContext context, int guiX, int guiY, int guiW, int guiH, float scale) {
		if (mc.player == null) return;
		String playerName = mc.player.getName().getString();
		dlindustries.vigillant.system.module.modules.client.NameProtect nameProtect =
				system.INSTANCE.getModuleManager().getModule(
						dlindustries.vigillant.system.module.modules.client.NameProtect.class);
		if (nameProtect != null)
			playerName = nameProtect.replaceName(playerName);

		String credits = "SC | " + playerName;
		int creditsW = mc.textRenderer.getWidth(credits);
		int creditsX = guiX + guiW - creditsW * 2 - (int) (12 * scale);
		int creditsY = guiY + guiH - (int) (18 * scale);
		TextRenderer.drawString(credits, context, creditsX, creditsY, new Color(120, 120, 120, 150).getRGB());
	}

	private String getCategoryDisplayName(Category cat) {
		String name = cat.name.toString();
		if (name.equalsIgnoreCase("Sword") || name.equalsIgnoreCase("Crystal")
				|| name.equalsIgnoreCase("Potions") || name.equalsIgnoreCase("SpearMace")) {
			return "Combat";
		} else if (name.equalsIgnoreCase("Render") || name.equalsIgnoreCase("Esp and Visuals")) {
			return "Render";
		} else if (name.equalsIgnoreCase("Utilities") || name.equalsIgnoreCase("Client")) {
			return "Other";
		}
		return name;
	}

	@Override
	public boolean keyPressed(KeyInput keyInput) {
		if (searchField != null && searchField.isFocused()) {
			if (keyInput.key() == 256) {
				searchField.setText("");
				searchField.setFocused(false);
				setFocused(null);
				return true;
			}
			if (searchField.keyPressed(keyInput))
				return true;
		}
		for (Window window : windows)
			window.keyPressed(keyInput.key(), keyInput.scancode(), keyInput.modifiers());
		return super.keyPressed(keyInput);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		float scaleFactor = (float) mc.getWindow().getScaleFactor();
		int pX = (int) (click.x() * scaleFactor);
		int pY = (int) (click.y() * scaleFactor);
		int pGuiX = (int) (guiX * scaleFactor);
		int pGuiY = (int) (guiY * scaleFactor);
		int pGuiW = (int) (GUI_WIDTH * scaleFactor);
		int pGuiH = (int) (GUI_HEIGHT * scaleFactor);
		int pSidebarW = (int) (SIDEBAR_WIDTH * scaleFactor);
		int pHeaderH = (int) (HEADER_HEIGHT * scaleFactor);

		if (searchField != null) {
			boolean overField = click.x() >= searchField.getX()
					&& click.x() <= searchField.getX() + searchField.getWidth()
					&& click.y() >= searchField.getY()
					&& click.y() <= searchField.getY() + searchField.getHeight();
			if (overField) {
				setFocused(searchField);
				searchField.setFocused(true);
				searchField.mouseClicked(click, doubled);
				return true;
			} else {
				setFocused(null);
				searchField.setFocused(false);
			}
		}

		if (click.button() == 0 && pX >= pGuiX && pX <= pGuiX + pGuiW && pY >= pGuiY && pY <= pGuiY + pGuiH) {
			if (pX >= pGuiX && pX <= pGuiX + pSidebarW) {
				handleSidebarClick(pX, pY, pGuiX, pGuiY, pSidebarW, scaleFactor);
			} else {
				handleModulePanelClick(pX, pY, click.button(), pGuiX + pSidebarW, pGuiY + pHeaderH,
						pGuiW - pSidebarW, pGuiH - pHeaderH, scaleFactor);
			}
			return true;
		}

		return super.mouseClicked(click, doubled);
	}

	private void handleSidebarClick(int mouseX, int mouseY, int sidebarX, int guiY, int sidebarW, float scale) {
		int headerH = (int) (HEADER_HEIGHT * scale);
		int catBtnH = (int) (CATEGORY_BUTTON_HEIGHT * scale);
		int catPad = (int) (CATEGORY_PADDING * scale);
		int catMarginX = (int) (8 * scale);
		int catStartY = guiY + headerH + (int) (10 * scale);

		Category[] categories = Category.values();
		for (int i = 0; i < categories.length; i++) {
			int btnX = sidebarX + catMarginX;
			int btnY = catStartY + i * (catBtnH + catPad);
			int btnW = sidebarW - catMarginX * 2;

			if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + catBtnH) {
				activeCategory = categories[i];
				rebuildModuleList();
				break;
			}
		}
	}

	private void handleModulePanelClick(int mouseX, int mouseY, int button, int contentX, int contentY, int contentW, int contentH, float scale) {
		int moduleBtnH = (int) (MODULE_BUTTON_HEIGHT * scale);
		int modulePad = (int) (MODULE_PADDING * scale);
		int moduleMarginX = (int) (10 * scale);
		int pScrollOffset = (int) (scrollOffset * scale);

		String query = getSearchQuery();
		List<ModuleEntry> visibleModules = new ArrayList<>();
		for (ModuleEntry entry : moduleEntries) {
			if (query.isEmpty() || entry.module.getName().toString().toLowerCase().contains(query.toLowerCase())) {
				visibleModules.add(entry);
			}
		}

		for (int i = 0; i < visibleModules.size(); i++) {
			int btnX = contentX + moduleMarginX;
			int btnY = contentY + (int) (8 * scale) + i * (moduleBtnH + modulePad) - pScrollOffset;
			int btnW = contentW - moduleMarginX * 2;

			if (mouseX >= btnX && mouseX <= btnX + btnW
					&& mouseY >= btnY && mouseY <= btnY + moduleBtnH
					&& mouseY >= contentY && mouseY <= contentY + contentH) {
				if (button == 0) {
					visibleModules.get(i).module.toggle();
				}
				break;
			}
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		scrollOffset -= (int) (verticalAmount * SCROLL_SPEED);
		scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
		return true;
	}

	@Override
	public boolean mouseDragged(Click click, double deltaX, double deltaY) {
		return super.mouseDragged(click, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(Click click) {
		return super.mouseReleased(click);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void close() {
		ClickGUI clickGuiModule = system.INSTANCE.getModuleManager().getModule(ClickGUI.class);
		if (clickGuiModule != null && clickGuiModule.isEnabled()) {
			clickGuiModule.setEnabled(false);
			return;
		}
		onGuiClose();
	}

	public void onGuiClose() {
		mc.setScreenAndRender(system.INSTANCE.previousScreen);
		currentColor = null;
		if (searchField != null) {
			searchField.setText("");
			searchField.setFocused(false);
		}
		for (Window window : windows)
			window.onGuiClose();
	}

	private static class ModuleEntry {
		final Module module;

		ModuleEntry(Module module) {
			this.module = module;
		}
	}
}
