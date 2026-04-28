package dlindustries.vigillant.system.gui;

import dlindustries.vigillant.system.system;
import dlindustries.vigillant.system.gui.render.GlassRenderer;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.modules.client.ClickGUI;
import dlindustries.vigillant.system.module.setting.*;
import dlindustries.vigillant.system.utils.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
	private static final int SETTING_ROW_HEIGHT = 22;

	private static final Color GOLD = new Color(255, 215, 0);
	private static final Color TEXT_WHITE = new Color(230, 230, 230);
	private static final Color TEXT_DIM = new Color(160, 160, 160);
	private static final Color SEPARATOR = new Color(255, 255, 255, 20);

	private static final String[] DISPLAY_GROUPS = {"Sword", "Crystal", "Mace", "Render", "Other"};

	private final Map<String, List<Category>> categoryGroups = new LinkedHashMap<>();
	private String activeGroup;
	private final List<ModuleEntry> moduleEntries = new ArrayList<>();
	private int guiX, guiY;
	private int scrollOffset;
	private int maxScroll;
	private float[] categoryHoverProgress;
	private float[] moduleHoverProgress;
	private long lastRenderTime;
	private TextFieldWidget searchField;
	private Module expandedModule;

	public Color currentColor;
	public List<Window> windows = new ArrayList<>();

	public ClickGui() {
		super(Text.empty());
		buildCategoryGroups();
		activeGroup = DISPLAY_GROUPS[0];
		categoryHoverProgress = new float[DISPLAY_GROUPS.length];
		rebuildModuleList();

		for (Category category : Category.values()) {
			windows.add(new Window(0, 0, 230, 30, category, this));
		}
	}

	private void buildCategoryGroups() {
		for (String group : DISPLAY_GROUPS) {
			categoryGroups.put(group, new ArrayList<>());
		}
		for (Category cat : Category.values()) {
			String group = mapCategoryToGroup(cat);
			categoryGroups.get(group).add(cat);
		}
	}

	private String mapCategoryToGroup(Category cat) {
		String name = cat.name.toString();
		if (name.equalsIgnoreCase("Sword") || name.equalsIgnoreCase("Potions")) {
			return "Sword";
		} else if (name.equalsIgnoreCase("Crystal")) {
			return "Crystal";
		} else if (name.equalsIgnoreCase("SpearMace")) {
			return "Mace";
		} else if (name.equalsIgnoreCase("Render") || name.equalsIgnoreCase("Esp and Visuals")) {
			return "Render";
		}
		return "Other";
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
		return false;
	}

	private void rebuildModuleList() {
		moduleEntries.clear();
		List<Category> cats = categoryGroups.get(activeGroup);
		if (cats != null) {
			for (Category cat : cats) {
				List<Module> modules = system.INSTANCE.getModuleManager().getModulesInCategory(cat);
				for (Module module : modules) {
					moduleEntries.add(new ModuleEntry(module));
				}
			}
		}
		moduleHoverProgress = new float[moduleEntries.size()];
		scrollOffset = 0;
		expandedModule = null;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		if (mc.currentScreen != this) return;

		long now = System.currentTimeMillis();
		float dt = Math.min((now - lastRenderTime) / 1000.0f, 0.05f);
		lastRenderTime = now;

		if (ClickGUI.blur.getValue()) {
			mc.gameRenderer.renderBlur();
		}

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
		int pCorner = (int) (CORNER_RADIUS * scaleFactor);

		context.fill(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight(), new Color(0, 0, 0, 100).getRGB());

		GlassRenderer.renderRoundedRect(context, pGuiX, pGuiY, pGuiW, pGuiH,
				new Color(10, 10, 10, 102), pCorner);

		GlassRenderer.renderRoundedBorder(context, pGuiX, pGuiY, pGuiW, pGuiH,
				new Color(255, 255, 255, 25), pCorner, 1.5);

		Color innerHighlight = new Color(255, 255, 255, 6);
		context.fill(pGuiX + 2, pGuiY + 2, pGuiX + pGuiW - 2, pGuiY + 3, innerHighlight.getRGB());

		renderSidebar(context, pGuiX, pGuiY, pSidebarW, pGuiH, pMouseX, pMouseY, dt, scaleFactor);

		renderModulePanel(context, pGuiX + pSidebarW, pGuiY, pGuiW - pSidebarW, pGuiH, pMouseX, pMouseY, dt, scaleFactor);

		renderCredits(context, pGuiX, pGuiY, pGuiW, pGuiH, scaleFactor);

		context.getMatrices().popMatrix();

		renderSearchBar(context, mouseX, mouseY, scaleFactor);
	}

	private void renderSearchBar(DrawContext context, int mouseX, int mouseY, float scale) {
		if (searchField == null) return;

		int sbX = guiX + SIDEBAR_WIDTH + 5;
		int sbY = guiY + 4;
		int sbW = GUI_WIDTH - SIDEBAR_WIDTH - 10;
		int sbH = 20;

		GlassRenderer.renderRoundedRect(context, (int)(sbX * scale), (int)(sbY * scale),
				(int)(sbW * scale), (int)(sbH * scale),
				new Color(255, 255, 255, 10), (int)(6 * scale));
		GlassRenderer.renderRoundedBorder(context, (int)(sbX * scale), (int)(sbY * scale),
				(int)(sbW * scale), (int)(sbH * scale),
				new Color(255, 255, 255, 15), (int)(6 * scale), 0.5);

		searchField.setX(sbX + 5);
		searchField.setY(sbY + 3);
		searchField.setWidth(sbW - 10);
		searchField.render(context, mouseX, mouseY, 0);
	}

	private void renderSidebar(DrawContext context, int x, int y, int w, int h, int mouseX, int mouseY, float dt, float scale) {
		context.fill(x, y, x + w, y + h, new Color(15, 15, 15, 140).getRGB());
		context.fill(x + w - 1, y, x + w, y + h, SEPARATOR.getRGB());

		int headerH = (int) (HEADER_HEIGHT * scale);

		String logoText = "SC";
		int logoTextW = mc.textRenderer.getWidth(logoText);
		float logoScale = 2.2f;
		int logoTotalW = (int) (logoTextW * logoScale);
		int logoTotalH = (int) (mc.textRenderer.fontHeight * logoScale);
		int logoX = x + (int) (10 * scale);
		int logoY = y + (headerH - logoTotalH) / 2;

		GlassRenderer.renderRoundedRect(context, logoX - (int)(2 * scale), logoY - (int)(2 * scale),
				logoTotalW + (int)(4 * scale), logoTotalH + (int)(4 * scale),
				new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 20), (int)(4 * scale));

		var matrices = context.getMatrices();
		matrices.pushMatrix();
		matrices.translate(logoX, logoY);
		matrices.scale(logoScale, logoScale);
		context.drawText(mc.textRenderer, logoText, 0, 0, GOLD.getRGB(), false);
		matrices.popMatrix();

		String title = "SYSTEM CLIENT";
		int titleTextW = mc.textRenderer.getWidth(title);
		int availableW = w - (int) (10 * scale) - logoTotalW - (int) (12 * scale) - (int) (6 * scale);
		float textScale = Math.min(1.0f, (float) availableW / titleTextW);
		int titleX = logoX + logoTotalW + (int) (8 * scale);
		int titleY = y + (headerH / 2) - (int) (mc.textRenderer.fontHeight * textScale / 2);
		matrices.pushMatrix();
		matrices.translate(titleX, titleY);
		matrices.scale(textScale, textScale);
		context.drawText(mc.textRenderer, title, 0, 0, TEXT_WHITE.getRGB(), false);
		matrices.popMatrix();

		context.fill(x + (int) (10 * scale), y + headerH - 1, x + w - (int) (10 * scale), y + headerH, SEPARATOR.getRGB());

		int catStartY = y + headerH + (int) (10 * scale);
		int catBtnH = (int) (CATEGORY_BUTTON_HEIGHT * scale);
		int catPad = (int) (CATEGORY_PADDING * scale);
		int catMarginX = (int) (8 * scale);

		for (int i = 0; i < DISPLAY_GROUPS.length; i++) {
			String group = DISPLAY_GROUPS[i];
			int btnX = x + catMarginX;
			int btnY = catStartY + i * (catBtnH + catPad);
			int btnW = w - catMarginX * 2;

			boolean hovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + catBtnH;
			boolean active = group.equals(activeGroup);

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

			int textY = btnY + (catBtnH - mc.textRenderer.fontHeight * 2) / 2;
			int textColor = active ? GOLD.getRGB() : (hovered ? TEXT_WHITE.getRGB() : TEXT_DIM.getRGB());
			TextRenderer.drawString(group, context, btnX + (int) (14 * scale), textY, textColor);
		}
	}

	private void renderModulePanel(DrawContext context, int x, int y, int w, int h, int mouseX, int mouseY, float dt, float scale) {
		int headerH = (int) (HEADER_HEIGHT * scale);
		int contentY = y + headerH;
		int contentH = h - headerH;

		context.fill(x, y + headerH - 1, x + w, y + headerH, SEPARATOR.getRGB());

		int titleX = x + (int) (15 * scale);
		int titleY = y + (headerH / 2) - (int) (mc.textRenderer.fontHeight * 0.9f);
		var matrices = context.getMatrices();
		matrices.pushMatrix();
		matrices.translate(titleX, titleY);
		matrices.scale(1.8f, 1.8f);
		context.drawText(mc.textRenderer, activeGroup, 0, 0, TEXT_WHITE.getRGB(), false);
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
		int settingRowH = (int) (SETTING_ROW_HEIGHT * scale);

		int totalContentHeight = 0;
		for (ModuleEntry entry : visibleModules) {
			totalContentHeight += moduleBtnH + modulePad;
			if (entry.module == expandedModule) {
				List<Setting<?>> settings = entry.module.getSettings();
				int visibleSettings = 0;
				for (Setting<?> s : settings) {
					if (!(s instanceof KeybindSetting && ((KeybindSetting) s).isModuleKey())) {
						visibleSettings++;
					}
				}
				totalContentHeight += visibleSettings * settingRowH + (int) (4 * scale);
			}
		}
		maxScroll = Math.max(0, totalContentHeight - contentH + (int) (10 * scale));
		int pScrollOffset = (int) (scrollOffset * scale);

		context.enableScissor(
				(int) (x / scale), (int) (contentY / scale),
				(int) ((x + w) / scale), (int) ((contentY + contentH) / scale)
		);

		int currentY = contentY + (int) (8 * scale) - pScrollOffset;

		for (int i = 0; i < visibleModules.size(); i++) {
			ModuleEntry entry = visibleModules.get(i);
			int btnX = x + moduleMarginX;
			int btnY = currentY;
			int btnW = w - moduleMarginX * 2;

			boolean visible = btnY + moduleBtnH >= contentY && btnY <= contentY + contentH;

			if (visible) {
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

				String keybindText = getKeybindText(entry.module);
				if (keybindText != null) {
					int kbW = mc.textRenderer.getWidth(keybindText) * 2;
					int kbX = btnX + btnW - kbW - (int) (10 * scale);
					TextRenderer.drawString(keybindText, context, kbX, textY, TEXT_DIM.getRGB());
				}

				if (entry.module == expandedModule) {
					context.fill(btnX + offsetX + scaledBtnW - (int)(3 * scale), btnY + offsetY,
							btnX + offsetX + scaledBtnW, btnY + offsetY + scaledBtnH,
							GlassRenderer.getGoldWithAlpha(100).getRGB());
				}

				if (hovered && entry.module.getDescription() != null) {
					renderModuleTooltip(context, mouseX, mouseY, entry.module.getDescription().toString(), scale);
				}
			}

			currentY += moduleBtnH + modulePad;

			if (entry.module == expandedModule) {
				currentY = renderSettingsPanel(context, entry.module, btnX, currentY, btnW, scale, mouseX, mouseY, contentY, contentH);
			}
		}

		context.disableScissor();
	}

	private int renderSettingsPanel(DrawContext context, Module module, int x, int startY, int w, float scale, int mouseX, int mouseY, int clipY, int clipH) {
		List<Setting<?>> settings = module.getSettings();
		int settingRowH = (int) (SETTING_ROW_HEIGHT * scale);
		int y = startY;
		int indent = (int) (10 * scale);

		for (Setting<?> setting : settings) {
			if (setting instanceof KeybindSetting kb && kb.isModuleKey()) continue;

			boolean visible = y + settingRowH >= clipY && y <= clipY + clipH;

			if (visible) {
				context.fill(x + indent, y, x + w - indent, y + settingRowH,
						new Color(255, 255, 255, 5).getRGB());
				context.fill(x + indent, y + settingRowH - 1, x + w - indent, y + settingRowH,
						new Color(255, 255, 255, 8).getRGB());

				String settingName = setting.getName().toString();
				int textY = y + (settingRowH - mc.textRenderer.fontHeight * 2) / 2;

				if (setting instanceof BooleanSetting bs) {
					TextRenderer.drawString(settingName, context, x + indent + (int) (8 * scale), textY, TEXT_DIM.getRGB());
					String val = bs.getValue() ? "ON" : "OFF";
					int valColor = bs.getValue() ? GOLD.getRGB() : TEXT_DIM.getRGB();
					int valW = mc.textRenderer.getWidth(val) * 2;
					TextRenderer.drawString(val, context, x + w - indent - valW - (int) (8 * scale), textY, valColor);
				} else if (setting instanceof NumberSetting ns) {
					TextRenderer.drawString(settingName, context, x + indent + (int) (8 * scale), textY, TEXT_DIM.getRGB());
					String val = String.format("%.1f", ns.getValue());
					int valW = mc.textRenderer.getWidth(val) * 2;
					TextRenderer.drawString(val, context, x + w - indent - valW - (int) (8 * scale), textY, TEXT_WHITE.getRGB());

					int barX = x + indent + (int) (8 * scale);
					int barY = y + settingRowH - (int) (4 * scale);
					int barW = w - indent * 2 - (int) (16 * scale);
					int barH = (int) (2 * scale);
					context.fill(barX, barY, barX + barW, barY + barH, new Color(255, 255, 255, 20).getRGB());
					double progress = (ns.getValue() - ns.getMin()) / (ns.getMax() - ns.getMin());
					int filledW = (int) (barW * progress);
					context.fill(barX, barY, barX + filledW, barY + barH, GlassRenderer.getGoldWithAlpha(180).getRGB());
				} else if (setting instanceof ModeSetting<?> ms) {
					TextRenderer.drawString(settingName, context, x + indent + (int) (8 * scale), textY, TEXT_DIM.getRGB());
					String val = ms.getMode().toString();
					int valW = mc.textRenderer.getWidth(val) * 2;
					TextRenderer.drawString(val, context, x + w - indent - valW - (int) (8 * scale), textY, GOLD.getRGB());
				} else {
					TextRenderer.drawString(settingName, context, x + indent + (int) (8 * scale), textY, TEXT_DIM.getRGB());
				}
			}

			y += settingRowH;
		}

		y += (int) (4 * scale);
		return y;
	}

	private String getKeybindText(Module module) {
		int key = module.getKey();
		if (key <= 0) return null;
		String name = GLFW.glfwGetKeyName(key, 0);
		if (name != null) return "[" + name.toUpperCase() + "]";
		return "[" + key + "]";
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

		if (pX >= pGuiX && pX <= pGuiX + pGuiW && pY >= pGuiY && pY <= pGuiY + pGuiH) {
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

		for (int i = 0; i < DISPLAY_GROUPS.length; i++) {
			int btnX = sidebarX + catMarginX;
			int btnY = catStartY + i * (catBtnH + catPad);
			int btnW = sidebarW - catMarginX * 2;

			if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + catBtnH) {
				activeGroup = DISPLAY_GROUPS[i];
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
		int settingRowH = (int) (SETTING_ROW_HEIGHT * scale);

		String query = getSearchQuery();
		List<ModuleEntry> visibleModules = new ArrayList<>();
		for (ModuleEntry entry : moduleEntries) {
			if (query.isEmpty() || entry.module.getName().toString().toLowerCase().contains(query.toLowerCase())) {
				visibleModules.add(entry);
			}
		}

		int currentY = contentY + (int) (8 * scale) - pScrollOffset;

		for (int i = 0; i < visibleModules.size(); i++) {
			ModuleEntry entry = visibleModules.get(i);
			int btnX = contentX + moduleMarginX;
			int btnY = currentY;
			int btnW = contentW - moduleMarginX * 2;

			if (mouseX >= btnX && mouseX <= btnX + btnW
					&& mouseY >= btnY && mouseY <= btnY + moduleBtnH
					&& mouseY >= contentY && mouseY <= contentY + contentH) {
				if (button == 0) {
					entry.module.toggle();
				} else if (button == 1) {
					expandedModule = (expandedModule == entry.module) ? null : entry.module;
				}
				return;
			}

			currentY += moduleBtnH + modulePad;

			if (entry.module == expandedModule) {
				List<Setting<?>> settings = entry.module.getSettings();
				int indent = (int) (10 * scale);
				for (Setting<?> setting : settings) {
					if (setting instanceof KeybindSetting kb && kb.isModuleKey()) continue;

					if (mouseX >= btnX + indent && mouseX <= btnX + btnW - indent
							&& mouseY >= currentY && mouseY <= currentY + settingRowH
							&& mouseY >= contentY && mouseY <= contentY + contentH) {
						handleSettingClick(setting, button);
						return;
					}
					currentY += settingRowH;
				}
				currentY += (int) (4 * scale);
			}
		}
	}

	private void handleSettingClick(Setting<?> setting, int button) {
		if (setting instanceof BooleanSetting bs) {
			bs.toggle();
		} else if (setting instanceof NumberSetting ns) {
			double increment = ns.getIncrement();
			if (button == 0) {
				ns.setValue(ns.getValue() + increment);
			} else if (button == 1) {
				ns.setValue(ns.getValue() - increment);
			}
		} else if (setting instanceof ModeSetting<?> ms) {
			ms.cycle();
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
		expandedModule = null;
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
