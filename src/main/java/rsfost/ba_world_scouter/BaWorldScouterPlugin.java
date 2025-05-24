package rsfost.ba_world_scouter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import okhttp3.*;

import java.io.IOException;

@Slf4j
@PluginDescriptor(
	name = "BA World Scouter"
)
public class BaWorldScouterPlugin extends Plugin
{
	private static final String API_BASE = "http://localhost:8000";
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	@Inject
	private Client client;

	@Inject
	private BaWorldScouterConfig config;

	@Inject
	private OkHttpClient httpClient;

	@Inject
	private Gson gson;

	private boolean shouldCheckLocation;
	private int lastRegionId;

	@Override
	protected void startUp() throws Exception
	{

	}

	@Override
	protected void shutDown() throws Exception
	{

	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		if (!shouldCheckLocation)
		{
			return;
		}

		final Player player = client.getLocalPlayer();
		final WorldView worldView = player.getWorldView();
		final WorldPoint wp = player.getWorldLocation();
		final int currentRegionId = wp.getRegionID();

		if (!worldView.isInstance() || currentRegionId == lastRegionId)
		{
			return;
		}

		final int templateRegionId = WorldPoint.fromLocalInstance(client,
				client.getLocalPlayer().getLocalLocation()).getRegionID();
		log.debug("y = {}, region id = {}", wp.getY(), templateRegionId);
		updateWorld(wp, templateRegionId);
		shouldCheckLocation = false;
		lastRegionId = currentRegionId;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOADING)
		{
			shouldCheckLocation = true;
		}
	}

	@Provides
	BaWorldScouterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BaWorldScouterConfig.class);
	}

	private void updateWorld(WorldPoint wp, int regionId)
	{
		final int world = client.getWorld();

		JsonObject data = new JsonObject();
		data.addProperty("x", wp.getX());
		data.addProperty("y", wp.getY());
		data.addProperty("region", regionId);
		Request request = new Request.Builder()
			.url(API_BASE + "/world/" + world)
			.put(RequestBody.create(JSON, gson.toJson(data)))
			.build();
		Call call = httpClient.newCall(request);
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Network error submitting world info", e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				if (response.code() == 200)
				{
					log.debug("Updated world info");
				}
				else
				{
					log.error("Unable to update world info (http {})", response.code());
				}
			}
		});
	}
}
