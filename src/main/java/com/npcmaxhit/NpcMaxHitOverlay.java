package com.npcmaxhit;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.ui.overlay.components.LineComponent;

import javax.inject.Inject;

public class NpcMaxHitOverlay extends Overlay
{
	private final PanelComponent panelComponent = new PanelComponent();
	private final NpcMaxHitConfig config;

	@Getter
	private List<NpcMaxHitData> currentNpcList = new ArrayList<>();

	@Inject
	public NpcMaxHitOverlay(NpcMaxHitConfig config)
	{
		this.config = config;
		setResizable(true);
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		panelComponent.setWrap(true);
		panelComponent.setPreferredSize(new Dimension(150, 0));
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

		panelComponent.getChildren().clear();
		panelComponent.setBackgroundColor(config.overlayBackgroundColor());

		// Create font with configured family, style and size
		graphics.setFont(new Font(
			config.fontFamily().getFamily(),
			config.fontStyle().getStyle(),
			config.overlayFontSize()
		));

		for (NpcMaxHitData data : currentNpcList)
		{
			// Add form/version title
			panelComponent.getChildren().add(TitleComponent.builder()
				.text(data.getDisplayName())
				.color(config.overlayTextColor())
				.build());

			// Add max hits based on compact mode
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

		return panelComponent.render(graphics);
	}
}
