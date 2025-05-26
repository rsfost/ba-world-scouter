package rsfost.ba_world_scouter;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.runelite.http.api.worlds.World;

@Data
@RequiredArgsConstructor
class InstanceInfo
{
    private final int worldId;
    private final int y;
    private final long time;

    private transient World world;
    private transient int worldLocation;
}
