package guideme.guidebook.document.block.recipes;

import guideme.guidebook.document.DefaultStyles;
import guideme.guidebook.document.LytRect;
import guideme.guidebook.document.block.LytSlot;
import guideme.guidebook.document.block.LytSlotGrid;
import guideme.guidebook.layout.LayoutContext;
import guideme.guidebook.render.RenderContext;
import guideme.GuideME;
import guideme.siteexport.ExportableResourceProvider;
import guideme.util.Platform;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LytSmithingRecipe extends LytRecipeBox implements ExportableResourceProvider {
    private static final Logger LOG = LoggerFactory.getLogger(LytSmithingRecipe.class);

    private static final ResourceLocation ARROW_LIGHT = GuideME.makeId("ae2guide/gui/recipe_arrow_light.png");
    private static final ResourceLocation ARROW_DARK = GuideME.makeId("ae2guide/gui/recipe_arrow_dark.png");

    private final SmithingRecipe recipe;

    private final LytSlotGrid inputGrid;

    private final LytSlot resultSlot;

    public LytSmithingRecipe(RecipeHolder<SmithingRecipe> holder) {
        super(holder);
        this.recipe = holder.value();
        setPadding(5);
        paddingTop = 15;

        append(inputGrid = LytSlotGrid.row(getIngredients(recipe), true));
        append(resultSlot = new LytSlot(recipe.getResultItem(Platform.getClientRegistryAccess())));
    }

    private static List<Ingredient> getIngredients(SmithingRecipe recipe) {

        if (recipe instanceof SmithingTrimRecipe trimRecipe) {
            return List.of(
                    trimRecipe.template,
                    trimRecipe.base,
                    trimRecipe.addition);
        } else if (recipe instanceof SmithingTransformRecipe transformRecipe) {
            return List.of(
                    transformRecipe.template,
                    transformRecipe.base,
                    transformRecipe.addition);
        } else {
            LOG.warn("Cannot determine ingredients of smithing recipe type {}", recipe.getClass());
            return List.of();
        }

    }

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        var inputBounds = inputGrid.layout(
                context,
                x,
                y,
                availableWidth);

        var resultBounds = resultSlot.layout(
                context,
                inputBounds.right() + 28,
                y,
                availableWidth);
        return LytRect.union(inputBounds, resultBounds);
    }

    @Override
    public void render(RenderContext context) {
        context.renderPanel(getBounds());

        context.renderItem(
                Blocks.SMITHING_TABLE.asItem().getDefaultInstance(),
                bounds.x() + paddingLeft,
                bounds.y() + 4,
                8,
                8);
        context.renderText(
                Items.SMITHING_TABLE.getDescription().getString(),
                DefaultStyles.CRAFTING_RECIPE_TYPE.mergeWith(DefaultStyles.BASE_STYLE),
                bounds.x() + paddingLeft + 10,
                bounds.y() + 4);

        context.fillTexturedRect(
                new LytRect(bounds.right() - 25 - 24, bounds.y() + 10 + (bounds.height() - 27) / 2, 24, 17),
                context.isDarkMode() ? ARROW_DARK : ARROW_LIGHT);

        super.render(context);
    }

}
