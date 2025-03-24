package com.opponentmaxhit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Singleton
public class WikiService {
    private static final String WIKI_API_URL = "https://oldschool.runescape.wiki/api.php?action=parse&format=json&prop=wikitext&page=";
    private static final String WIKI_LOOKUP_URL = "https://oldschool.runescape.wiki/w/Special:Lookup?type=npc&id=%d&name=%s";
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\|\\s*version(\\d+)\\s*=\\s*([^\\n|]+)");
    private static final Pattern MAX_HIT_PATTERN = Pattern.compile("\\|\\s*max hit(\\d*)\\s*=\\s*([^\\n|]+)");
    private static final Pattern MAX_HIT_VALUE_PATTERN = Pattern.compile("(\\d+)\\s*\\(([^)]+)\\)");

    private final Map<Integer, OpponentMaxHitData> maxHitCache = new HashMap<>();
    private final Map<Integer, String> npcNameCache = new HashMap<>();

    @Inject
    private OkHttpClient httpClient;

    @Inject
    private void init() {
        httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(chain -> chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "RuneLite opponent-max-hit plugin")
                    .build()))
            .build();
    }

    public Optional<OpponentMaxHitData> getMaxHitData(String monsterName, int npcId) {
        // Check cache first
        if (maxHitCache.containsKey(npcId)) {
            return Optional.of(maxHitCache.get(npcId));
        }

        try {
            // First try to get the canonical page name
            String pageName = getCanonicalName(monsterName, npcId);
            if (pageName == null) {
                return Optional.empty();
            }

            String encodedPage = java.net.URLEncoder.encode(pageName, "UTF-8");
            String url = WIKI_API_URL + encodedPage;

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.debug("Failed to fetch wiki data for {} (ID: {}): {}", pageName, npcId, response.code());
                    return Optional.empty();
                }

                String responseBody = response.body().string();
                JsonObject jsonResponse = new Gson().fromJson(responseBody, JsonObject.class);

                if (!jsonResponse.has("parse") || !jsonResponse.getAsJsonObject("parse").has("wikitext")) {
                    log.debug("Invalid wiki response for {} (ID: {})", pageName, npcId);
                    return Optional.empty();
                }

                String wikitext = jsonResponse.getAsJsonObject("parse")
                        .getAsJsonObject("wikitext")
                        .get("*").getAsString();

                Map<String, Integer> maxHits = parseMaxHits(wikitext, npcId);
                if (!maxHits.isEmpty()) {
                    OpponentMaxHitData data = new OpponentMaxHitData(pageName, npcId, maxHits);
                    log.info("Fetched highest max hit data for {} (ID: {}): {}", pageName, npcId, data.getHighestMaxHit());
                    maxHitCache.put(npcId, data);
                    return Optional.of(data);
                }
            }
        } catch (Exception e) {
            log.debug("Error fetching wiki data for {} (ID: {}): {}", monsterName, npcId, e.getMessage());
        }
        return Optional.empty();
    }

    private String getCanonicalName(String monsterName, int npcId) {
        if (npcNameCache.containsKey(npcId)) {
            return npcNameCache.get(npcId);
        }

        try {
            String lookupUrl = String.format(WIKI_LOOKUP_URL, npcId, 
                java.net.URLEncoder.encode(monsterName, "UTF-8"));

            Request request = new Request.Builder()
                    .url(lookupUrl)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return null;
                }

                String finalUrl = response.request().url().toString();
                // Extract the page name from the final URL
                String pageName = finalUrl.substring(finalUrl.indexOf("/w/") + 3);
                // URL decode the page name
                pageName = java.net.URLDecoder.decode(pageName, "UTF-8");
                
                // If there's a section (indicated by #), include it
                // This helps differentiate between different forms of the same NPC
                npcNameCache.put(npcId, pageName);
                return pageName;
            }
        } catch (Exception e) {
            log.debug("Error getting canonical name for {} (ID: {}): {}", monsterName, npcId, e.getMessage());
        }
        return null;
    }

    private Map<String, Integer> parseMaxHits(String wikitext, int npcId) {
        Map<String, Integer> maxHits = new HashMap<>();
        String versionName = null;
        int versionNumber = -1;

        // Check if the cached page name contains a version (after #)
        String pageName = npcNameCache.get(npcId);
        if (pageName != null && pageName.contains("#")) {
            versionName = pageName.substring(pageName.indexOf("#") + 1);
            versionNumber = findVersionNumber(wikitext, versionName);
            log.debug("Found version name: {} with number: {}", versionName, versionNumber);
        }

        // Try to find version-specific max hit first
        if (versionNumber > 0) {
            String versionSpecificMaxHits = findMaxHitForVersion(wikitext, versionNumber);
            log.debug("Found version-specific max hits: {}", versionSpecificMaxHits);
            if (versionSpecificMaxHits != null) {
                parseMaxHitValues(versionSpecificMaxHits, maxHits);
                return maxHits;
            }
        }

        // Fall back to default max hit if no version-specific one found
        String defaultMaxHits = findMaxHitForVersion(wikitext, 0);
        log.debug("Found default max hits: {}", defaultMaxHits);
        if (defaultMaxHits != null) {
            parseMaxHitValues(defaultMaxHits, maxHits);
        }

        return maxHits;
    }

    private int findVersionNumber(String wikitext, String targetVersion) {
        // Normalize target version by replacing underscores with spaces and cleaning
        String normalizedTarget = targetVersion.replace('_', ' ').replaceAll("[()]", "").trim();
        
        Matcher versionMatcher = VERSION_PATTERN.matcher(wikitext);
        while (versionMatcher.find()) {
            int number = Integer.parseInt(versionMatcher.group(1));
            String version = versionMatcher.group(2).trim();
            
            // Normalize version from wiki by cleaning
            String normalizedVersion = version.replaceAll("[()]", "").trim();
            
            log.debug("Comparing versions - Target: '{}' vs Wiki: '{}' (number: {})", 
                     normalizedTarget, normalizedVersion, number);
                     
            if (normalizedVersion.equalsIgnoreCase(normalizedTarget)) {
                return number;
            }
        }
        
        // If exact match fails, try matching just the level number
        if (normalizedTarget.startsWith("Level")) {
            String targetLevel = normalizedTarget.substring("Level".length()).trim();
            Matcher versionMatcher2 = VERSION_PATTERN.matcher(wikitext);
            
            while (versionMatcher2.find()) {
                int number = Integer.parseInt(versionMatcher2.group(1));
                String version = versionMatcher2.group(2).trim();
                
                if (version.contains(targetLevel)) {
                    return number;
                }
            }
        }
        
        return -1;
    }

    private String findMaxHitForVersion(String wikitext, int versionNumber) {
        String versionSuffix = versionNumber > 0 ? String.valueOf(versionNumber) : "";
        Pattern pattern = Pattern.compile("\\|\\s*max hit" + versionSuffix + "\\s*=\\s*([^\\n|]+)");
        Matcher matcher = pattern.matcher(wikitext);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private void parseMaxHitValues(String maxHitSection, Map<String, Integer> maxHits) {
        String[] hits = maxHitSection.split("(?:<br/?>|,)");
        
        for (String hit : hits) {
            hit = hit.trim().replaceAll("<[^>]+>", "");
            
            Matcher valueMatcher = MAX_HIT_VALUE_PATTERN.matcher(hit);
            if (valueMatcher.find()) {
                int value = Integer.parseInt(valueMatcher.group(1));
                String style = valueMatcher.group(2).trim();
                maxHits.put(style, value);
            } else {
                try {
                    int value = Integer.parseInt(hit);
                    maxHits.put("Max hit", value);
                } catch (NumberFormatException ignored) {}
            }
        }
    }
}

