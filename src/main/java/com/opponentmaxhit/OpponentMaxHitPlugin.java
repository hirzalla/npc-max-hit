package com.opponentmaxhit;

import com.google.inject.Inject;
import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
        name = "Opponent Max Hit",
        description = "Displays the max hit for non-player opponents",
        tags = {"maxhit", "monster", "npc", "opponent", "hit", "damage", "overlay", "combat", "pvm", "pve", "max hit"}
)
public class OpponentMaxHitPlugin extends Plugin
{
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
        wikiService.getMaxHitData("Zulrah").ifPresent(overlay::updateMonsterData);
        wikiService.getMaxHitData("Araxxor").ifPresent(overlay::updateMonsterData);
        wikiService.getMaxHitData("Phantom Muspah").ifPresent(overlay::updateMonsterData);

    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
    }

    @Provides
    OpponentMaxHitConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(OpponentMaxHitConfig.class);
    }
}
