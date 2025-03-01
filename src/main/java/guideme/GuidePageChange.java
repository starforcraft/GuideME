package guideme;

import guideme.compiler.ParsedGuidePage;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record GuidePageChange(
        ResourceLocation pageId,
        @Nullable ParsedGuidePage oldPage,
        @Nullable ParsedGuidePage newPage) {
}
