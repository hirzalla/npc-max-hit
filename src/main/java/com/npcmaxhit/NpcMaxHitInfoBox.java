package com.npcmaxhit;

import java.awt.Color;
import java.util.Map;
import java.util.List;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;

public class NpcMaxHitInfoBox extends InfoBox
{
	private final List<NpcMaxHitData> dataList;
	private static final int RED_HITSPLAT = 1359;
	private final NpcMaxHitConfig config;

	public NpcMaxHitInfoBox(List<NpcMaxHitData> dataList, NpcMaxHitConfig config, SpriteManager spriteManager, Plugin plugin)
	{
		super(spriteManager.getSprite(RED_HITSPLAT, 0), plugin);
		this.dataList = dataList;
		this.config = config;
		setPriority(InfoBoxPriority.HIGH);
	}

	@Override
	public String getText()
	{
		return String.valueOf(dataList.stream()
			.mapToInt(NpcMaxHitData::getHighestMaxHit)
			.max()
			.orElse(0));
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

		for (NpcMaxHitData data : dataList)
		{
			if (tooltip.length() > 0)
			{
				tooltip.append("</br>").append("</br>");
			}

			tooltip.append(ColorUtil.wrapWithColorTag(data.getDisplayName(), config.infoboxTooltipTitleColor()));

			for (Map.Entry<String, Integer> entry : data.getMaxHits().entrySet())
			{
				tooltip.append("</br>")
					.append(entry.getKey())
					.append(": ")
					.append(ColorUtil.wrapWithColorTag(String.valueOf(entry.getValue()), config.infoboxTooltipValueColor()));
			}
		}

		return tooltip.toString();
	}

	@Override
	public boolean render()
	{
		return config.showInfobox();
	}
}
