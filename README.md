# NPC Max Hit
[![Active Installs](http://img.shields.io/endpoint?url=https://api.runelite.net/pluginhub/shields/installs/plugin/npc-max-hit)](https://runelite.net/plugin-hub/show/npc-max-hit)
[![Plugin Rank](http://img.shields.io/endpoint?url=https://api.runelite.net/pluginhub/shields/rank/plugin/npc-max-hit)](https://runelite.net/plugin-hub/show/npc-max-hit)

A RuneLite plugin that displays the max hit values for NPCs you are currently fighting, sourced from the OSRS Wiki.

## Screenshots

| Feature                  | Screenshot 1                                            | Screenshot 2                                            |
|--------------------------|---------------------------------------------------------|---------------------------------------------------------|
| Multiple Variants/Phases | ![Multiple Forms Example 1](screenshots/scurrius_1.png) | ![Multiple Forms Example 2](screenshots/scurrius_2.png) |
| Overlay                  | ![Overlay Example 1](screenshots/overlay_1.png)         | ![Overlay Example 2](screenshots/overlay_2.png)         |
| Infobox                  | ![Infobox Example 1](screenshots/infobox_1.png)         | ![Infobox Example 2](screenshots/infobox_2.png)         |

## Features

- Shows max hit values for your current opponent (NPCs, bosses, etc.)
- Fetches data directly from the OSRS Wiki
- Supports multiple monster variants/phases (e.g. Zulrah, Phantom Muspah)
- Display options:
    - Detailed overlay with all max hit information (e.g. melee, ranged, magic, specials)
    - Compact infobox showing highest max hit with a detailed tooltip on hover
- Auto-hides after combat inactivity

## Notes

The max hit values displayed:

- Don't account for protection prayers or other damage reduction effects
- Are parsed from OSRS Wiki and may not always be accurate
- May be outdated if the Wiki hasn't been updated after game updates

Please verify critical information directly on the Wiki.

## Configurations

- Toggle and position both the overlay and infobox displays
- Customize fonts, colors, and transparency
- Adjust combat timeout duration
- Switch between detailed and compact display modes

## Attribution

Data is sourced from the [Old School RuneScape Wiki](https://oldschool.runescape.wiki/).

