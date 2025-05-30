package com.npcmaxhit;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.List;
import net.runelite.client.ui.FontManager;
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
	private BufferedImage baseImage;

	public NpcMaxHitInfoBox(List<NpcMaxHitData> dataList, NpcMaxHitConfig config, SpriteManager spriteManager, Plugin plugin)
	{
		super(null, plugin);
		this.dataList = dataList;
		this.config = config;
		setPriority(InfoBoxPriority.HIGH);
		spriteManager.getSpriteAsync(RED_HITSPLAT, 0, img -> {
			baseImage = img;
			setImage(createInfoboxImage());
		});
	}

	private BufferedImage createInfoboxImage()
	{
		if (baseImage == null)
		{
			return null;
		}

		BufferedImage image = new BufferedImage(baseImage.getWidth(), baseImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();

		g.setFont(FontManager.getRunescapeSmallFont());
		g.drawImage(baseImage, 0, 0, null);

		final FontMetrics metrics = g.getFontMetrics();
		int maxHit = dataList.stream()
			.mapToInt(NpcMaxHitData::getHighestMaxHit)
			.max()
			.orElse(-1);

		String text = maxHit >= 0 ? String.valueOf(maxHit) : "?";

		int x = image.getWidth() / 2 - metrics.stringWidth(text) / 2;
		int y = image.getHeight() / 2 - metrics.getHeight() / 2 + metrics.getAscent() + 2;

		g.setColor(Color.BLACK);
		g.drawString(text, x + 1, y + 1);
		g.setColor(config.infoboxTextColor());
		g.drawString(text, x, y);

		g.dispose();
		return image;
	}

	@Override
	public String getText()
	{
		return null;
	}

	@Override
	public Color getTextColor()
	{
		return null;
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

			for (Map.Entry<String, String> entry : data.getMaxHits().entrySet())
			{
				tooltip.append("</br>")
					.append(ColorUtil.wrapWithColorTag(entry.getKey(), config.infoboxTooltipTextColor()))
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
