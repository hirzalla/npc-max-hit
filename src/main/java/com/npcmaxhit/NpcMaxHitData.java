package com.npcmaxhit;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import lombok.Value;

@Value
public class NpcMaxHitData
{
	String npcName;
	String version;
	String npcId;
	boolean defaultVersion;
	Map<String, String> maxHits;

	public NpcMaxHitData(String name, String version, String npcId, boolean defaultVersion, Map<String, String> maxHits)
	{
		this.npcName = (name == null ? "" : name.trim().replaceAll("_", " "));
		this.version = (version == null || version.isEmpty()) ? null : version.trim();
		this.npcId = npcId == null ? "" : npcId.trim();
		this.defaultVersion = defaultVersion;
		this.maxHits = new TreeMap<>((a, b) -> toTitleCase(a).compareTo(toTitleCase(b)));
		maxHits.forEach((style, value) -> this.maxHits.put(toTitleCase(style), value == null ? "" : value.trim()));
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
		int best = -1;
		Pattern rangePattern = Pattern.compile("(\\d+)\\s*-\\s*(\\d+)");
		Pattern exprPattern = Pattern.compile("\\d+(?:\\s*[+x]\\s*\\d+)+", Pattern.CASE_INSENSITIVE);
		Pattern intPattern = Pattern.compile("\\d+");

		for (String raw : maxHits.values())
		{
			if (raw == null || raw.isEmpty())
			{
				continue;
			}

			String s = raw;

			// 1) Handle ranges like "3-10" -> take the upper bound (10)
			Matcher r = rangePattern.matcher(s);
			while (r.find())
			{
				try
				{
					int a = Integer.parseInt(r.group(1));
					int b = Integer.parseInt(r.group(2));
					best = Math.max(best, Math.max(a, b));
				}
				catch (NumberFormatException ignored)
				{
				}
			}

			// 2) Handle simple + and x expressions like "4+8+20" or "12x3"
			Matcher e = exprPattern.matcher(s);
			while (e.find())
			{
				String expr = e.group();
				try
				{
					int val;
					if (expr.contains("x") || expr.contains("X"))
					{
						// product chain
						String[] parts = expr.split("[xX]");
						val = 1;
						for (String p : parts)
						{
							val *= Integer.parseInt(p.trim());
						}
					}
					else
					{
						// sum chain
						String[] parts = expr.split("\\+");
						val = 0;
						for (String p : parts)
						{
							val += Integer.parseInt(p.trim());
						}
					}
					best = Math.max(best, val);
				}
				catch (NumberFormatException ignored)
				{
				}
			}

			// 3) Any standalone integers (e.g., "32 total 4+8+20" -> 32)
			Matcher m = intPattern.matcher(s);
			while (m.find())
			{
				try
				{
					int v = Integer.parseInt(m.group());
					best = Math.max(best, v);
				}
				catch (NumberFormatException ignored)
				{
				}
			}
		}

		return best;
	}
}
