package guideme.guidebook.document.block;

import guideme.guidebook.color.SymbolicColor;
import guideme.guidebook.document.LytRect;
import guideme.guidebook.layout.LayoutContext;
import guideme.guidebook.render.RenderContext;
import net.minecraft.client.renderer.MultiBufferSource;

public class LytThematicBreak extends LytBlock {
    @Override
    public LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        return new LytRect(x, y, availableWidth, 6);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
    }

    @Override
    public void renderBatch(RenderContext context, MultiBufferSource buffers) {
    }

    @Override
    public void render(RenderContext context) {
        var line = bounds.withHeight(2).centerVerticallyIn(bounds);

        context.fillRect(line, SymbolicColor.THEMATIC_BREAK);
    }
}
