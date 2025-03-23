package com.opponentmaxhit;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class OpponentMaxHitPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(OpponentMaxHitPlugin.class);
		RuneLite.main(args);
	}
}