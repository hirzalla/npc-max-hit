package com.npcmaxhit;

import com.google.inject.Provides;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.events.CommandExecuted;
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
	private ExecutorService executor;

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
		executor = Executors.newSingleThreadExecutor();
		overlay.updateNpcDataList(List.of());
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		shutdownExecutor();
		overlayManager.remove(overlay);
		overlay.updateNpcDataList(List.of());
		removeInfoBox();

	}

	private void shutdownExecutor()
	{
		if (executor != null)
		{
			executor.shutdown();
			try
			{
				if (!executor.awaitTermination(1, TimeUnit.SECONDS))
				{
					executor.shutdownNow();
				}
			}
			catch (InterruptedException e)
			{
				executor.shutdownNow();
				Thread.currentThread().interrupt();
			}
			executor = null;
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

	private void updateInfoBox(List<NpcMaxHitData> dataList)
	{
		removeInfoBox();
		if (!dataList.isEmpty())
		{
			infoBox = new NpcMaxHitInfoBox(dataList, config, spriteManager, this);
			infoBoxManager.addInfoBox(infoBox);
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
		int npcId = npc.getId();

		// Update last hitsplat time
		lastHitsplatTime = System.currentTimeMillis();

		// dont attempt to re-fetch data if the same npc is being attacked and overlay includes the npc
		if (player.getInteracting() == npc && overlay.getCurrentNpcList().stream().anyMatch(data -> data.getNpcId() == npcId))
		{
			return;
		}

		fetchAndDisplayMaxHitData(npcId);
	}

	public void fetchAndDisplayMaxHitData(int npcId)
	{
		executor.submit(() -> {
			List<NpcMaxHitData> dataList = wikiService.getMaxHitData(npcId);

			if (!dataList.isEmpty())
			{
				clientThread.invoke(() -> {
					overlay.updateNpcDataList(dataList);
					updateInfoBox(dataList);
//					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Max hit data for NPC ID: " + npcId + " displayed.", null);
				});
			}
		});
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		long currentTime = System.currentTimeMillis();
		int timeoutMs = config.timeout() * 1000;
		if (currentTime - lastHitsplatTime >= timeoutMs)
		{
			if (!overlay.getCurrentNpcList().isEmpty())
			{
				clientThread.invoke(() -> {
					overlay.updateNpcDataList(List.of());
					removeInfoBox();
				});
			}
		}
	}

//	@Subscribe
//	public void onCommandExecuted(CommandExecuted event)
//	{
//		String command = event.getCommand().toLowerCase();
//		if (!command.equals("maxhit"))
//		{
//			return;
//		}
//
//		String[] arg = event.getArguments();
//		int npcId = Integer.parseInt(arg[0]);
//
//		fetchAndDisplayMaxHitData(npcId);
//	}

	@Provides
	NpcMaxHitConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(NpcMaxHitConfig.class);
	}
}
