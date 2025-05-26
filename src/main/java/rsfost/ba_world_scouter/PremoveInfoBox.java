package rsfost.ba_world_scouter;

import lombok.Getter;
import lombok.Setter;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;

import java.awt.Color;
import java.awt.image.BufferedImage;

class PremoveInfoBox extends InfoBox
{
    @Getter
    @Setter
    private String text;
    @Getter @Setter
    private boolean goodPremove;

    public PremoveInfoBox(BufferedImage image, Plugin plugin)
    {
        super(image, plugin);
    }

    @Override
    public Color getTextColor()
    {
        return goodPremove ? Color.GREEN : Color.RED;
    }
}
