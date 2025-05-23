package rsfost.ba_world_scouter;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BaWorldScouterPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BaWorldScouterPlugin.class);
		RuneLite.main(args);
	}
}