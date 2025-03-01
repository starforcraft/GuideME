package guideme.scene;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SodiumCompat {
    private static final Logger LOG = LoggerFactory.getLogger(SodiumCompat.class);

    @Nullable
    private static final MethodHandle METHOD_HANDLE;

    static {
        MethodHandle handle = null;
        try {
            handle = MethodHandles.lookup().findStatic(
                    Class.forName("me.jellysquid.mods.sodium.client.render.texture.SpriteUtil"),
                    "markSpriteActive",
                    MethodType.methodType(void.class, TextureAtlasSprite.class));
            LOG.info("Loaded Sodium active sprite compat.");
        } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
            if (ModList.get().getModContainerById("sodium").isPresent()) {
                LOG.error("Failed to load Sodium active sprite compat.", e);
            }
        }

        METHOD_HANDLE = handle;
    }

    public static void markSpriteActive(TextureAtlasSprite sprite) {
        if (sprite != null && METHOD_HANDLE != null) {
            try {
                METHOD_HANDLE.invokeExact(sprite);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to invoke SpriteUtil#markSpriteActive", e);
            }
        }
    }
}
