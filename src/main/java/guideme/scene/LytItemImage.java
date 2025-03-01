package guideme.scene;

import guideme.document.LytRect;
import guideme.document.block.LytBlock;
import guideme.layout.LayoutContext;
import guideme.render.RenderContext;
import guideme.siteexport.ExportableResourceProvider;
import guideme.siteexport.ResourceExporter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class LytItemImage extends LytBlock implements ExportableResourceProvider {
    private ItemStack item = ItemStack.EMPTY;

    private float scale = 1;

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        return new LytRect(x, y, Mth.ceil(16 * scale), Mth.ceil(16 * scale));
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
    }

    @Override
    public void renderBatch(RenderContext context, MultiBufferSource buffers) {
    }

    @Override
    public void render(RenderContext context) {
        context.renderItem(item, bounds.x(), bounds.y(), 16 * scale, 16 * scale);
    }

    @Override
    public void exportResources(ResourceExporter exporter) {
        exporter.referenceItem(item);
    }
}
