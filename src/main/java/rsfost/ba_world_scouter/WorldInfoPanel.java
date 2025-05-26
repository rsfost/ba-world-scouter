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

import com.google.common.collect.Ordering;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.function.Function;

/**
 * Borrows heavily from World Hopper plugin
 */
public class WorldInfoPanel extends PluginPanel
{
    private static final Color ODD_ROW = new Color(44, 44, 44);

    private static final int WORLD_COLUMN_WIDTH = 60;
    private static final int Y_COLUMN_WIDTH = 40;
    private static final int LAST_UPDATED_COLUMN_WIDTH = 47;

    private final BaWorldScouterPlugin plugin;
    private final JPanel listContainer;

    private WorldOrder orderIndex = WorldOrder.WORLD;
    private boolean ascendingOrder = true;

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

        rows.sort((r1, r2) -> {
            switch (orderIndex)
            {
                case INSTANCE_Y:
                    return getCompareValue(r1, r2, row -> row.getInstanceInfo().getY());
                case WORLD:
                    return getCompareValue(r1, r2, row -> row.getInstanceInfo().getWorldId());
                case LAST_UPDATED:
                    return getCompareValue(r1, r2, row -> row.getInstanceInfo().getTime());
                default:
                    return 0;
            }
        });

        for (int i = 0; i < rows.size(); ++i)
        {
            WorldTableRow row = rows.get(i);
            row.setBackground(i % 2 == 0 ? ODD_ROW : ColorScheme.DARK_GRAY_COLOR);
            listContainer.add(row);
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

    private int getCompareValue(WorldTableRow row1, WorldTableRow row2, Function<WorldTableRow, Comparable> compareByFn)
    {
        Ordering<Comparable> ordering = Ordering.natural();
        if (!ascendingOrder)
        {
            ordering = ordering.reverse();
        }
        ordering = ordering.nullsLast();
        return ordering.compare(compareByFn.apply(row1), compareByFn.apply(row2));
    }

    @Override
    public void onActivate()
    {

    }

    @Override
    public void onDeactivate()
    {

    }

    private void orderBy(WorldOrder order)
    {
        yHeader.highlight(false, ascendingOrder);
        worldHeader.highlight(false, ascendingOrder);
        lastUpdatedHeader.highlight(false, ascendingOrder);

        switch (order)
        {
            case INSTANCE_Y:
                yHeader.highlight(true, ascendingOrder);
                break;
            case WORLD:
                worldHeader.highlight(true, ascendingOrder);
                break;
            case LAST_UPDATED:
                lastUpdatedHeader.highlight(true, ascendingOrder);
                break;
        }

        orderIndex = order;
        updateList();
    }

    private JPanel buildHeader()
    {
        JPanel header = new JPanel(new BorderLayout());
        JPanel leftSide = new JPanel(new BorderLayout());
        JPanel rightSide = new JPanel(new BorderLayout());

        yHeader = new WorldInfoHeader("Y", orderIndex == WorldOrder.INSTANCE_Y, ascendingOrder);
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
                ascendingOrder = orderIndex != WorldOrder.INSTANCE_Y || !ascendingOrder;
                orderBy(WorldOrder.INSTANCE_Y);
            }
        });

        worldHeader = new WorldInfoHeader("World", orderIndex == WorldOrder.WORLD, ascendingOrder);
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
                ascendingOrder = orderIndex != WorldOrder.WORLD || !ascendingOrder;
                orderBy(WorldOrder.WORLD);
            }
        });

        lastUpdatedHeader = new WorldInfoHeader("Last updated", orderIndex == WorldOrder.LAST_UPDATED, ascendingOrder);
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
                ascendingOrder = orderIndex != WorldOrder.LAST_UPDATED || !ascendingOrder;
                orderBy(WorldOrder.LAST_UPDATED);
            }
        });

        leftSide.add(worldHeader, BorderLayout.WEST);
        leftSide.add(yHeader, BorderLayout.CENTER);

        rightSide.add(lastUpdatedHeader, BorderLayout.CENTER);

        header.add(leftSide, BorderLayout.WEST);
        header.add(rightSide, BorderLayout.CENTER);

        return header;
    }

    private enum WorldOrder
    {
        WORLD,
        INSTANCE_Y,
        LAST_UPDATED
    }
}
