package rsfost.ba_world_scouter;

import lombok.RequiredArgsConstructor;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("ba_world_scouter")
public interface BaWorldScouterConfig extends Config
{
    @ConfigItem(
        keyName = "indicatorActiveMode",
        name = "Indication condition",
        description = "When to display the premove indicator",
        position = 1
    )
    default IndicatorActiveMode indicatorActiveMode()
    {
        return IndicatorActiveMode.SCROLLER_ONLY;
    }

    @ConfigItem(
        keyName = "indicatorDisplayMode",
        name = "Indication display",
        description = "How to display the premove indicator",
        position = 2
    )
    default IndicatorDisplayMode indicatorDisplayMode()
    {
        return IndicatorDisplayMode.INFO_BOX_AND_CHAT;
    }

    @RequiredArgsConstructor
    enum IndicatorDisplayMode
    {
        INFO_BOX_AND_CHAT("Info box/chat"),
        INFO_BOX("Info box"),
        CHAT("Chat");

        private final String name;

        public String toString()
        {
            return name;
        }
    }

    @RequiredArgsConstructor
    enum IndicatorActiveMode
    {
        SCROLLER_ONLY("As scroller"),
        BA_ONLY("In BA"),
        ALL_INSTANCES("All instances"),
        DISABLE("Disable");

        private final String name;

        public String toString()
        {
            return name;
        }
    }
}
