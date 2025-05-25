package rsfost.ba_world_scouter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.*;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.WorldsFetch;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import okhttp3.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
	name = "BA World Scouter",
	description = "Helps scout for optimal worlds for quick-starting in Barbarian Assault"
)
public class BaWorldScouterPlugin extends Plugin
{
	private static final String PROD_API_BASE = "https://gkcgbnux6ylar7fzz6aiztnk3a0vzoct.lambda-url.us-east-1.on.aws";
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	@Inject
	private Client client;

	@Inject
	private BaWorldScouterConfig config;

	@Inject
	private WorldService worldService;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ScheduledExecutorService executorService;

	@Inject
	private OkHttpClient httpClient;

	@Inject
	private Gson gson;

	@Inject @Named("developerMode")
	private boolean developerMode;

	private NavigationButton navButton;
	private WorldInfoPanel panel;

	private String apiBaseUrl;
	private boolean shouldCheckLocation;
	private int lastRegionId;
	private ScheduledFuture<?> fetchWorldsFuture;
	private Map<Integer, World> allWorlds;
	private EnumComposition worldLocations;

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

		BufferedImage icon = ImageUtil.loadImageResource(BaWorldScouterPlugin.class, "icon.png");
		panel = new WorldInfoPanel(this);
		navButton = NavigationButton.builder()
			.tooltip("BP World Scouter")
			.panel(panel)
			.icon(icon)
			.build();
		clientToolbar.addNavigation(navButton);

		fetchWorldsFuture = executorService.scheduleAtFixedRate(this::fetchInstanceInfo, 10, 30, TimeUnit.SECONDS);
		clientThread.invokeLater(() -> {
			this.worldLocations = client.getEnum(EnumID.WORLD_LOCATIONS);
			updateWorlds();
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		fetchWorldsFuture.cancel(true);
		clientToolbar.removeNavigation(navButton);
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
		updateInstanceInfo(wp, templateRegionId);
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

	@Subscribe
	public void onWorldsFetch(WorldsFetch event)
	{
		updateWorlds();
	}

	private void updateWorlds()
	{
		WorldResult worldResult = worldService.getWorlds();
		if (worldResult == null)
		{
			return;
		}
		List<World> worlds = worldResult.getWorlds();
		this.allWorlds = worlds.stream().collect(Collectors.toMap(World::getId, w -> w));
		this.worldLocations = client.getEnum(EnumID.WORLD_LOCATIONS);
	}

	@Provides
	BaWorldScouterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BaWorldScouterConfig.class);
	}

	void updateInstanceInfo(WorldPoint wp, int regionId)
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

	void fetchInstanceInfo()
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
					if (!response.isSuccessful())
					{
						log.error("Unable to fetch world info (http {})", response.code());
						return;
					}
					if (respBody == null)
					{
						log.error("empty response from world fetch");
						return;
					}

					String json = respBody.string();
					InstanceInfo[] worlds = gson.fromJson(json, InstanceInfo[].class);
					Arrays.stream(worlds).forEach(w -> {
						if (allWorlds != null)
						{
							w.setWorld(allWorlds.get(w.getWorldId()));
						}
						if (worldLocations != null)
						{
							w.setWorldLocation(worldLocations.getIntValue(w.getWorldId()));
						}
					});
					log.debug("Fetched latest world info");
					SwingUtilities.invokeLater(() -> {
						panel.populate(worlds);
					});
				}
			}
		});
	}
}
