package com.opponentmaxhit;

import java.util.Map;
import java.util.TreeMap;

import lombok.Value;

@Value
public class OpponentMaxHitData {
    String monsterName;
    int npcId;
    Map<String, Integer> maxHits;

    public OpponentMaxHitData(String monsterName, int npcId, Map<String, Integer> maxHits) {
        this.monsterName = monsterName;
        this.npcId = npcId;
        this.maxHits = new TreeMap<>();
        this.maxHits.putAll(maxHits);
    }

    public int getHighestMaxHit() {
        return maxHits.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }
}
