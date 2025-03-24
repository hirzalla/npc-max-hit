package com.npcmaxhit;

import java.util.Map;
import java.util.TreeMap;

import lombok.Value;

@Value
public class NpcMaxHitData
{
	String npcName;
	int npcId;
	Map<String, Integer> maxHits;

	public NpcMaxHitData(String npcName, int npcId, Map<String, Integer> maxHits)
	{
		this.npcName = npcName;
		this.npcId = npcId;
		this.maxHits = new TreeMap<>();
		this.maxHits.putAll(maxHits);
	}

	public int getHighestMaxHit()
	{
		return maxHits.values().stream()
			.mapToInt(Integer::intValue)
			.max()
			.orElse(0);
	}
}
