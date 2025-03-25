package com.npcmaxhit;

import java.awt.Color;
import java.awt.Font;
import lombok.Getter;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("npcmaxhit")
public interface NpcMaxHitConfig extends Config
{
	@ConfigSection(
		name = "General",
		description = "General plugin settings",
		position = 0
	)
	String generalSection = "general";

	@ConfigSection(
		name = "Overlay",
		description = "Overlay appearance settings",
		position = 1
	)
	String overlaySection = "overlay";

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show Overlay",
		description = "Show the max hit overlay",
		section = generalSection,
		position = 0
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "compact",
		name = "Compact Mode",
		description = "Only show the highest max hit value when multiple attack styles are present",
		section = overlaySection,
		position = 0
	)
	default boolean compact()
	{
		return false;
	}

	@ConfigItem(
		keyName = "inactivityTimeout",
		name = "Inactivity Timeout",
		description = "Time in seconds before the overlay is hidden after the player stops attacking",
		section = overlaySection,
		position = 1
	)
	default int inactivityTimeout()
	{
		return 6;
	}

	@ConfigItem(
		keyName = "fontFamily",
		name = "Font",
		description = "Font to use in the overlay",
		section = overlaySection,
		position = 93
	)
	default FontFamily fontFamily()
	{
		return FontFamily.REGULAR;
	}

	@Getter
	enum FontFamily
	{
		REGULAR("RuneScape"),
		PLAIN("RuneScape Plain"),
		BOLD("RuneScape Bold"),
		SMALL("RuneScape Small");

		private final String family;

		FontFamily(String family)
		{
			this.family = family;
		}

	}

	@ConfigItem(
		keyName = "fontStyle",
		name = "Font Style",
		description = "Style of the font in the overlay",
		section = overlaySection,
		position = 94
	)
	default FontStyle fontStyle()
	{
		return FontStyle.PLAIN;
	}

	@Getter
	enum FontStyle
	{
		PLAIN(Font.PLAIN),
		BOLD(Font.BOLD),
		ITALIC(Font.ITALIC),
		BOLD_ITALIC(Font.BOLD + Font.ITALIC);

		private final int style;

		FontStyle(int style)
		{
			this.style = style;
		}

	}

	@ConfigItem(
		keyName = "fontSize",
		name = "Font Size",
		description = "Size of the text in the overlay",
		section = overlaySection,
		position = 95
	)
	default int fontSize()
	{
		return 16;
	}

	@ConfigItem(
		keyName = "titleColor",
		name = "Title Color",
		description = "Color of the NPC name in the overlay",
		section = overlaySection,
		position = 96
	)
	default Color titleColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "textColor",
		name = "Text Color",
		description = "Color of the max hit values",
		section = overlaySection,
		position = 97
	)
	default Color textColor()
	{
		return Color.YELLOW;
	}

	@Alpha
	@ConfigItem(
		keyName = "overlayBackgroundColor",
		name = "Background Color",
		description = "Color of the overlay background",
		section = overlaySection,
		position = 98
	)
	default Color overlayBackgroundColor()
	{
		return new Color(70, 61, 50, 156);
	}

}
