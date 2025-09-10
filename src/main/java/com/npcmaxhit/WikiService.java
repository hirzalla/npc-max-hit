package com.npcmaxhit;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.HttpUrl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Singleton
public class WikiService
{
	private static final String WIKI_API_BASE = "https://oldschool.runescape.wiki/api.php";
	private static final String BUCKET_QUERY = "bucket('infobox_monster').select('id','name','version_anchor','default_version','max_hit').limit(5000).run()";
	private static final Pattern MAX_HIT_VALUE_PATTERN = Pattern.compile("(.+?)\\s*\\(([^)]+)\\)");
	private final Map<String, List<NpcMaxHitData>> maxHitCache = new ConcurrentHashMap<>();

	@Inject
	private OkHttpClient httpClient;

	@Inject
	private Gson gson;

	public List<NpcMaxHitData> getMaxHitData(int npcId)
	{
		return getMaxHitData(String.valueOf(npcId));
	}

	public List<NpcMaxHitData> getMaxHitData(String npcId)
	{
		return maxHitCache.getOrDefault(npcId, Collections.emptyList());
	}

	public CompletableFuture<Void> preloadAll()
	{
		return CompletableFuture.runAsync(() -> {
			try
			{
				HttpUrl url = HttpUrl.parse(WIKI_API_BASE).newBuilder()
					.addQueryParameter("action", "bucket")
					.addQueryParameter("format", "json")
					.addQueryParameter("query", BUCKET_QUERY)
					.addQueryParameter("smaxage", "86400")
					.build();

				Request request = new Request.Builder()
					.url(url)
					.header("User-Agent", "RuneLite npc-max-hit plugin")
					.header("Accept", "application/json")
					.build();

				try (Response response = httpClient.newCall(request).execute())
				{
					if (response.isSuccessful() && response.body() != null)
					{
						parseBucketResponseAndFillCache(response.body().string());
					}
					else
					{
						log.warn("Bucket API request failed: {}", response.code());
					}
				}
			}
			catch (Exception e)
			{
				log.warn("Error preloading bucket data: {}", e.getMessage());
			}
		});
	}

	private void parseBucketResponseAndFillCache(String responseBody)
	{
		JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
		if (jsonResponse == null || !jsonResponse.has("bucket"))
		{
			return;
		}

		Map<String, List<NpcMaxHitData>> temp = new HashMap<>();

		for (JsonElement el : jsonResponse.getAsJsonArray("bucket"))
		{
			JsonObject obj = el.getAsJsonObject();

			List<String> idList = getAsStringList(obj, "id");
			List<String> maxHitStrings = getAsStringList(obj, "max_hit");

			String name = obj.has("name") && !obj.get("name").isJsonNull() ? obj.get("name").getAsString() : null;
			String version = obj.has("version_anchor") && !obj.get("version_anchor").isJsonNull() ? obj.get("version_anchor").getAsString() : null;
			boolean isDefault = obj.has("default_version");

			Map<String, String> maxHits = new HashMap<>();
			// each element may itself include <br/> or comma-separated values
			for (String s : maxHitStrings)
			{
				if (s != null)
				{
					parseMaxHitValues(s, maxHits);
				}
			}

			for (String idStr : idList)
			{
				if (idStr == null || idStr.trim().isEmpty())
				{
					continue;
				}
				String key = idStr.trim();
				NpcMaxHitData data = new NpcMaxHitData(name, version, key, isDefault, maxHits);
				temp.computeIfAbsent(key, k -> new ArrayList<>()).add(data);
			}
		}

		maxHitCache.clear();

		// de-dupe per-id by identical maxHits; if a default version exists in a group, keep only that
		for (Map.Entry<String, List<NpcMaxHitData>> e : temp.entrySet())
		{
			Map<String, List<NpcMaxHitData>> byHits = new HashMap<>();
			for (NpcMaxHitData d : e.getValue())
			{
				String key = canonicalizeMaxHits(d.getMaxHits());
				byHits.computeIfAbsent(key, k -> new ArrayList<>()).add(d);
			}

			List<NpcMaxHitData> filtered = new ArrayList<>();
			for (List<NpcMaxHitData> group : byHits.values())
			{
				NpcMaxHitData chosen = group.stream().filter(NpcMaxHitData::isDefaultVersion).findFirst().orElse(group.get(0));
				filtered.add(chosen);
			}

			// order: default version first, then by version name (if present)
			filtered.sort((a, b) -> {
				if (a.isDefaultVersion() != b.isDefaultVersion())
				{
					return a.isDefaultVersion() ? -1 : 1;
				}
				String va = a.getVersion();
				String vb = b.getVersion();
				if (va == null && vb == null)
				{
					return 0;
				}
				if (va == null)
				{
					return 1;
				}
				if (vb == null)
				{
					return -1;
				}
				return va.compareToIgnoreCase(vb);
			});

			maxHitCache.put(e.getKey(), filtered);
		}
	}

	private String canonicalizeMaxHits(Map<String, String> maxHits)
	{
		// sort keys case-insensitively and build a stable string
		StringBuilder sb = new StringBuilder();
		maxHits.keySet().stream().sorted(String::compareToIgnoreCase).forEach(k -> {
			sb.append(k.toLowerCase()).append('=').append(maxHits.get(k).trim()).append(';');
		});
		return sb.toString();
	}

	private List<String> getAsStringList(JsonObject obj, String key)
	{
		List<String> out = new ArrayList<>();
		if (!obj.has(key) || obj.get(key).isJsonNull())
		{
			return out;
		}
		JsonElement el = obj.get(key);
		if (el.isJsonArray())
		{
			for (JsonElement e : el.getAsJsonArray())
			{
				if (!e.isJsonNull())
				{
					out.add(e.getAsString());
				}
			}
		}
		else
		{
			out.add(el.getAsString());
		}
		return out;
	}

	private void parseMaxHitValues(String maxHitString, Map<String, String> maxHits)
	{
		// Remove HTML line break tags and split on commas
		String[] hits = maxHitString.replaceAll("<br/?>", ",").split(",");

		for (String hit : hits)
		{
			hit = hit.trim();
			Matcher matcher = MAX_HIT_VALUE_PATTERN.matcher(hit);

			if (matcher.find())
			{
				String value = matcher.group(1).trim();
				String style = matcher.group(2).trim();
				maxHits.put(style, value);
			}
			else
			{
				// If it's not empty, store as is
				if (!hit.isEmpty())
				{
					maxHits.put("Max Hit", hit);
				}
				else
				{
					log.warn("Invalid max hit value: {}", hit);
				}
			}
		}
	}

	public void clearCache()
	{
		maxHitCache.clear();
	}
}
