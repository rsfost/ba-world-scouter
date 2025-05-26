package rsfost.ba_world_scouter;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.*;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
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
	@Inject
	private Client client;

	@Inject
	private BaWorldScouterConfig config;

	@Inject
	private InstanceInfoService instanceInfoService;

	@Inject
	private EventBus eventBus;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ScheduledExecutorService executorService;

	private NavigationButton navButton;
	private WorldInfoPanel panel;

	private boolean shouldCheckLocation;
	private int lastRegionId;
	private ScheduledFuture<?> fetchWorldsFuture;

	@Override
	protected void startUp() throws Exception
	{
		BufferedImage icon = ImageUtil.loadImageResource(BaWorldScouterPlugin.class, "icon.png");
		panel = new WorldInfoPanel(this);
		navButton = NavigationButton.builder()
			.tooltip("BA World Scouter")
			.priority(4)
			.panel(panel)
			.icon(icon)
			.build();
		clientToolbar.addNavigation(navButton);
		fetchWorldsFuture = executorService.scheduleAtFixedRate(this::updateWorlds, 10, 30, TimeUnit.SECONDS);
		eventBus.register(instanceInfoService);
	}

	@Override
	protected void shutDown() throws Exception
	{
		fetchWorldsFuture.cancel(true);
		clientToolbar.removeNavigation(navButton);
		eventBus.unregister(instanceInfoService);
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
		instanceInfoService.putInstanceInfo(wp, templateRegionId);
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

	private void updateWorlds()
	{
		instanceInfoService.getInstanceInfos(
			instanceInfos -> {
				SwingUtilities.invokeLater(() -> {
					panel.populate(instanceInfos);
				});
			},
			error -> {
				log.error("Unable to update instance information", error);
			}
		);
	}
}
