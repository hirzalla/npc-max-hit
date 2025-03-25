package com.npcmaxhit;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.callback.ClientThread;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@PluginDescriptor(
	name = "Npc Max Hit",
	description = "Displays the max hit for non-player opponents",
	tags = {"maxhit", "monster", "boss", "npc", "opponent", "hit", "damage", "overlay", "combat", "pvm", "pve", "max hit"}
)
public class NpcMaxHitPlugin extends Plugin
{
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
	private NpcMaxHitOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private NpcMaxHitConfig config;

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);

		// Test cases with specific NPC IDs
		// 3017 Giant spider
//		wikiService.getMaxHitData("Giant spider", 3017).ifPresent(overlay::updateNpcData);
//		wikiService.getMaxHitData("Goblin", 3029).ifPresent(overlay::updateNpcData);
//		wikiService.getMaxHitData("Zulrah", 2042).ifPresent(overlay::updateNpcData); // Green form
//		wikiService.getMaxHitData("Phantom Muspah", 12079).ifPresent(overlay::updateNpcData);
		wikiService.getMaxHitData("Araxxor", 13668).ifPresent(overlay::updateNpcData);
		wikiService.getMaxHitData("Duke Sucellus", 12191).ifPresent(overlay::updateNpcData);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlay.updateNpcData(null);

		try
		{
			executor.shutdownNow();
			if (!executor.awaitTermination(1, TimeUnit.SECONDS))
			{
				log.warn("Executor didn't terminate in the specified time.");
			}
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			log.warn("Executor shutdown interrupted", e);
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		Actor actor = event.getActor();
		Hitsplat hitsplat = event.getHitsplat();

		log.debug("Hitsplat applied: " + hitsplat.getHitsplatType() + " to " + actor.getName());
		log.debug("Hitsplat isMine: " + hitsplat.isMine());

		// Only interested in NPC hitsplats caused by the player
		if (!(actor instanceof NPC) || !hitsplat.isMine())
		{
			return;
		}

		NPC npc = (NPC) actor;
		String npcName = npc.getName();
		int npcId = npc.getId();

		// Update last hitsplat time
		lastHitsplatTime = System.currentTimeMillis();

		// if the overlay includes the same npc/ID, no need to request data again
		if (overlay.getCurrentNpcName() != null && overlay.getCurrentNpcName().equals(npcName) && npcId == overlay.getCurrentNpcId())
		{
			return;
		}

		// Run wiki request in background
		executor.submit(() -> {
			log.debug("Getting max hit data for {} (ID: {})", npcName, npcId);
			Optional<NpcMaxHitData> data = wikiService.getMaxHitData(npcName, npcId);
			data.ifPresent(npcData -> {
					log.debug("Got max hit data for {} (ID: {}): {}", npcName, npcId, npcData.getHighestMaxHit());
					clientThread.invoke(() -> overlay.updateNpcData(npcData));
				}
			);
		});
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		long currentTime = System.currentTimeMillis();
		int timeoutMs = config.inactivityTimeout() * 1000;
		if (currentTime - lastHitsplatTime >= timeoutMs && overlay.getCurrentNpcName() != null)
		{
			clientThread.invoke(() -> {
				overlay.updateNpcData(null);
				log.debug("Overlay cleared after {} seconds of inactivity", config.inactivityTimeout());
			});
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			player = client.getLocalPlayer();
		}
	}

	@Provides
	NpcMaxHitConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(NpcMaxHitConfig.class);
	}
}
