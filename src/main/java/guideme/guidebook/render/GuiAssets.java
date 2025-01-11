package guideme.guidebook.render;

import guideme.GuideME;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.resources.ResourceLocation;

/**
 * Asset management
 */
public final class GuiAssets {
    /**
     * @see net.minecraft.client.gui.GuiSpriteManager
     */
    public static final ResourceLocation GUI_SPRITE_ATLAS = ResourceLocation
            .withDefaultNamespace("textures/atlas/gui.png");

    public static final ResourceLocation WINDOW_SPRITE = GuideME.makeId("window");
    public static final ResourceLocation INNER_BORDER_SPRITE = GuideME.makeId("window_inner");
    public static final ResourceLocation SLOT_BACKGROUND = GuideME.makeId("slot");
    public static final ResourceLocation SLOT_LARGE_BACKGROUND = GuideME.makeId("slot_large");
    public static final ResourceLocation SLOT_BORDER = GuideME.makeId("slot_border");
    public static final ResourceLocation SLOT_LIGHT = GuideME.makeId("slot_light");
    public static final ResourceLocation SLOT_DARK = GuideME.makeId("slot_dark");
    public static final ResourceLocation LARGE_SLOT_LIGHT = GuideME.makeId("large_slot_light");
    public static final ResourceLocation LARGE_SLOT_DARK = GuideME.makeId("large_slot_dark");
    public static final ResourceLocation ARROW_LIGHT = GuideME.makeId("recipe_arrow_light");
    public static final ResourceLocation ARROW_DARK = GuideME.makeId("recipe_arrow_dark");

    private GuiAssets() {
    }

    public static SpritePadding getWindowPadding() {
        return getNineSliceSprite(WINDOW_SPRITE).padding;
    }

    public static NineSliceSprite getNineSliceSprite(ResourceLocation id) {
        var guiSprites = Minecraft.getInstance().getGuiSprites();
        var sprite = guiSprites.getSprite(id);
        if (!(guiSprites.getSpriteScaling(sprite) instanceof GuiSpriteScaling.NineSlice nineSlice)) {
            throw new IllegalStateException("Expected sprite " + id + " to be a nine-slice sprite!");
        }

        var border = nineSlice.border();
        // Compute the delimiting U values *in the atlas* for the three slices.
        var u0 = sprite.getU0();
        var u1 = sprite.getU(border.left() / (float) nineSlice.width());
        var u2 = sprite.getU(1 - border.right() / (float) nineSlice.width());
        var u3 = sprite.getU1();
        // Compute the delimiting V values *in the atlas* for the three slices.
        var v0 = sprite.getV0();
        var v1 = sprite.getV(border.top() / (float) nineSlice.height());
        var v2 = sprite.getV(1 - border.bottom() / (float) nineSlice.height());
        var v3 = sprite.getV1();

        return new NineSliceSprite(
                sprite.atlasLocation(),
                new SpritePadding(border.left(), border.top(), border.right(), border.bottom()),
                new float[] { u0, u1, u2, u3, v0, v1, v2, v3 });
    }

    /**
     * @param uv First 4 U values delimiting the horizontal slices, then 4 V values delimiting the vertical slices.
     *           These values refer to the atlas.
     */
    public record NineSliceSprite(ResourceLocation atlasLocation,
            SpritePadding padding,
            float[] uv) {
    }
}
