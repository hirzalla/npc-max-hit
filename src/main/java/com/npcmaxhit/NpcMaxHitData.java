package com.npcmaxhit;

import java.util.Map;
import java.util.TreeMap;

import lombok.Value;

@Value
public class NpcMaxHitData
{
	String npcName;
	String version;
	int npcId;
	Map<String, Integer> maxHits;

	public NpcMaxHitData(String fullName, int npcId, Map<String, Integer> maxHits)
	{
		String[] parts = fullName.split("#", 2);
		this.npcName = parts[0].trim().replaceAll("_", " ");
		this.version = parts.length > 1 ? parts[1].trim().replaceAll("_", " ") : null;
		this.npcId = npcId;
		this.maxHits = new TreeMap<>(maxHits);
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
			.mapToInt(Integer::intValue)
			.max()
			.orElse(0);
	}
}
