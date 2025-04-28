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
		position = 1,
		closedByDefault = true
	)
	String overlaySection = "overlay";

	@ConfigSection(
		name = "Infobox",
		description = "Infobox appearance settings",
		position = 2,
		closedByDefault = true
	)
	String infoboxSection = "infobox";

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
		keyName = "showInfobox",
		name = "Show Infobox",
		description = "Show max hit information in an infobox",
		section = generalSection,
		position = 1
	)
	default boolean showInfobox()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMaxHitInMenus",
		name = "Show Max Hit in Menus",
		description = "Show the max hit information in right-click menus",
		section = generalSection,
		position = 2
	)
	default boolean showMaxHitInMenus()
	{
		return false;
	}

	@ConfigItem(
		keyName = "timeout",
		name = "Display Timeout",
		description = "Time in seconds before the displays are hidden after the player stops attacking",
		section = generalSection,
		position = 3
	)
	default int timeout()
	{
		return 6;
	}

	@ConfigItem(
		keyName = "combatLevelThreshold",
		name = "Level Threshold",
		description = "Will not display max hits for NPCs below this combat level (0 to disable)",
		section = generalSection,
		position = 4
	)
	default int combatLevelThreshold()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "filteredNpcIds",
		name = "Filtered NPC IDs",
		description = "Will not display max hits for these NPCs (comma-separated list of IDs, e.g. 3029,12191,2042)",
		section = generalSection,
		position = 5
	)
	default String filteredNpcIds()
	{
		return "";
	}

	@ConfigItem(
		keyName = "compactMaxHits",
		name = "Compact Max Hits",
		description = "Only show the highest max hit value when multiple attack styles are present",
		section = overlaySection,
		position = 0
	)
	default boolean compactMaxHits()
	{
		return false;
	}

	@ConfigItem(
		keyName = "compactNames",
		name = "Compact NPC Names",
		description = "Show simplified NPC names without version/variant information, if available (e.g. 'Vorkath' instead of 'Vorkath (Post-quest)') <br>Note: This can make it harder to identify the version if there are multiple variants of the same NPC ID (e.g. DT2 bosses, Araxxor, etc.)",
		section = overlaySection,
		position = 1
	)
	default boolean compactNames()
	{
		return false;
	}

	@ConfigItem(
		keyName = "fontFamily",
		name = "Font",
		description = "Font to use in the overlay",
		section = overlaySection,
		position = 92
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
		position = 93
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
		keyName = "overlayFontSize",
		name = "Font Size",
		description = "Size of the text in the overlay",
		section = overlaySection,
		position = 94
	)
	default int overlayFontSize()
	{
		return 16;
	}

	@ConfigItem(
		keyName = "overlayTitleColor",
		name = "Title Color",
		description = "Color of the title in the overlay (NPC name)",
		section = overlaySection,
		position = 95
	)
	default Color overlayTitleColor()
	{
		return Color.YELLOW;
	}

	@ConfigItem(
		keyName = "overlayTextColor",
		name = "Text Color",
		description = "Color of the text in the overlay",
		section = overlaySection,
		position = 96
	)
	default Color overlayTextColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "overlayValueColor",
		name = "Max Hit Color",
		description = "Color of the max hit values in the overlay",
		section = overlaySection,
		position = 97
	)
	default Color overlayValueColor()
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

	@ConfigItem(
		keyName = "infoboxTextColor",
		name = "Max Hit Color",
		description = "Color of the highest max hit value in the infobox",
		section = infoboxSection,
		position = 96
	)
	default Color infoboxTextColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "infoboxTooltipTitleColor",
		name = "Tooltip Title Color",
		description = "Color of the title in the infobox tooltip (NPC name)",
		section = infoboxSection,
		position = 97
	)
	default Color infoboxTooltipTitleColor()
	{
		return Color.YELLOW;
	}

	@ConfigItem(
		keyName = "infoboxTooltipTextColor",
		name = "Tooltip Text Color",
		description = "Color of the text in the infobox tooltip (NPC name, attack style/type)",
		section = infoboxSection,
		position = 98
	)
	default Color infoboxTooltipTextColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "infoboxTooltipValueColor",
		name = "Tooltip Max Hits Color",
		description = "Color of the max hit values in the infobox tooltip",
		section = infoboxSection,
		position = 99
	)
	default Color infoboxTooltipValueColor()
	{
		return Color.YELLOW;
	}

}
