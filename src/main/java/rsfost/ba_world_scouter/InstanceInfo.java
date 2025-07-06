package rsfost.ba_world_scouter;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.runelite.http.api.worlds.World;

@Data
@RequiredArgsConstructor
class InstanceInfo
{
    private final int worldId;
    private final Coord confirmed;
    private final Coord prediction;
    private final long time;

    private transient World world;
    private transient int worldLocation;

    public int getY()
    {
        return confirmed.getY();
    }

    public long getTime()
    {
        return confirmed.getTime();
    }

    @RequiredArgsConstructor
    @Data
    static class Coord
    {
        private final long time;
        private final int y;
    }
}
