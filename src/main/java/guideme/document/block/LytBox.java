package guideme.document.block;

import guideme.document.LytRect;
import guideme.layout.LayoutContext;
import guideme.render.RenderContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;

public abstract class LytBox extends LytBlock implements LytBlockContainer {
    protected final List<LytBlock> children = new ArrayList<>();

    protected int paddingLeft;
    protected int paddingTop;
    protected int paddingRight;
    protected int paddingBottom;

    @Override
    public void removeChild(LytNode node) {
        if (node instanceof LytBlock block && block.parent == this) {
            children.remove(block);
            block.parent = null;
        }
    }

    @Override
    public void append(LytBlock block) {
        if (block.parent != null) {
            block.parent.removeChild(block);
        }
        block.parent = this;
        children.add(block);
    }

    public void clearContent() {
        for (var child : children) {
            child.parent = null;
        }
        children.clear();
    }

    protected abstract LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth);

    @Override
    protected final LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        // Apply adding
        var innerLayout = computeBoxLayout(
                context,
                x + paddingLeft,
                y + paddingTop,
                availableWidth - paddingLeft - paddingRight);

        return innerLayout.expand(paddingLeft, paddingTop, paddingRight, paddingBottom);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        for (var child : children) {
            child.setLayoutPos(child.bounds.point().add(deltaX, deltaY));
        }
    }

    public final void setPadding(int padding) {
        paddingLeft = padding;
        paddingTop = padding;
        paddingRight = padding;
        paddingBottom = padding;
    }

    @Override
    public List<? extends LytNode> getChildren() {
        return children;
    }

    @Override
    public void renderBatch(RenderContext context, MultiBufferSource buffers) {
        for (var child : children) {
            child.renderBatch(context, buffers);
        }
    }

    @Override
    public void render(RenderContext context) {
        for (var child : children) {
            child.render(context);
        }
    }
}
