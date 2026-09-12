package rsfost.world_locations;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.cache.definitions.EnumDefinition;
import net.runelite.cache.definitions.loaders.EnumLoader;
import net.runelite.cache.fs.ArchiveFiles;
import net.runelite.cache.fs.Container;
import net.runelite.cache.fs.FSFile;
import net.runelite.cache.index.ArchiveData;
import net.runelite.cache.index.FileData;
import net.runelite.cache.index.IndexData;

/**
 * Prints the game cache's world location enum as JSON on stdout:
 *
 * <pre>{"cache": 2695, "timestamp": "...", "locations": {"301": -42, ...}}</pre>
 *
 * This is the enum the RuneLite client exposes as EnumID.WORLD_LOCATIONS and
 * the plugin uses to split US worlds into east (-42) and west (-73) coast.
 *
 * Rather than downloading a whole ~190 MB cache, only the two groups needed
 * are fetched from the OpenRS2 archive: the config index, which lists the file
 * ids packed into the enum group, and the enum group itself.
 */
public class WorldLocations
{
	private static final String OPENRS2 = "https://archive.openrs2.org";

	private static final int INDEX_CONFIGS = 2;
	private static final int INDEX_META = 255;
	private static final int CONFIG_ENUM = 8;
	private static final int WORLD_LOCATIONS = 4992;

	private static final HttpClient http = HttpClient.newHttpClient();
	private static final Gson gson = new Gson();

	public static void main(String[] args) throws Exception
	{
		JsonObject cache = latestCache();
		int cacheId = cache.get("id").getAsInt();
		String base = OPENRS2 + "/caches/runescape/" + cacheId;

		IndexData index = new IndexData();
		index.load(decompress(get(base + "/archives/" + INDEX_META + "/groups/" + INDEX_CONFIGS + ".dat")));

		ArchiveFiles files = new ArchiveFiles();
		for (FileData file : findGroup(index, CONFIG_ENUM).getFiles())
		{
			files.addFile(new FSFile(file.getId()));
		}
		files.loadContents(decompress(get(base + "/archives/" + INDEX_CONFIGS + "/groups/" + CONFIG_ENUM + ".dat")));

		FSFile file = files.findFile(WORLD_LOCATIONS);
		if (file == null)
		{
			throw new IllegalStateException("cache " + cacheId + " has no enum " + WORLD_LOCATIONS);
		}
		EnumDefinition def = new EnumLoader().load(WORLD_LOCATIONS, file.getContents());

		Map<String, Integer> locations = new LinkedHashMap<>();
		for (int i = 0; i < def.getSize(); i++)
		{
			locations.put(String.valueOf(def.getKeys()[i]), def.getIntVals()[i]);
		}

		JsonObject out = new JsonObject();
		out.addProperty("cache", cacheId);
		out.add("timestamp", cache.get("timestamp"));
		out.add("locations", gson.toJsonTree(locations));
		System.out.println(gson.toJson(out));
	}

	/** The newest live OSRS cache OpenRS2 has archived in full. */
	private static JsonObject latestCache() throws IOException, InterruptedException
	{
		JsonArray caches = gson.fromJson(new String(get(OPENRS2 + "/caches.json")), JsonArray.class);
		JsonObject latest = null;
		for (JsonElement element : caches)
		{
			JsonObject c = element.getAsJsonObject();
			if (!c.get("game").getAsString().equals("oldschool")
				|| !c.get("environment").getAsString().equals("live")
				|| c.get("timestamp").isJsonNull()
				|| c.get("groups").getAsInt() != c.get("valid_groups").getAsInt())
			{
				continue;
			}
			// ISO-8601 UTC timestamps sort lexically.
			if (latest == null || c.get("timestamp").getAsString().compareTo(latest.get("timestamp").getAsString()) > 0)
			{
				latest = c;
			}
		}
		if (latest == null)
		{
			throw new IllegalStateException("no complete oldschool cache listed on OpenRS2");
		}
		return latest;
	}

	private static ArchiveData findGroup(IndexData index, int groupId)
	{
		for (ArchiveData group : index.getArchives())
		{
			if (group.getId() == groupId)
			{
				return group;
			}
		}
		throw new IllegalStateException("config index has no group " + groupId);
	}

	private static byte[] decompress(byte[] container) throws IOException
	{
		// Config groups aren't XTEA encrypted, so no keys are needed.
		return Container.decompress(container, null).data;
	}

	private static byte[] get(String url) throws IOException, InterruptedException
	{
		HttpResponse<byte[]> resp = http.send(HttpRequest.newBuilder(URI.create(url)).build(),
			HttpResponse.BodyHandlers.ofByteArray());
		if (resp.statusCode() != 200)
		{
			throw new IOException("GET " + url + " failed (http " + resp.statusCode() + ")");
		}
		return resp.body();
	}
}
