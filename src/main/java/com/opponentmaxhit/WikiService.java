package com.opponentmaxhit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Singleton
public class WikiService {
    private static final String WIKI_API_URL = "https://oldschool.runescape.wiki/api.php?action=parse&page=%s&prop=wikitext&format=json";
    private static final Pattern MAX_HIT_PATTERN = Pattern.compile("\\|\\s*max hit\\s*=\\s*([^\\n|]+)");
    private static final Pattern MAX_HIT_VALUE_PATTERN = Pattern.compile("(\\d+)\\s*\\(([^)]+)\\)");
    private static final Pattern VERSION_NAME_PATTERN = Pattern.compile("\\|version(\\d+)\\s*=\\s*([^\\n|]+)");
    private static final Pattern VERSION_HIT_PATTERN = Pattern.compile("\\|\\s*max hit(\\d+)\\s*=\\s*([^\\n|]+)");

    @Inject
    private OkHttpClient httpClient;

    private final Map<String, OpponentMaxHitData> maxHitCache = new HashMap<>();

    public Optional<OpponentMaxHitData> getMaxHitData(String monsterName) {
        // Check cache first
        if (maxHitCache.containsKey(monsterName)) {
            return Optional.of(maxHitCache.get(monsterName));
        }

        try {
            String encodedName = URLEncoder.encode(monsterName, StandardCharsets.UTF_8);
            Request request = new Request.Builder()
                    .url(String.format(WIKI_API_URL, encodedName))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return Optional.empty();
                }

                String responseBody = response.body().string();
                JsonObject jsonResponse = new Gson().fromJson(responseBody, JsonObject.class);
                String wikitext = jsonResponse.getAsJsonObject("parse")
                        .getAsJsonObject("wikitext")
                        .get("*").getAsString();

                OpponentMaxHitData data = parseMaxHits(wikitext, monsterName);
                if (data != null) {
                    maxHitCache.put(monsterName, data);
                    return Optional.of(data);
                }
            }
        } catch (IOException e) {
            log.error("Error fetching wiki data for " + monsterName, e);
        }
        return Optional.empty();
    }

    // move to separate WikiTextParser class or similar
    private OpponentMaxHitData parseMaxHits(String wikitext, String monsterName) {
        Map<String, Integer> maxHits = new HashMap<>();

        // Extract version names
        Matcher versionNameMatcher = VERSION_NAME_PATTERN.matcher(wikitext);
        Map<String, String> versionNames = new HashMap<>();
        while (versionNameMatcher.find()) {
            versionNames.put(versionNameMatcher.group(1), versionNameMatcher.group(2).trim());
        }

        // Handle base max hit (unnumbered)
        Matcher baseHitMatcher = MAX_HIT_PATTERN.matcher(wikitext);
        if (baseHitMatcher.find()) {
            String maxHitSection = baseHitMatcher.group(1).trim();

            if (maxHitSection.contains("<br")) {
                // Handle break tag separated values as separate entries
                String[] hits = maxHitSection.split("<br/?>");
                for (String hit : hits) {
                    hit = hit.trim();
                    Matcher valueMatcher = MAX_HIT_VALUE_PATTERN.matcher(hit);
                    if (valueMatcher.find()) {
                        int value = Integer.parseInt(valueMatcher.group(1));
                        String style = valueMatcher.group(2).trim();
                        maxHits.put("Base (" + style + ")", value);
                    } else {
                        try {
                            int value = Integer.parseInt(hit);
                            maxHits.put("Base", value);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            } else {
                // Handle comma separated values, keeping only highest
                String[] hits = maxHitSection.split(",");
                int highestValue = 0;
                String highestStyle = "";

                for (String hit : hits) {
                    hit = hit.trim();
                    Matcher valueMatcher = MAX_HIT_VALUE_PATTERN.matcher(hit);
                    if (valueMatcher.find()) {
                        int value = Integer.parseInt(valueMatcher.group(1));
                        String style = valueMatcher.group(2).trim();
                        if (value > highestValue) {
                            highestValue = value;
                            highestStyle = style;
                        }
                    } else {
                        try {
                            int value = Integer.parseInt(hit);
                            if (value > highestValue) {
                                highestValue = value;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }

                String key = highestStyle.isEmpty() ? "Base" : "Base (" + highestStyle + ")";
                maxHits.put(key, highestValue);
            }
        }

        // Handle versioned max hits (unchanged)
        Matcher versionHitMatcher = VERSION_HIT_PATTERN.matcher(wikitext);

        while (versionHitMatcher.find()) {
            String versionNumber = versionHitMatcher.group(1);
            String maxHitSection = versionHitMatcher.group(2).trim();
            String versionName = versionNames.getOrDefault(versionNumber, "Base");

            // Split by commas and process each hit
            String[] hits = maxHitSection.split(",");
            int highestValue = 0;
            String highestStyle = "";

            for (String hit : hits) {
                hit = hit.trim();
                Matcher valueMatcher = MAX_HIT_VALUE_PATTERN.matcher(hit);
                if (valueMatcher.find()) {
                    int value = Integer.parseInt(valueMatcher.group(1));
                    String style = valueMatcher.group(2).trim();
                    if (value > highestValue) {
                        highestValue = value;
                        highestStyle = style;
                    }
                } else {
                    try {
                        int value = Integer.parseInt(hit);
                        if (value > highestValue) {
                            highestValue = value;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            String key = highestStyle.isEmpty() ? versionName : versionName + " (" + highestStyle + ")";
            maxHits.put(key, highestValue);
        }

        return maxHits.isEmpty() ? null : new OpponentMaxHitData(monsterName, maxHits);    }
}
