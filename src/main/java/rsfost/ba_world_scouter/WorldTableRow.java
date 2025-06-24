/*
 * Copyright (c) 2018, Psikoi <https://github.com/Psikoi>
 * Copyright (c) 2025, rsfost <https://github.com/rsfost>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package rsfost.ba_world_scouter;

import lombok.Getter;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldRegion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;

/**
 * Borrows heavily from World Hopper plugin
 */
class WorldTableRow extends JPanel
{
    private static final ImageIcon FLAG_AUS;
    private static final ImageIcon FLAG_UK;
    private static final ImageIcon FLAG_US;
    private static final ImageIcon FLAG_US_EAST;
    private static final ImageIcon FLAG_US_WEST;
    private static final ImageIcon FLAG_GER;

    private static final int WORLD_COLUMN_WIDTH = 60;
    private static final int Y_COLUMN_WIDTH = 40;
    private static final int LAST_UPDATED_COLUMN_WIDTH = 70;

    static
    {
        FLAG_AUS = new ImageIcon(ImageUtil.loadImageResource(BaWorldScouterPlugin.class, "flag_aus.png"));
        FLAG_UK = new ImageIcon(ImageUtil.loadImageResource(BaWorldScouterPlugin.class, "flag_uk.png"));
        FLAG_US = new ImageIcon(ImageUtil.loadImageResource(BaWorldScouterPlugin.class, "flag_us.png"));
        FLAG_US_EAST = new ImageIcon(ImageUtil.loadImageResource(BaWorldScouterPlugin.class, "flag_us_east.png"));
        FLAG_US_WEST = new ImageIcon(ImageUtil.loadImageResource(BaWorldScouterPlugin.class, "flag_us_west.png"));
        FLAG_GER = new ImageIcon(ImageUtil.loadImageResource(BaWorldScouterPlugin.class, "flag_ger.png"));
    }

    private static final int LOCATION_US_WEST = -73;
    private static final int LOCATION_US_EAST = -42;

    @Getter
    private final InstanceInfo instanceInfo;

    private JLabel worldField;
    private JLabel yField;
    private JLabel lastUpdatedField;

    public WorldTableRow(InstanceInfo instanceInfo)
    {
        this.instanceInfo = instanceInfo;
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(2, 0, 2, 0));

        JPanel leftSide = new JPanel(new BorderLayout());
        JPanel rightSide = new JPanel(new BorderLayout());
        leftSide.setOpaque(false);
        rightSide.setOpaque(false);

        JPanel worldField = buildWorldField();
        worldField.setPreferredSize(new Dimension(WORLD_COLUMN_WIDTH, 20));
        worldField.setOpaque(false);

        JPanel yField = buildYField();
        yField.setPreferredSize(new Dimension(Y_COLUMN_WIDTH, 20));
        yField.setOpaque(false);

        JPanel lastUpdatedField = buildLastUpdatedField();
        lastUpdatedField.setPreferredSize(new Dimension(LAST_UPDATED_COLUMN_WIDTH, 20));
        lastUpdatedField.setOpaque(false);

        leftSide.add(worldField, BorderLayout.WEST);
        leftSide.add(yField, BorderLayout.CENTER);
        rightSide.add(lastUpdatedField, BorderLayout.EAST);

        add(leftSide, BorderLayout.WEST);
        add(rightSide, BorderLayout.CENTER);

    }

    private JPanel buildWorldField()
    {
        JPanel column = new JPanel(new BorderLayout(7, 0));
        column.setBorder(new EmptyBorder(0, 5, 0, 5));

        worldField = new JLabel(Integer.toString(instanceInfo.getWorldId()));

        World world = instanceInfo.getWorld();
        if (world != null)
        {
            ImageIcon flagIcon = getFlag(instanceInfo.getWorld().getRegion(), instanceInfo.getWorldLocation());
            if (flagIcon != null)
            {
                JLabel flag = new JLabel(flagIcon);
                column.add(flag, BorderLayout.WEST);
            }
        }

        column.add(worldField, BorderLayout.CENTER);

        return column;
    }

    private JPanel buildYField()
    {
        JPanel column = new JPanel(new BorderLayout());
        column.setBorder(new EmptyBorder(0, 5, 0, 5));

        yField = new JLabel(formatInt(instanceInfo.getY()));
        yField.setFont(FontManager.getRunescapeSmallFont());

        column.add(yField, BorderLayout.EAST);

        return column;
    }

    private JPanel buildLastUpdatedField()
    {
        JPanel column = new JPanel(new BorderLayout());
        column.setBorder(new EmptyBorder(0, 5, 0, 5));

        lastUpdatedField = new JLabel() {
            @Override
            public String getText() {
                return formatTime(instanceInfo.getTime());
            }
        };
        lastUpdatedField.setFont(FontManager.getRunescapeSmallFont());
        column.add(lastUpdatedField, BorderLayout.EAST);

        return column;
    }

    private static ImageIcon getFlag(WorldRegion region, int worldLocation)
    {
        if (region == null)
        {
            return null;
        }

        switch (region)
        {
            case UNITED_STATES_OF_AMERICA:
                switch (worldLocation)
                {
                    case LOCATION_US_WEST:
                        return FLAG_US_WEST;
                    case LOCATION_US_EAST:
                        return FLAG_US_EAST;
                    default:
                        return FLAG_US;
                }
            case UNITED_KINGDOM:
                return FLAG_UK;
            case AUSTRALIA:
                return FLAG_AUS;
            case GERMANY:
                return FLAG_GER;
            default:
                return null;
        }
    }

    private static String formatTime(long time)
    {
        Instant timestamp = Instant.ofEpochSecond(time);
        Instant now = Instant.now();
        long minutes = Duration.between(timestamp, now).toMinutes();

        if (minutes == 0)
        {
            return "Just now";
        }
        else if (minutes == 1)
        {
            return "1 min ago";
        }
        else
        {
            return String.format("%d mins ago", minutes);
        }
    }

    private static String formatInt(int a)
    {
        return String.format("%d.%dk", a / 1000, (a % 1000) / 100);
    }
}
