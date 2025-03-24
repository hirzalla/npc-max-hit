package com.npcmaxhit;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
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
	private NpcMaxHitData currentNpc;

	@Inject
	public NpcMaxHitOverlay(NpcMaxHitConfig config)
	{
		this.config = config;
		panelComponent.setPreferredSize(new Dimension(150, 0));
	}

	public void updateNpcData(NpcMaxHitData data)
	{
		this.currentNpc = data;
	}

	// get current npc name
	public String getCurrentNpcName()
	{
		return currentNpc != null ? currentNpc.getNpcName() : null;
	}

	// get current npc ID
	public int getCurrentNpcId()
	{
		return currentNpc != null ? currentNpc.getNpcId() : -1;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (currentNpc == null || !config.showOverlay())
		{
			return null;
		}

		panelComponent.getChildren().clear();
		setPosition(OverlayPosition.TOP_LEFT);
		panelComponent.setBackgroundColor(config.overlayBackgroundColor());

		// Create font with configured family, style and size
		graphics.setFont(new Font(
			config.fontFamily().getFamily(),
			config.fontStyle().getStyle(),
			config.fontSize()
		));

		// Add title with NPC ID
		String[] npcNameAndVersion = currentNpc.getNpcName().replaceAll("_", " ").split("#");
		String npcName = npcNameAndVersion[0];
		// for testing
		String npcVersion = npcNameAndVersion.length > 1 ? npcNameAndVersion[1] : "";
		int npcId = currentNpc.getNpcId();
		
		panelComponent.getChildren().add(TitleComponent.builder()
			.text(npcName)
			.color(config.titleColor())
			.build());

		// Add max hits based on compact mode
		if (config.compact())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Max Hit")
				.right(Integer.toString(currentNpc.getHighestMaxHit()))
				.leftColor(config.textColor())
				.rightColor(config.textColor())
				.build());
		}
		else
		{
			// Show all combat style variations
			currentNpc.getMaxHits().forEach((style, hit) ->
				panelComponent.getChildren().add(LineComponent.builder()
					.left(style)
					.right(Integer.toString(hit))
					.leftColor(config.textColor())
					.rightColor(config.textColor())
					.build())
			);
		}

		return panelComponent.render(graphics);
	}
}
