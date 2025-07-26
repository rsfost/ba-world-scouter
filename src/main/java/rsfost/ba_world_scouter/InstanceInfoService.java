package rsfost.ba_world_scouter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.WorldsFetch;
import net.runelite.client.game.WorldService;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import okhttp3.*;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
class InstanceInfoService
{
    private static final String API_BASE = "https://bascout.jfost.com/api/v1";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final Client client;
    private final ClientThread clientThread;
    private final WorldService worldService;
    private final OkHttpClient httpClient;
    private final Gson gson;

    private volatile Map<Integer, World> allWorlds;
    private volatile EnumComposition worldLocations;

    @Inject
    public InstanceInfoService(
        Client client, ClientThread clientThread, ScheduledExecutorService executorService,
        WorldService worldService, OkHttpClient httpClient, Gson gson)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.worldService = worldService;
        this.httpClient = httpClient;
        this.gson = gson;

        // Get initial world list
        clientThread.invokeLater(() -> {
            if (client.getGameState().getState() < GameState.LOGIN_SCREEN.getState())
            {
                return false;
            }
            executorService.execute(() -> {
                if (!this.updateWorlds())
                {
                    log.warn("Failed to get initial world list.");
                }
            });
            return true;
        });
    }

    public void putInstanceInfo(WorldPoint wp, int regionId)
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

    public void getInstanceInfos(Consumer<InstanceInfo[]> onSuccess, Consumer<Throwable> onError)
    {
        Request request = new Request.Builder()
            .url(API_BASE + "/worlds")
            .get()
            .build();
        Call call = httpClient.newCall(request);
        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                String message = "Network error fetching world info";
                log.error("Network error fetching world info", e);
                onError.accept(new RuntimeException(message));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                try (ResponseBody respBody = response.body())
                {
                    if (!response.isSuccessful())
                    {
                        String message = String.format("Unable to fetch world info (http %d)", response.code());
                        log.error(message);
                        onError.accept(new RuntimeException(message));
                        return;
                    }
                    if (respBody == null)
                    {
                        String message = "Empty response from world fetch";
                        log.error(message);
                        onError.accept(new RuntimeException(message));
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
                    onSuccess.accept(worlds);
                }
            }
        });
    }

    @Subscribe
    public void onWorldsFetch(WorldsFetch event)
    {
        updateWorlds();
    }

    private boolean updateWorlds()
    {
        if (client.getGameState().ordinal() < GameState.LOGIN_SCREEN.ordinal())
        {
            return false;
        }

        WorldResult worldResult = worldService.getWorlds();
        if (worldResult == null)
        {
            return false;
        }

        List<World> worlds = worldResult.getWorlds();
        this.allWorlds = worlds.stream().collect(Collectors.toMap(World::getId, w -> w));
        clientThread.invokeLater(() -> {
            this.worldLocations = client.getEnum(EnumID.WORLD_LOCATIONS);
        });

        return true;
    }
}
