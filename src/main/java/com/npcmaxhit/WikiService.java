package com.npcmaxhit;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Singleton
public class WikiService
{
	private static final String WIKI_API_URL = "https://oldschool.runescape.wiki/api.php?action=ask&format=json&query=[[NPC ID::%d]]|?Max hit";
	private static final Pattern MAX_HIT_VALUE_PATTERN = Pattern.compile("(.+?)\\s*\\(([^)]+)\\)");
	private final Map<Integer, List<NpcMaxHitData>> maxHitCache = new HashMap<>();

	@Inject
	private OkHttpClient httpClient;

	@Inject
	private Gson gson;

	public List<NpcMaxHitData> getMaxHitData(int npcId)
	{
		// Check cache first
		if (maxHitCache.containsKey(npcId))
		{
			return maxHitCache.get(npcId);
		}

		try
		{
			String url = String.format(WIKI_API_URL, npcId);

			Request request = new Request.Builder()
				.url(url)
				.header("User-Agent", "RuneLite npc-max-hit plugin")
				.build();

			try (Response response = httpClient.newCall(request).execute())
			{
				if (!response.isSuccessful() || response.body() == null)
				{
					return Collections.emptyList();
				}

				String responseBody = response.body().string();
				JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

				if (!jsonResponse.has("query") || !jsonResponse.getAsJsonObject("query").has("results"))
				{
					return Collections.emptyList();
				}

				List<NpcMaxHitData> results = new ArrayList<>();
				JsonObject resultsObj = jsonResponse.getAsJsonObject("query").getAsJsonObject("results");

				for (Map.Entry<String, JsonElement> entry : resultsObj.entrySet())
				{
					String fullName = entry.getKey();
					JsonObject result = entry.getValue().getAsJsonObject();
					JsonObject printouts = result.getAsJsonObject("printouts");

					if (printouts.has("Max hit") && printouts.getAsJsonArray("Max hit").size() > 0)
					{
						Map<String, String> maxHits = new HashMap<>();
						var maxHitArray = printouts.getAsJsonArray("Max hit");

						if (maxHitArray.size() > 1)
						{
							// Process each element in the array directly
							for (JsonElement maxHitElement : maxHitArray)
							{
								String maxHitString = maxHitElement.getAsString().trim();
								parseMaxHitValues(maxHitString, maxHits);
							}
						}
						else
						{
							// Single element - might contain multiple hits
							String maxHitString = maxHitArray.get(0).getAsString();
							parseMaxHitValues(maxHitString, maxHits);
						}

						if (!maxHits.isEmpty())
						{
							results.add(new NpcMaxHitData(fullName, npcId, maxHits));
						}
					}
				}

				if (!results.isEmpty())
				{
					maxHitCache.put(npcId, results);
					return results;
				}
			}
		}
		catch (Exception e)
		{
			log.warn("Error fetching wiki data for (ID: {}): {}", npcId, e.getMessage());
		}

		return Collections.emptyList();
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
}
