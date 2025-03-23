package com.opponentmaxhit;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@PluginDescriptor(
        name = "Opponent Max Hit",
        description = "Displays the max hit for non-player opponents",
        tags = {"maxhit", "monster", "npc", "opponent", "hit", "damage", "overlay", "combat", "pvm", "pve", "max hit"}
)
public class OpponentMaxHitPlugin extends Plugin {
    private Actor player;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> scheduledTask;
    @Inject
    private Client client;

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
//        wikiService.getMaxHitData("Zulrah").ifPresent(overlay::updateMonsterData);
//        wikiService.getMaxHitData("Araxxor").ifPresent(overlay::updateMonsterData);
//        wikiService.getMaxHitData("Phantom Muspah").ifPresent(overlay::updateMonsterData);

    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event) {

        Actor actor = event.getActor();
        Hitsplat hitsplat = event.getHitsplat();
        // check the actor interface type
        if (!(event.getActor() instanceof NPC) || !hitsplat.isMine()) {
            return;
        }

        // Cancel the previous task if it exists
        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(false);
        }
        int id = ((NPC) actor).getId();
        if (actor.isDead()) {
            log.debug("dead boy");
        }

        log.debug("onHitsplatApplied on " + actor.getName() + " " + id + " with hitsplat type " + event.getHitsplat().getHitsplatType());
        wikiService.getMaxHitData(actor.getName()).ifPresent(overlay::updateMonsterData);


        // Schedule the overlay to be cleared after 5 seconds
        scheduledTask = scheduler.schedule(() -> {
            overlay.updateMonsterData(null);
            log.debug("Overlay cleared after 5 seconds");
        }, 3, TimeUnit.SECONDS);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
            player = client.getLocalPlayer();
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        Actor actor = event.getActor();
        NPC npc = event.getNpc();

//        log.debug("onNpcDespawned on " + actor.getName() + " killed " + npc.getName());

        if (actor == player && event.getActor().getName().equals(overlay.getCurrentMonsterName())) {
            // clear overlay after 5 ticks

            overlay.updateMonsterData(null);
        }
    }


    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
    }

    @Provides
    OpponentMaxHitConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(OpponentMaxHitConfig.class);
    }
}
