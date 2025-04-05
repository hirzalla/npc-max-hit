package com.npcmaxhit;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import lombok.Value;

@Value
public class NpcMaxHitData
{
	String npcName;
	String version;
	int npcId;
	Map<String, String> maxHits;

	public NpcMaxHitData(String fullName, int npcId, Map<String, String> maxHits)
	{
		String[] parts = fullName.split("#", 2);
		this.npcName = parts[0].trim().replaceAll("_", " ");
		this.version = parts.length > 1 ? parts[1].split(",")[0] : null;
		this.npcId = npcId;
		this.maxHits = new TreeMap<>((a, b) -> toTitleCase(a).compareTo(toTitleCase(b)));
		maxHits.forEach((style, value) -> this.maxHits.put(toTitleCase(style), value));
	}

	private String toTitleCase(String input)
	{
		if (input == null || input.isEmpty())
		{
			return input;
		}
		String[] words = input.toLowerCase().split("\\s+");
		StringBuilder titleCase = new StringBuilder();
		for (String word : words)
		{
			if (!word.isEmpty())
			{
				titleCase.append(Character.toUpperCase(word.charAt(0)))
					.append(word.substring(1))
					.append(" ");
			}
		}
		return titleCase.toString().trim();
	}

	public String getDisplayName()
	{
		if (version == null || version.isEmpty())
		{
			return npcName;
		}
		return npcName + " (" + version + ")";
	}

	public int getHighestMaxHit()
	{
		return maxHits.values().stream()
			.mapToInt(s -> {
				try
				{
					if (s.contains("+")) // e.g. NpcID.FRAGMENT_OF_SEREN
					{
						// Split on + and sum all numbers
						return Arrays.stream(s.split("\\+"))
							.mapToInt(num -> Integer.parseInt(num.trim()))
							.sum();
					}
					return Integer.parseInt(s.trim());
				}
				catch (NumberFormatException e)
				{
					return 0;
				}
			})
			.max()
			.orElse(0);
	}
}
