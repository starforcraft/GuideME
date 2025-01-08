package guideme.guidebook.scene;

import guideme.guidebook.extensions.Extension;
import guideme.guidebook.extensions.ExtensionPoint;
import guideme.guidebook.scene.annotation.SceneAnnotation;
import guideme.guidebook.scene.level.GuidebookLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Provides a way to generate a {@link SceneAnnotation} on the fly if no explicit annotation could be found under the
 * mouse.
 */
public interface ImplicitAnnotationStrategy extends Extension {
    ExtensionPoint<ImplicitAnnotationStrategy> EXTENSION_POINT = new ExtensionPoint<>(ImplicitAnnotationStrategy.class);

    @Nullable
    SceneAnnotation getAnnotation(GuidebookLevel level, BlockState blockState, BlockHitResult blockHitResult);
}
