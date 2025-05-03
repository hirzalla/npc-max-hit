package com.npcmaxhit;

import com.google.inject.Provides;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
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
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.events.ConfigChanged;
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
	description = "Displays the max hits of NPCs",
	tags = {"maxhit", "monster", "boss", "npc", "opponent", "hit", "damage", "overlay", "combat", "pvm", "pve", "max hit", "infobox"}
)
public class NpcMaxHitPlugin extends Plugin
{
	private Actor player;
	private long lastDisplayTime = 0;
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
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();

		if (!config.showInMenu() || shouldFilterNpc(npc))
		{
			return;
		}
		fetchMaxHitData(npc.getId());
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (event.getTarget() == null || event.getTarget().isEmpty())
		{
			return;
		}

		if (!config.showInMenu())
		{
			return;
		}

		MenuEntry menuEntry = event.getMenuEntry();
		NPC npc = menuEntry.getNpc();
		if (shouldFilterNpc(npc))
		{
			return;
		}

		boolean isAttackOption = event.getType() == MenuAction.NPC_SECOND_OPTION.getId() && event.getOption().equals("Attack");
		boolean isExamineOption = event.getType() == MenuAction.EXAMINE_NPC.getId();

		List<NpcMaxHitData> npcMaxHitData = wikiService.getCachedMaxHitData(npc.getId());
		if (npcMaxHitData.isEmpty())
		{
			return;
		}

		// add max hit to enabled menu options
		if (config.showInMenu() &&
			((isAttackOption && config.showOnAttackOption()) ||
				(isExamineOption && config.showOnExamineOption())))
		{
			int maxHit = npcMaxHitData.get(0).getHighestMaxHit();
			// skip if max hit is not computed i.e. -1
			if (maxHit < 0)
			{
				return;
			}
			String maxHitText = config.menuDisplayStyle() == NpcMaxHitConfig.MenuDisplayStyle.NUMBER_ONLY ?
				String.format(" (%d)", maxHit) :
				String.format(" (Max Hit: %d)", maxHit);

			String target = event.getTarget();
			String colorTag = "<col=" + Integer.toHexString(config.menuMaxHitColor().getRGB() & 0xFFFFFF) + ">";
			String newTarget = target + colorTag + maxHitText + "</col>";
			menuEntry.setTarget(newTarget);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!config.displayMaxHitOnExamine())
		{
			return;
		}

		if (event.getMenuAction() != MenuAction.EXAMINE_NPC)
		{
			return;
		}

		NPC npc = event.getMenuEntry().getNpc();
		if (shouldFilterNpc(npc))
		{
			return;
		}

		fetchMaxHitData(npc.getId()).thenAccept(this::displayMaxHitData);

	}

	private void displayMaxHitData(List<NpcMaxHitData> dataList)
	{
		if (dataList.isEmpty())
		{
			return;
		}

		lastDisplayTime = System.currentTimeMillis();

		clientThread.invoke(() -> {
			overlay.updateNpcDataList(dataList);
			updateInfoBox(dataList);
		});
	}

	private CompletableFuture<List<NpcMaxHitData>> fetchMaxHitData(int npcId)
	{
		return wikiService.getMaxHitData(npcId);
	}

	private boolean shouldFilterNpc(NPC npc)
	{
		if (npc == null || npc.getCombatLevel() <= 0 || !npc.getComposition().isInteractible())
		{
			return true;
		}

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

		// dont attempt to re-fetch data if the same npc is being attacked and overlay includes the npc
		if (player.getInteracting() == npc && overlay.getCurrentNpcList().stream().anyMatch(data -> data.getNpcId() == npc.getId()))
		{
			// Update last hitsplat time
			lastDisplayTime = System.currentTimeMillis();
			return;
		}

		fetchMaxHitData(npc.getId()).thenAccept(this::displayMaxHitData);
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		long currentTime = System.currentTimeMillis();
		int timeoutMs = config.timeout() * 1000;
		if (currentTime - lastDisplayTime >= timeoutMs)
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

	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		String command = event.getCommand().toLowerCase();
		// ::npcmaxhit 3029
		if (!command.equals("npcmaxhit"))
		{
			return;
		}

		String[] args = event.getArguments();
		if (args.length == 0)
		{
			return;
		}

		if (args[0].equals("clear"))
		{
			wikiService.clearCache();
			return;
		}

		try
		{
			int npcId = Integer.parseInt(args[0]);

			fetchMaxHitData(npcId).thenAccept(this::displayMaxHitData);
		}
		catch (NumberFormatException e)
		{
			log.warn("Invalid command arguments", e);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals(NpcMaxHitConfig.GROUP))
		{
			return;
		}

		if (event.getKey().equals("showInMenu") && config.showInMenu())
		{
			clientThread.invoke(() -> {
				for (NPC npc : client.getTopLevelWorldView().npcs())
				{
					if (!shouldFilterNpc(npc))
					{
						fetchMaxHitData(npc.getId());
					}
				}
			});

		}
	}

	@Provides
	NpcMaxHitConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(NpcMaxHitConfig.class);
	}
}
