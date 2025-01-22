package guideme.document.interaction;

import guideme.document.LytRect;
import guideme.document.block.LytBlock;
import guideme.layout.LayoutContext;
import guideme.render.RenderContext;
import guideme.ui.GuideUiHost;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Wraps an {@link AbstractWidget} for use within the guidebook layout tree.
 */
public class LytWidget extends LytBlock implements InteractiveElement {
    private final AbstractWidget widget;

    public LytWidget(AbstractWidget widget) {
        this.widget = widget;
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        return new LytRect(
                x, y,
                widget.getWidth(), widget.getHeight());
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        widget.setX(widget.getX() + deltaX);
        widget.setY(widget.getY() + deltaY);
    }

    @Override
    public void renderBatch(RenderContext context, MultiBufferSource buffers) {
    }

    @Override
    public void render(RenderContext context) {
        updateWidgetPosition();

        var minecraft = Minecraft.getInstance();

        if (!(minecraft.screen instanceof GuideUiHost uiHost)) {
            return; // Can't render if we can't translate
        }

        var mouseX = (minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth()
                / minecraft.getWindow().getScreenWidth());
        var mouseY = (minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight()
                / minecraft.getWindow().getScreenHeight());

        var mouseDocPos = uiHost.getDocumentPoint(mouseX, mouseY);

        widget.render(
                context.guiGraphics(),
                mouseDocPos != null ? mouseDocPos.x() : -100,
                mouseDocPos != null ? mouseDocPos.y() : -100,
                minecraft.getTimer().getRealtimeDeltaTicks());
    }

    private void updateWidgetPosition() {
        widget.setPosition(bounds.x(), bounds.y());
    }

    @Override
    public boolean mouseMoved(GuideUiHost screen, int x, int y) {
        widget.mouseMoved(x, y);
        return true;
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button) {
        return widget.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseReleased(GuideUiHost screen, int x, int y, int button) {
        return widget.mouseReleased(x, y, button);
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        return Optional.empty();
    }

    public AbstractWidget getWidget() {
        return widget;
    }
}
