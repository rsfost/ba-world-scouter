package rsfost.ba_world_scouter;

import lombok.RequiredArgsConstructor;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(BaWorldScouterConfig.CONFIG_GROUP)
public interface BaWorldScouterConfig extends Config
{
    String CONFIG_GROUP = "ba_world_scouter";
    String INDICATOR_ACTIVE_MODE = "indicatorActiveMode";
    String INDICATOR_DISPLAY_MODE = "indicatorDisplayMode";
    String SHOW_PREDICTED_VALUES = "showPredictedValues";
    String HIDE_SIDE_PANEL = "hideSidePanel";

    @ConfigItem(
        keyName = INDICATOR_ACTIVE_MODE,
        name = "Indication condition",
        description = "When to display the premove indicator",
        position = 1
    )
    default IndicatorActiveMode indicatorActiveMode()
    {
        return IndicatorActiveMode.SCROLLER_ONLY;
    }

    @ConfigItem(
        keyName = INDICATOR_DISPLAY_MODE,
        name = "Indication display",
        description = "How to display the premove indicator",
        position = 2
    )
    default IndicatorDisplayMode indicatorDisplayMode()
    {
        return IndicatorDisplayMode.INFO_BOX_AND_CHAT;
    }

    @ConfigItem(
        keyName = SHOW_PREDICTED_VALUES,
        name = "Show predictions",
        description = "Show predicted Y values instead of confirmed Y values",
        position = 3
    )
    default boolean showPredictedValues()
    {
        return false;
    }

    @ConfigItem(
        keyName = HIDE_SIDE_PANEL,
        name = "Hide side panel",
        description = "Hide the world list side panel",
        position = 4
    )
    default boolean hideSidePanel()
    {
        return false;
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
