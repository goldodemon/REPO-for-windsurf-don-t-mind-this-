package dlindustries.vigillant.system.gui.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;

import java.awt.*;

public final class GlassRenderer {
	private static final Color GOLD = new Color(255, 215, 0);
	private static final int GLOW_RADIUS = 100;

	private static Matrix4f getMatrix(DrawContext context) {
		Matrix3x2fStack stack2d = context.getMatrices();
		Matrix3x2f m = new Matrix3x2f(stack2d);
		MatrixStack stack = new MatrixStack();
		Matrix4f matrix = stack.peek().getPositionMatrix();
		matrix.set(
				m.m00(), m.m10(), 0.0f, 0.0f,
				m.m01(), m.m11(), 0.0f, 0.0f,
				0.0f, 0.0f, 1.0f, 0.0f,
				m.m20(), m.m21(), 0.0f, 1.0f
		);
		return matrix;
	}

	public static void renderMouseGlow(DrawContext context, int mouseX, int mouseY, int panelX, int panelY, int panelW, int panelH) {
		int steps = 40;
		for (int i = steps; i > 0; i--) {
			float progress = (float) i / steps;
			int radius = (int) (GLOW_RADIUS * progress);
			int alpha = (int) (35 * (1.0f - progress) * (1.0f - progress));
			if (alpha < 1) continue;

			Color glowColor = new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), alpha);
			int gx = mouseX - radius;
			int gy = mouseY - radius;
			int gw = radius * 2;
			int gh = radius * 2;

			int cx = Math.max(gx, panelX);
			int cy = Math.max(gy, panelY);
			int cx2 = Math.min(gx + gw, panelX + panelW);
			int cy2 = Math.min(gy + gh, panelY + panelH);

			if (cx < cx2 && cy < cy2) {
				context.fill(cx, cy, cx2, cy2, glowColor.getRGB());
			}
		}
	}

	public static void renderCategoryGlow(DrawContext context, int x, int y, int w, int h) {
		for (int i = 0; i < 3; i++) {
			int alpha = 40 - i * 12;
			if (alpha < 1) continue;
			Color glow = new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), alpha);
			context.fill(x - i, y - i, x + w + i, y + h + i, glow.getRGB());
		}
	}

	public static void renderHoverOverlay(DrawContext context, int x, int y, int w, int h, float hoverProgress) {
		if (hoverProgress <= 0.001f) return;
		int alpha = (int) (38 * hoverProgress);
		Color overlay = new Color(255, 215, 0, alpha);
		context.fill(x, y, x + w, y + h, overlay.getRGB());
	}

	public static void renderRoundedRect(DrawContext context, int x, int y, int w, int h, Color color, double radius) {
		GlStateManager._enableBlend();
		Matrix4f matrix = getMatrix(context);
		float r = color.getRed() / 255f;
		float g = color.getGreen() / 255f;
		float b = color.getBlue() / 255f;
		float a = color.getAlpha() / 255f;

		BufferBuilder bufferBuilder = Tessellator.getInstance().begin(
				VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

		int samples = 20;
		double[][] corners = {
				{x + w - radius, y + radius, radius, 270},
				{x + w - radius, y + h - radius, radius, 0},
				{x + radius, y + h - radius, radius, 90},
				{x + radius, y + radius, radius, 180}
		};

		double centerX = x + w / 2.0;
		double centerY = y + h / 2.0;
		bufferBuilder.vertex(matrix, (float) centerX, (float) centerY, 0).color(r, g, b, a);

		for (double[] corner : corners) {
			double cx = corner[0], cy = corner[1], cr = corner[2], startAngle = corner[3];
			for (int j = 0; j <= samples; j++) {
				double angle = Math.toRadians(startAngle + (90.0 * j / samples));
				float px = (float) (cx + Math.cos(angle) * cr);
				float py = (float) (cy + Math.sin(angle) * cr);
				bufferBuilder.vertex(matrix, px, py, 0).color(r, g, b, a);
			}
		}

		double firstAngle = Math.toRadians(corners[0][3]);
		float firstPx = (float) (corners[0][0] + Math.cos(firstAngle) * corners[0][2]);
		float firstPy = (float) (corners[0][1] + Math.sin(firstAngle) * corners[0][2]);
		bufferBuilder.vertex(matrix, firstPx, firstPy, 0).color(r, g, b, a);

		BuiltBuffer built = bufferBuilder.end();
		RenderLayers.debugFilledBox().draw(built);
		GlStateManager._disableBlend();
	}

	public static void renderRoundedBorder(DrawContext context, int x, int y, int w, int h, Color color, double radius, double width) {
		GlStateManager._enableBlend();
		Matrix4f matrix = getMatrix(context);
		float r = color.getRed() / 255f;
		float g = color.getGreen() / 255f;
		float b = color.getBlue() / 255f;
		float a = color.getAlpha() / 255f;

		BufferBuilder bufferBuilder = Tessellator.getInstance().begin(
				VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

		int samples = 20;
		double[][] corners = {
				{x + w - radius, y + radius, radius, 270},
				{x + w - radius, y + h - radius, radius, 0},
				{x + radius, y + h - radius, radius, 90},
				{x + radius, y + radius, radius, 180}
		};

		for (double[] corner : corners) {
			double cx = corner[0], cy = corner[1], cr = corner[2], startAngle = corner[3];
			for (int j = 0; j <= samples; j++) {
				double angle = Math.toRadians(startAngle + (90.0 * j / samples));
				float cos = (float) Math.cos(angle);
				float sin = (float) Math.sin(angle);
				float outerX = (float) (cx + cos * cr);
				float outerY = (float) (cy + sin * cr);
				float innerX = (float) (cx + cos * (cr - width));
				float innerY = (float) (cy + sin * (cr - width));
				bufferBuilder.vertex(matrix, outerX, outerY, 0).color(r, g, b, a);
				bufferBuilder.vertex(matrix, innerX, innerY, 0).color(r, g, b, a);
			}
		}

		double firstAngle = Math.toRadians(corners[0][3]);
		float cos = (float) Math.cos(firstAngle);
		float sin = (float) Math.sin(firstAngle);
		bufferBuilder.vertex(matrix, (float) (corners[0][0] + cos * corners[0][2]), (float) (corners[0][1] + sin * corners[0][2]), 0).color(r, g, b, a);
		bufferBuilder.vertex(matrix, (float) (corners[0][0] + cos * (corners[0][2] - width)), (float) (corners[0][1] + sin * (corners[0][2] - width)), 0).color(r, g, b, a);

		BuiltBuffer built = bufferBuilder.end();
		RenderLayers.debugFilledBox().draw(built);
		GlStateManager._disableBlend();
	}

	public static Color withAlpha(Color color, int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
	}

	public static Color getGold() {
		return GOLD;
	}

	public static Color getGoldWithAlpha(int alpha) {
		return withAlpha(GOLD, alpha);
	}
}
