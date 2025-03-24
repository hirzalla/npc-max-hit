package com.npcmaxhit;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class NpcMaxHitPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(NpcMaxHitPlugin.class);
		RuneLite.main(args);
	}
}