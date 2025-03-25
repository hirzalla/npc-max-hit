package com.npcmaxhit;

import java.util.Map;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;
import net.runelite.client.util.ColorUtil;

import java.awt.*;
import java.awt.image.BufferedImage;

public class NpcMaxHitInfoBox extends InfoBox
{
	private final NpcMaxHitData data;
	private final NpcMaxHitConfig config;

	public NpcMaxHitInfoBox(NpcMaxHitData data, BufferedImage image, NpcMaxHitPlugin plugin, NpcMaxHitConfig config)
	{
		super(image, plugin);
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
}
