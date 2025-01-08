package guideme.guidebook.layout.flow;

import guideme.guidebook.document.LytRect;
import guideme.guidebook.document.flow.LytFlowContent;
import guideme.guidebook.render.RenderContext;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;

public abstract class LineElement {
    /**
     * Next Element in flow direction.
     */
    @Nullable
    LineElement next;

    LytRect bounds = LytRect.empty();

    /**
     * The original flow content this line element is associated with.
     */
    @Nullable
    LytFlowContent flowContent;

    boolean containsMouse;

    boolean floating;

    @Nullable
    public LytFlowContent getFlowContent() {
        return flowContent;
    }

    /**
     * Render text content as part of batch rendering.
     */
    public void renderBatch(RenderContext context, MultiBufferSource buffers) {
    }

    /**
     * Render any other content individually.
     */
    public void render(RenderContext context) {
    }
}
