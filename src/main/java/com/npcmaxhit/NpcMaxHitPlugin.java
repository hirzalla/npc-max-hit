package com.npcmaxhit;

import com.google.inject.Inject;
import com.google.inject.Provides;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

@Slf4j
@PluginDescriptor(
	name = "NPC Max Hit",
	description = "Displays the max hits of the NPC you are fighting",
	tags = {"maxhit", "monster", "boss", "npc", "opponent", "hit", "damage", "overlay", "combat", "pvm", "pve", "max hit", "infobox"}
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

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private SpriteManager spriteManager;

	private NpcMaxHitInfoBox infoBox;

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);

		// testing
//		wikiService.getMaxHitData("Giant spider", 3017).ifPresent(overlay::updateNpcData);
//		wikiService.getMaxHitData("Goblin", 3029).ifPresent(overlay::updateNpcData);
//		wikiService.getMaxHitData("Zulrah", 2042).ifPresent(overlay::updateNpcData); // Green form
//		wikiService.getMaxHitData("Phantom Muspah", 12079).ifPresent(overlay::updateNpcData);
//		wikiService.getMaxHitData("Araxxor", 13668).ifPresent(overlay::updateNpcData);
//		wikiService.getMaxHitData("Duke Sucellus", 12191).ifPresent(overlay::updateNpcData);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlay.updateNpcData(null);
		removeInfoBox();

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

	private void removeInfoBox()
	{
		if (infoBox != null)
		{
			infoBoxManager.removeInfoBox(infoBox);
			infoBox = null;
		}
	}

	private void updateInfoBox(NpcMaxHitData data)
	{
		removeInfoBox();
		if (data != null && config.showInfobox())
		{
			final int RED_HITSPLAT = 1359;
			infoBox = new NpcMaxHitInfoBox(data, spriteManager.getSprite(RED_HITSPLAT, 0), this, config);
			infoBoxManager.addInfoBox(infoBox);
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		Actor actor = event.getActor();
		Hitsplat hitsplat = event.getHitsplat();

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

		// Run wiki request in background
		executor.submit(() -> {
			Optional<NpcMaxHitData> data = wikiService.getMaxHitData(npcName, npcId);
			data.ifPresent(npcData -> {
					clientThread.invoke(() -> {
						overlay.updateNpcData(npcData);
						updateInfoBox(npcData);
					});
				}
			);
		});
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		long currentTime = System.currentTimeMillis();
		int timeoutMs = config.timeout() * 1000;
		if (currentTime - lastHitsplatTime >= timeoutMs)
		{
			if (overlay.getCurrentNpcName() != null)
			{
				clientThread.invoke(() -> {
					overlay.updateNpcData(null);
					removeInfoBox();
				});
			}
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
