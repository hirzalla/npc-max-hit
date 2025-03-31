package com.npcmaxhit;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.ui.overlay.components.LineComponent;

public class NpcMaxHitOverlay extends OverlayPanel
{
	private static final int MAX_ENTRIES = 5;
	private static final int PANEL_WIDTH_OFFSET = 10;

	private final NpcMaxHitConfig config;

	@Getter
	private List<NpcMaxHitData> currentNpcList = new ArrayList<>();

	@Inject
	public NpcMaxHitOverlay(NpcMaxHitConfig config)
	{
		this.config = config;
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		setResizable(true);
	}

	public void updateNpcDataList(List<NpcMaxHitData> dataList)
	{
		this.currentNpcList = dataList;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (currentNpcList.isEmpty() || !config.showOverlay())
		{
			return null;
		}

		panelComponent.setBackgroundColor(config.overlayBackgroundColor());
		panelComponent.setPreferredSize(new Dimension(config.overlayFontSize() * PANEL_WIDTH_OFFSET, 0));

		// Create font with configured family, style and size
		graphics.setFont(new Font(
			config.fontFamily().getFamily(),
			config.fontStyle().getStyle(),
			config.overlayFontSize()
		));

		// Only show up to MAX_ENTRIES NPCs
		List<NpcMaxHitData> displayList = currentNpcList.size() > MAX_ENTRIES ?
			currentNpcList.subList(0, MAX_ENTRIES) : currentNpcList;

		for (NpcMaxHitData data : displayList)
		{
			panelComponent.getChildren().add(TitleComponent.builder()
				.text(data.getDisplayName())
				.color(config.overlayTitleColor())
				.build());

			if (config.compact())
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Max Hit")
					.right(Integer.toString(data.getHighestMaxHit()))
					.leftColor(config.overlayTextColor())
					.rightColor(config.overlayValueColor())
					.build());
			}
			else
			{
				data.getMaxHits().forEach((style, hit) ->
					panelComponent.getChildren().add(LineComponent.builder()
						.left(style)
						.right(Integer.toString(hit))
						.leftColor(config.overlayTextColor())
						.rightColor(config.overlayValueColor())
						.build())
				);
			}
		}

		return super.render(graphics);
	}
}
