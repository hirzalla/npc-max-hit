package com.npcmaxhit;

import java.util.Map;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;

import java.awt.*;

public class NpcMaxHitInfoBox extends InfoBox
{
	private static final int RED_HITSPLAT = 1359;
	private final NpcMaxHitData data;
	private final NpcMaxHitConfig config;

	public NpcMaxHitInfoBox(NpcMaxHitData data, NpcMaxHitConfig config, SpriteManager spriteManager, Plugin plugin)
	{
		super(spriteManager.getSprite(RED_HITSPLAT, 0), plugin);
		this.data = data;
		this.config = config;
		setPriority(InfoBoxPriority.HIGH);
	}

	@Override
	public String getText()
	{
		return String.valueOf(data.getHighestMaxHit());
	}

	@Override
	public Color getTextColor()
	{
		return config.infoboxTextColor();
	}

	@Override
	public String getTooltip()
	{
		StringBuilder tooltip = new StringBuilder();
		tooltip.append(ColorUtil.wrapWithColorTag(data.getDisplayName(), config.infoboxTooltipTextColor()));

		for (Map.Entry<String, Integer> entry : data.getMaxHits().entrySet())
		{
			tooltip.append("</br>")
				.append(entry.getKey())
				.append(": ")
				.append(ColorUtil.wrapWithColorTag(String.valueOf(entry.getValue()), config.infoboxTooltipValueColor()));
		}

		return tooltip.toString();
	}

	@Override
	public boolean render()
	{
		return config.showInfobox();
	}
}

