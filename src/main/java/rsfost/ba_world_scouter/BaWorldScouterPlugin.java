package rsfost.ba_world_scouter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import javax.inject.Inject;
import javax.inject.Named;

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
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@PluginDescriptor(
	name = "BA World Scouter",
	description = "Helps scout for optimal worlds for quick-starting in Barbarian Assault"
)
public class BaWorldScouterPlugin extends Plugin
{
	private static final String PROD_API_BASE = "https://todo";
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	@Inject
	private Client client;

	@Inject
	private BaWorldScouterConfig config;

	@Inject
	private ScheduledExecutorService executorService;

	@Inject
	private OkHttpClient httpClient;

	@Inject
	private Gson gson;

	@Inject @Named("developerMode")
	private boolean developerMode;

	private String apiBaseUrl;
	private boolean shouldCheckLocation;
	private int lastRegionId;
	private ScheduledFuture<?> fetchWorldsFuture;
	private World[] worlds;

	@Override
	protected void startUp() throws Exception
	{
		if (developerMode)
		{
			String apiUrl = System.getenv("BASCOUT_API_URL");
			this.apiBaseUrl = Objects.requireNonNullElse(apiUrl, PROD_API_BASE);
		}
		else
		{
			this.apiBaseUrl = PROD_API_BASE;
		}
		fetchWorldsFuture = executorService.scheduleAtFixedRate(this::fetchWorlds, 10, 30, TimeUnit.SECONDS);
	}

	@Override
	protected void shutDown() throws Exception
	{
		fetchWorldsFuture.cancel(true);
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

	void updateWorld(WorldPoint wp, int regionId)
	{
		final int world = client.getWorld();

		JsonObject data = new JsonObject();
		data.addProperty("x", wp.getX());
		data.addProperty("y", wp.getY());
		data.addProperty("region", regionId);
		Request request = new Request.Builder()
			.url(apiBaseUrl + "/world/" + world)
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
				try (response)
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
			}
		});
	}

	void fetchWorlds()
	{
		Request request = new Request.Builder()
			.url(apiBaseUrl + "/worlds")
			.get()
			.build();
		Call call = httpClient.newCall(request);
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Network error fetching world info", e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody respBody = response.body())
				{
					if (response.code() == 200)
					{
						if (respBody == null)
						{
							log.error("empty response from world fetch");
							return;
						}
						String json = respBody.string();
						respBody.close();
						World[] worlds = gson.fromJson(json, World[].class);
						BaWorldScouterPlugin.this.worlds = worlds;
						log.debug("Fetched latest world info");
					}
					else
					{
						log.error("Unable to fetch world info (http {})", response.code());
					}
				}
			}
		});
	}
}
