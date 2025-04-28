package com.npcmaxhit;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import lombok.Value;

@Value
public class NpcMaxHitData
{
	private static final int MAX_HIT_LENGTH = 25;

	String npcName;
	String version;
	int npcId;
	Map<String, String> maxHits;

	private String truncateHitValue(String value)
	{
		return value.length() > MAX_HIT_LENGTH ? value.substring(0, MAX_HIT_LENGTH) + "..." : value;
	}

	public NpcMaxHitData(String fullName, int npcId, Map<String, String> maxHits)
	{
		String[] parts = fullName.split("#", 2);
		this.npcName = parts[0].trim().replaceAll("_", " ");
		this.version = parts.length > 1 ? parts[1].split(",")[0] : null;
		this.npcId = npcId;
		this.maxHits = new TreeMap<>((a, b) -> toTitleCase(a).compareTo(toTitleCase(b)));
		maxHits.forEach((style, value) -> this.maxHits.put(toTitleCase(style), truncateHitValue(value)));
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
					s = s.replaceAll("\\s+", ""); // remove any whitespace
					if (s.contains("+")) // 12+3 -> 15
					{
						return Arrays.stream(s.split("\\+"))
							.mapToInt(num -> Integer.parseInt(num.trim()))
							.sum();
					}
					else if (s.contains("x")) // 12x3 -> 36
					{
						String[] parts = s.split("x");
						if (parts.length == 2)
						{
							return Integer.parseInt(parts[0].trim()) * Integer.parseInt(parts[1].trim());
						}
					}
					return Integer.parseInt(s.trim());
				}
				catch (NumberFormatException e)
				{
					return 0;
				}
			})
			.max()
			.orElse(-1);
	}
}
