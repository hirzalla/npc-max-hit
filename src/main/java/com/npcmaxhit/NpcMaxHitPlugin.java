package com.npcmaxhit;

import com.google.inject.Provides;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
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

	//
	@Subscribe
	public void onNpcSpawned(NpcSpawned event) {
		NPC npc = event.getNpc();
		if (event.getActor().getCombatLevel() <= 0 || shouldFilterNpc(npc)) {
			return;
		}
		fetchAndDisplayMaxHitData(npc.getId(), false);
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event) {
		if (event.getType() != MenuAction.NPC_FIRST_OPTION.getId() &&
				event.getType() != MenuAction.NPC_SECOND_OPTION.getId() &&
				event.getType() != MenuAction.NPC_THIRD_OPTION.getId() &&
				event.getType() != MenuAction.NPC_FOURTH_OPTION.getId() &&
				event.getType() != MenuAction.NPC_FIFTH_OPTION.getId()) {
			return;
		}
		NPC npc = event.getMenuEntry().getNpc();
		if (npc == null || npc.getCombatLevel() <= 0 || shouldFilterNpc(npc)) {
			return;
		}

		for (MenuEntry menuEntry : client.getMenu().getMenuEntries()) {
			if (menuEntry.getOption().contains("Max Hit")) {
				return;
			}
		}

		List<NpcMaxHitData> npcMaxHitData = wikiService.getMaxHitData(npc.getId());
		if (npcMaxHitData.isEmpty()) return;
		String menuEntry = "Max Hit: " + npcMaxHitData.get(0).getHighestMaxHit();
		client.getMenu().createMenuEntry(client.getMenu().getMenuEntries().length)
				.setOption(menuEntry).onClick(
						(entry) -> {
							// Update last hitsplat time to make it timeout after 6s
							lastHitsplatTime = System.currentTimeMillis();
							displayMaxHitData(npcMaxHitData);
						}
				);
	}

	private void displayMaxHitData(List<NpcMaxHitData> dataList) {
		if (dataList.isEmpty()) {
			return;
		}

		clientThread.invoke(() -> {
			overlay.updateNpcDataList(dataList);
			updateInfoBox(dataList);
		});
	}


	private boolean shouldFilterNpc(NPC npc)
	{
		int threshold = config.combatLevelThreshold();
		if (threshold > 0 && npc.getCombatLevel() < threshold)
		{
			return true;
		}

		String filteredIds = config.filteredNpcIds().trim();
		if (!filteredIds.isEmpty())
		{
			int npcId = npc.getId();
			for (String idStr : filteredIds.split(","))
			{
				try
				{
					if (Integer.parseInt(idStr.trim()) == npcId)
					{
						return true;
					}
				}
				catch (NumberFormatException e)
				{
					log.warn("Invalid NPC ID in filter: {}", idStr, e);
				}
			}
		}

		return false;
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

		if (shouldFilterNpc(npc))
		{
			return;
		}

		// Update last hitsplat time
		lastHitsplatTime = System.currentTimeMillis();

		// dont attempt to re-fetch data if the same npc is being attacked and overlay includes the npc
		if (player.getInteracting() == npc && overlay.getCurrentNpcList().stream().anyMatch(data -> data.getNpcId() == npc.getId()))
		{
			return;
		}

		fetchAndDisplayMaxHitData(npc.getId(), true);
	}

	public void fetchAndDisplayMaxHitData(int npcId, boolean shouldDisplay)
	{
		executor.submit(() -> {
			List<NpcMaxHitData> dataList = wikiService.getMaxHitData(npcId);
			if (!dataList.isEmpty() && shouldDisplay)
			{
				displayMaxHitData(dataList);
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
