package heizoel.backend.confirmation.adapter.out.geocoding;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class AddressNormalizer {

    private static final Pattern STREET_ABBREVIATION_PATTERN =
            Pattern.compile("\\bstr\\.?\\b", Pattern.CASE_INSENSITIVE);

    public String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT)
                .replace("ß", "ss")
                .replace("ä", "ae")
                .replace("ö", "oe")
                .replace("ü", "ue")
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    public String cacheKey(String value) {
        return normalize(expandCommonAbbreviations(value));
    }

    public String toGeocodingQuery(String value) {
        if (value == null) {
            return "";
        }

        return expandCommonAbbreviations(value)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String expandCommonAbbreviations(String value) {
        String expanded = STREET_ABBREVIATION_PATTERN.matcher(value).replaceAll("strasse");
        return expanded
                .replace("ß", "ss")
                .replace("Ä", "Ae")
                .replace("Ö", "Oe")
                .replace("Ü", "Ue")
                .replace("ä", "ae")
                .replace("ö", "oe")
                .replace("ü", "ue");
    }
}


