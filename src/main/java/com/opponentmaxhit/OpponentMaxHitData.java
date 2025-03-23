package com.opponentmaxhit;

import java.util.Map;
import java.util.TreeMap;

import lombok.Getter;
import lombok.Value;

@Value
public class OpponentMaxHitData {
    private final String monsterName;
    @Getter
    private final Map<String, Integer> allMaxHits;

    public OpponentMaxHitData(String monsterName, Map<String, Integer> maxHits) {
        this.monsterName = monsterName;
        // Create sorted map based on version order
        this.allMaxHits = new TreeMap<>((a, b) -> {
            // Base version always comes first
            if (a.startsWith("Base")) return -1;
            if (b.startsWith("Base")) return 1;

            // Extract version names for comparison
            String[] partsA = a.split(" ", 2);
            String[] partsB = b.split(" ", 2);

            // Compare versions
            return partsA[0].compareTo(partsB[0]);
        });
        this.allMaxHits.putAll(maxHits);
    }

    public int getHighestMaxHit() {
        return allMaxHits.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }
}
