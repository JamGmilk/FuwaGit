package jamgmilk.fuwagit.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LanguageManager {
    /**
     * Sets the application language.
     *
     * @param languageTag BCP-47 language tag. Supported values:
     *   - "system": Use system language (clears Per-App Language setting)
     *   - "en": English
     *   - "zh-Hans": Simplified Chinese
     *
     * On API 33+, this uses LocaleManager internally.
     * On API 26-32, this uses AppCompatDelegate with auto-persistence.
     */
    fun setLanguage(languageTag: String) {
        if (languageTag == "system") {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else {
            val appLocales = LocaleListCompat.forLanguageTags(languageTag)
            AppCompatDelegate.setApplicationLocales(appLocales)
        }
    }
}
