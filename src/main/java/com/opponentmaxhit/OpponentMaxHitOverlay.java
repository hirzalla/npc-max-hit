package com.opponentmaxhit;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.ui.overlay.components.LineComponent;

import java.awt.*;
import javax.inject.Inject;

public class OpponentMaxHitOverlay extends Overlay {
    private final PanelComponent panelComponent = new PanelComponent();
    private final OpponentMaxHitConfig config;
    private OpponentMaxHitData currentMonster;

    @Inject
    public OpponentMaxHitOverlay(OpponentMaxHitConfig config) {
        this.config = config;
        panelComponent.setPreferredSize(new Dimension(150, 0));
    }

    public void updateMonsterData(OpponentMaxHitData data) {
        this.currentMonster = data;
    }

    // get current monster name
    public String getCurrentMonsterName() {
        return currentMonster != null ? currentMonster.getMonsterName() : null;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (currentMonster == null || !config.showOverlay()) {
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
        panelComponent.getChildren().add(TitleComponent.builder()
                .text(currentMonster.getMonsterName().split("#")[0].replaceAll("_", " ") + " #" + currentMonster.getNpcId())
                .color(config.titleColor())
                .build());

        // Add max hits based on compact mode
        if (config.compact()) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Max Hit")
                    .right(Integer.toString(currentMonster.getHighestMaxHit()))
                    .leftColor(config.textColor())
                    .rightColor(config.textColor())
                    .build());
        } else {
            // Show all combat style variations
            currentMonster.getMaxHits().forEach((style, hit) ->
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
