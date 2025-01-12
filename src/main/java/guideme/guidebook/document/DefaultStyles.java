package guideme.guidebook.document;

import guideme.api.color.ConstantColor;
import guideme.api.color.SymbolicColor;
import guideme.api.style.ResolvedTextStyle;
import guideme.api.style.TextAlignment;
import guideme.api.style.TextStyle;
import guideme.api.style.WhiteSpaceMode;
import net.minecraft.client.Minecraft;

public final class DefaultStyles {
    private DefaultStyles() {
    }

    /**
     * The base style everything else is based on.
     */
    public static final ResolvedTextStyle BASE_STYLE = new ResolvedTextStyle(
            1,
            false,
            false,
            false,
            false,
            false,
            Minecraft.UNIFORM_FONT,
            SymbolicColor.BODY_TEXT,
            WhiteSpaceMode.NORMAL,
            TextAlignment.LEFT,
            false);

    public static final TextStyle BODY_TEXT = TextStyle.builder()
            .font(Minecraft.UNIFORM_FONT)
            .color(SymbolicColor.BODY_TEXT)
            .build();

    public static final TextStyle CRAFTING_RECIPE_TYPE = TextStyle.builder()
            .font(Minecraft.UNIFORM_FONT)
            .color(SymbolicColor.CRAFTING_RECIPE_TYPE)
            .build();

    public static final TextStyle HEADING1 = TextStyle.builder()
            .fontScale(1.3f)
            .bold(true).font(Minecraft.DEFAULT_FONT)
            .color(ConstantColor.WHITE)
            .build();
    public static final TextStyle HEADING2 = TextStyle.builder()
            .fontScale(1.1f)
            .font(Minecraft.DEFAULT_FONT)
            .build();
    public static final TextStyle HEADING3 = TextStyle.builder()
            .fontScale(1f)
            .font(Minecraft.DEFAULT_FONT)
            .build();
    public static final TextStyle HEADING4 = TextStyle.builder()
            .fontScale(1.1f)
            .bold(true)
            .font(Minecraft.UNIFORM_FONT)
            .build();
    public static final TextStyle HEADING5 = TextStyle.builder()
            .fontScale(1f)
            .bold(true)
            .font(Minecraft.UNIFORM_FONT)
            .build();
    public static final TextStyle HEADING6 = TextStyle.builder()
            .fontScale(1f)
            .font(Minecraft.UNIFORM_FONT)
            .build();
}
