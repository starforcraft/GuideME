package guideme.data;

import guideme.GuideME;
import guideme.guidebook.GuidebookText;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public final class GuideMELanguageProvider extends LanguageProvider {
    public GuideMELanguageProvider(PackOutput gen) {
        super(gen, GuideME.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        addEnum(GuidebookText.class);
    }

    public <T extends Enum<T> & LocalizationEnum> void addEnum(Class<T> localizedEnum) {
        for (var enumConstant : localizedEnum.getEnumConstants()) {
            add(enumConstant.getTranslationKey(), enumConstant.getEnglishText());
        }
    }

    @Override
    public String getName() {
        return "GuideME Translations";
    }
}
