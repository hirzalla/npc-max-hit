package com.opponentmaxhit;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.task.Schedule;

import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@PluginDescriptor(
        name = "Opponent Max Hit",
        description = "Displays the max hit for non-player opponents",
        tags = {"maxhit", "monster", "npc", "opponent", "hit", "damage", "overlay", "combat", "pvm", "pve", "max hit"}
)
public class OpponentMaxHitPlugin extends Plugin {
    private Actor player;
    private long lastHitsplatTime = 0;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();


    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private WikiService wikiService;

    @Inject
    private OpponentMaxHitOverlay overlay;

    @Inject
    private OverlayManager overlayManager;

    @Override
    protected void startUp() throws Exception {
        overlayManager.add(overlay);
        // For testing (last one applies
        wikiService.getMaxHitData("Goblin").ifPresent(overlay::updateMonsterData);
        wikiService.getMaxHitData("Giant spider").ifPresent(overlay::updateMonsterData);
        wikiService.getMaxHitData("Zulrah").ifPresent(overlay::updateMonsterData);
        wikiService.getMaxHitData("Araxxor").ifPresent(overlay::updateMonsterData);
        wikiService.getMaxHitData("Phantom Muspah").ifPresent(overlay::updateMonsterData);


    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        overlay.updateMonsterData(null);

        try {
            executor.shutdownNow();
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                log.warn("Executor didn't terminate in the specified time.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Executor shutdown interrupted", e);
        }
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event) {
        Actor actor = event.getActor();
        Hitsplat hitsplat = event.getHitsplat();

        log.info("Hitsplat applied: " + hitsplat.getHitsplatType() + " to " + actor.getName());
        log.info("Hitsplat isMine: " + hitsplat.isMine());

        if (!(actor instanceof NPC) || !hitsplat.isMine()) {
            return;
        }

        lastHitsplatTime = System.currentTimeMillis();
        log.info("valid Hitsplat time: " + lastHitsplatTime);

        // Capture the actor name before submitting to executor
        final String monsterName = actor.getName();
        
        // Run wiki request in background
        executor.submit(() -> {
            log.info("Getting max hit data for " + monsterName);
            Optional<OpponentMaxHitData> data = wikiService.getMaxHitData(monsterName);
            data.ifPresent(monsterData -> {
                log.info("Got max hit data for " + monsterName + ": " + monsterData.getHighestMaxHit());
                clientThread.invoke(() -> overlay.updateMonsterData(monsterData));
                }
            );
        });
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastHitsplatTime >= 6000 && overlay.getCurrentMonsterName() != null) {
            clientThread.invoke(() -> {
                overlay.updateMonsterData(null);
                log.debug("Overlay cleared after 6 seconds of inactivity");
            });
        }
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged interactingChanged){
        Actor source = interactingChanged.getSource();
        Actor target = interactingChanged.getTarget();
        if (source == null || target == null) {
            return;
        }
       log.info("Interacting changed: " + source.getName() + " -> " + target.getName());

    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
            player = client.getLocalPlayer();
        }
    }

    @Provides
    OpponentMaxHitConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(OpponentMaxHitConfig.class);
    }
}
