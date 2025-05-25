/*
 * Copyright (c) 2018, Psikoi <https://github.com/Psikoi>
 * Copyright (c) 2025, rsfost <https://github.com/rsfost>
 *
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

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

/**
 * Borrows heavily from World Hopper plugin
 */
public class WorldInfoPanel extends PluginPanel
{
    private static final int WORLD_COLUMN_WIDTH = 60;
    private static final int Y_COLUMN_WIDTH = 40;
    private static final int LAST_UPDATED_COLUMN_WIDTH = 47;

    private final BaWorldScouterPlugin plugin;
    private final JPanel listContainer;

    private final ArrayList<WorldTableRow> rows = new ArrayList<>();

    private WorldInfoHeader worldHeader;
    private WorldInfoHeader yHeader;
    private WorldInfoHeader lastUpdatedHeader;

    public WorldInfoPanel(BaWorldScouterPlugin plugin)
    {
        this.plugin = plugin;

        setBorder(null);
        setLayout(new DynamicGridLayout(0, 1));

        JPanel headerContainer = buildHeader();
        this.listContainer = new JPanel();
        listContainer.setLayout(new GridLayout(0, 1));

        add(headerContainer);
        add(listContainer);
    }

    void populate(InstanceInfo[] worlds)
    {
        rows.clear();

        for (InstanceInfo instanceInfo : worlds)
        {
            WorldTableRow row = new WorldTableRow(instanceInfo);
            rows.add(row);
        }
        updateList();
    }

    void updateList()
    {
        listContainer.removeAll();
        for (WorldTableRow row : rows)
        {
            row.setBackground(ColorScheme.DARK_GRAY_COLOR);
            listContainer.add(row);
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

    @Override
    public void onActivate()
    {

    }

    @Override
    public void onDeactivate()
    {

    }

    private JPanel buildHeader()
    {
        JPanel header = new JPanel(new BorderLayout());
        JPanel leftSide = new JPanel(new BorderLayout());
        JPanel rightSide = new JPanel(new BorderLayout());

        yHeader = new WorldInfoHeader("Y", false, false);
        yHeader.setPreferredSize(new Dimension(Y_COLUMN_WIDTH, 20));
        yHeader.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                if (SwingUtilities.isRightMouseButton(e))
                {
                    return;
                }
                // TODO: sorting
            }
        });

        worldHeader = new WorldInfoHeader("World", false, false);
        worldHeader.setPreferredSize(new Dimension(WORLD_COLUMN_WIDTH, 20));
        worldHeader.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                if (SwingUtilities.isRightMouseButton(e))
                {
                    return;
                }
                // TODO: sorting
            }
        });

        lastUpdatedHeader = new WorldInfoHeader("Last updated", false, false);
        lastUpdatedHeader.setPreferredSize(new Dimension(LAST_UPDATED_COLUMN_WIDTH, 20));
        lastUpdatedHeader.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                if (SwingUtilities.isRightMouseButton(e))
                {
                    return;
                }
                // TODO: sorting
            }
        });

        leftSide.add(worldHeader, BorderLayout.WEST);
        leftSide.add(yHeader, BorderLayout.CENTER);

        rightSide.add(lastUpdatedHeader, BorderLayout.CENTER);

        header.add(leftSide, BorderLayout.WEST);
        header.add(rightSide, BorderLayout.CENTER);

        return header;
    }
}
