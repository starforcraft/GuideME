package guideme.guidebook.document.interaction;

import guideme.guidebook.document.LytRect;
import guideme.guidebook.document.block.LytBlock;
import guideme.guidebook.layout.LayoutContext;
import guideme.guidebook.layout.MinecraftFontMetrics;
import guideme.guidebook.render.SimpleRenderContext;
import guideme.siteexport.ExportableResourceProvider;
import guideme.siteexport.ResourceExporter;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * A {@link GuideTooltip} that renders a {@link LytBlock} as the tooltip content.
 */
public class ContentTooltip implements GuideTooltip {
    private final List<ClientTooltipComponent> components;

    // The window size for which we performed layout
    @Nullable
    private LytRect layoutViewport;
    @Nullable
    private LytRect layoutBox;

    private final LytBlock content;

    public ContentTooltip(LytBlock content) {
        this.content = content;

        this.components = List.of(
                new ClientTooltipComponent() {
                    @Override
                    public int getHeight() {
                        return getLayoutBox().height();
                    }

                    @Override
                    public int getWidth(Font font) {
                        return getLayoutBox().width();
                    }

                    @Override
                    public void renderText(Font font, int x, int y, Matrix4f matrix,
                            MultiBufferSource.BufferSource bufferSource) {
                        getLayoutBox(); // Updates layout

                        var guiGraphics = new GuiGraphics(Minecraft.getInstance(), bufferSource);
                        var poseStack = guiGraphics.pose();
                        poseStack.mulPose(matrix);
                        poseStack.translate(x, y, 0);

                        var ctx = new SimpleRenderContext(layoutViewport, guiGraphics);
                        content.renderBatch(ctx, bufferSource);
                    }

                    @Override
                    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
                        getLayoutBox(); // Updates layout

                        var pose = guiGraphics.pose();
                        pose.pushPose();
                        pose.translate(x, y, 0);
                        var ctx = new SimpleRenderContext(layoutViewport, guiGraphics);
                        content.render(ctx);
                        pose.popPose();
                    }
                });
    }

    @Override
    public List<ClientTooltipComponent> getLines() {
        return components;
    }

    public LytBlock getContent() {
        return content;
    }

    private LytRect getLayoutBox() {
        var window = Minecraft.getInstance().getWindow();
        var currentViewport = new LytRect(0, 0, window.getGuiScaledWidth(), window.getGuiScaledHeight());
        if (layoutBox == null || !currentViewport.equals(layoutViewport)) {
            layoutViewport = currentViewport;
            var layoutContext = new LayoutContext(new MinecraftFontMetrics());
            layoutBox = content.layout(layoutContext, 0, 0, window.getGuiScaledWidth() / 2);
        }
        return layoutBox;
    }

    @Override
    public void exportResources(ResourceExporter exporter) {
        ExportableResourceProvider.visit(content, exporter);
    }
}
