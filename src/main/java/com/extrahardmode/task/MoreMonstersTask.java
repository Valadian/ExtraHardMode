/*
 * This file is part of
 * ExtraHardMode Server Plugin for Minecraft
 *
 * Copyright (C) 2012 Ryan Hamshire
 * Copyright (C) 2013 Diemex
 *
 * ExtraHardMode is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ExtraHardMode is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero Public License
 * along with ExtraHardMode.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.extrahardmode.task;


import com.extrahardmode.ExtraHardMode;
import com.extrahardmode.config.RootConfig;
import com.extrahardmode.config.RootNode;
import com.extrahardmode.module.DataStoreModule;
import com.extrahardmode.module.EntityHelper;
import com.extrahardmode.module.PlayerModule;
import com.extrahardmode.service.Feature;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.AbstractMap.SimpleEntry;

/**
 * Task to spawn more monsters, especially in light.
 */
public class MoreMonstersTask implements Runnable
{

    //TODO Return to this and make it actually spawn and not just take the old locations
    //TODO if block not valid check random block nearby

    /**
     * Plugin instance.
     */
    private final ExtraHardMode plugin;

    /**
     * Config instanz
     */
    private final RootConfig CFG;

    private final PlayerModule playerModule;


    /**
     * Constructor.
     *
     * @param plugin - Plugin instance.
     */

    public MoreMonstersTask(ExtraHardMode plugin)
    {
        this.plugin = plugin;
        CFG = plugin.getModuleForClass(RootConfig.class);

        playerModule = plugin.getModuleForClass(PlayerModule.class);
    }


    @Override
    public void run()
    {
        DataStoreModule dataStore = plugin.getModuleForClass(DataStoreModule.class);
        // spawn monsters from the last pass
        for (SimpleEntry<Player, Location> entry : dataStore.getPreviousLocations())
        {
            Location location = entry.getValue();
            Player player = entry.getKey();
            World world = location.getWorld();

            try
            {
                location = verifyLocation(location);
                if (location != null && location.getChunk().isLoaded() && player.isOnline()) //fix monsters spawning at previous locations on login
                {//Check if the player is within 64 blocks, but there are no other players within 16 blocks
                    if (world.getEnvironment() == Environment.NORMAL &&
                            (location.distanceSquared(player.getLocation()) < 64 * 64) && !EntityHelper.arePlayersNearby(location, 16.0))
                    {
                        Entity mob = EntityHelper.spawnRandomMob(location);
                        EntityHelper.markAsOurs(plugin, mob);
                    }
                }
            } catch (IllegalArgumentException ignored)
            {
            } // in case the player is in a different world from the saved location
        }

        // plan for the next pass
        dataStore.getPreviousLocations().clear();
        for (Player player : plugin.getServer().getOnlinePlayers())
        {
            Location verifiedLocation = null;
            //only if player hasn't got bypass and is in survival check location
            if (!playerModule.playerBypasses(player, Feature.MONSTERRULES))
                verifiedLocation = verifyLocation(player.getLocation());
            if (verifiedLocation != null)
                dataStore.getPreviousLocations().add(new SimpleEntry<Player, Location>(player, verifiedLocation));
        }
    }

    //TODO move this into a utility class


    /**
     * Tests if a a given location is elligible to be spawned on
     *
     * @return a valid Location or null if the location is invalid
     */
    private Location verifyLocation(Location location)
    {
        World world = location.getWorld();
        Location verifiedLoc = null;

        final int maxY = CFG.getInt(RootNode.MONSTER_SPAWNS_IN_LIGHT_MAX_Y, world.getName());

        // Only spawn monsters in normal world. End is crowded with endermen and nether is too extreme anyway, add config later
        int lightLvl = location.getBlock().getLightFromSky();
        if (world.getEnvironment() == World.Environment.NORMAL && (location.getY() < maxY && lightLvl < 3))
            verifiedLoc = EntityHelper.isLocSafeSpawn(location);

        return verifiedLoc;
    }
}