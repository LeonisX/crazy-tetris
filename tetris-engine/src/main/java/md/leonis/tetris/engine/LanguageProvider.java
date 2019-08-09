package md.leonis.tetris.engine;

import md.leonis.tetris.engine.model.Language;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;

public class LanguageProvider {

    private static final Logger LOGGER = Logger.getLogger("LanguageProvider");

    private static final String BUNDLE_PREFIX = "language";

    private static final Locale ENGLISH = new Locale("en", "US");
    private static final Locale RUSSIAN = new Locale("ru", "RU");

    private static final Map<Language, ResourceBundle> languageMap = new HashMap<>();
    private static final Map<Locale, ResourceBundle> bundleMap = new HashMap<>();

    private static final ResourceBundle RESOURCE_BUNDLE_RU = initializeBundle(RUSSIAN);
    private static final ResourceBundle RESOURCE_BUNDLE_EN = initializeBundle(ENGLISH);

    private static Language currentLanguage = Language.RU;

    static {
        languageMap.put(Language.RU, RESOURCE_BUNDLE_RU);
        languageMap.put(Language.EN, RESOURCE_BUNDLE_EN);
        bundleMap.put(RUSSIAN, RESOURCE_BUNDLE_RU);
        bundleMap.put(ENGLISH, RESOURCE_BUNDLE_EN);
    }

    private List<Locale> locales = Collections.unmodifiableList(Arrays.asList(ENGLISH, RUSSIAN));

    public LanguageProvider() {
    }

    public LanguageProvider(Language currentLanguage) {
        LanguageProvider.currentLanguage = currentLanguage;
    }

    public List<Locale> getProvidedLocales() {
        return locales;
    }

    public String getTranslation(String key, Locale locale, Object... params) {
        return translate(key, bundleMap.get(locale), params);
    }

    public String getTranslation(String key, Object[] params) {
        return getTranslation(key, currentLanguage, params);
    }

    public String getTranslation(String key, Language language, Object... params) {
        return translate(key, languageMap.get(language), params);
    }

    private String translate(String key, ResourceBundle bundle, Object... params) {
        if (key == null) {
            LOGGER.warning("Got lang request for key with null value!");
            return "";
        }

        if (!bundle.containsKey(key)) {
            LOGGER.warning("Missing resource key (i18n): " + key);
            return key;
        } else {
            String value = bundle.getString(key);
            if (params.length > 0) {
                value = MessageFormat.format(value, params);
            }
            return value;
        }
    }

    private static ResourceBundle initializeBundle(final Locale locale) {
        final ClassLoader classLoader = LanguageProvider.class.getClassLoader();
        //propertiesBundle = ResourceBundle.getBundle(BUNDLE_PREFIX, locale, classLoader); - doesn't support Russian UTF-8 :(
        return newBundle(BUNDLE_PREFIX, locale, classLoader);
    }

    // Original of this method is in the ResourceBundle class
    private static ResourceBundle newBundle(String baseName, Locale locale, ClassLoader loader) {
        String resourceName = baseName + "_" + locale.getLanguage() + ".properties";
        try {
            InputStream stream = loader.getResourceAsStream(resourceName);
            if (stream == null) {
                throw new IOException("Can't find bundle file: " + resourceName);
            }
            try {
                return new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
            } finally {
                stream.close();
            }
        } catch (Exception e) {
            LOGGER.severe("Can't find bundle for base name: " + baseName + ", locale: " + locale);
            throw new RuntimeException("Can't load language bundle: " + resourceName, e);
        }
    }

    public static Language getCurrentLanguage() {
        return currentLanguage;
    }

    public static void setCurrentLanguage(Language currentLanguage) {
        LanguageProvider.currentLanguage = currentLanguage;
    }
}
