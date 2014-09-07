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

package com.extrahardmode.features.monsters;


import java.util.logging.Logger;

import com.extrahardmode.ExtraHardMode;
import com.extrahardmode.config.RootConfig;
import com.extrahardmode.config.RootNode;
import com.extrahardmode.module.EntityHelper;
import com.extrahardmode.service.ListenerModule;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Changes to ZombiePigmen including:
 * <p/>
 * Always angry , drop netherwart in the nether , spawn on lighting strikes
 */
public class PigMen extends ListenerModule
{
    private static final Logger log_ = Logger.getLogger("ExtraHardMode");

    private static void info(String message)
    {
    	log_.info("[ExtraHardMode] "+message);
    }
    private RootConfig CFG;

    public PigMen(ExtraHardMode plugin)
    {
        super(plugin);
    }


    @Override
    public void starting()
    {
        super.starting();
        CFG = plugin.getModuleForClass(RootConfig.class);
    }


    /**
     * When an Entity dies (Piggie)
     * <p/>
     * Drop netherwart in fortresses and elsewhere in the nether
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event)
    {
        LivingEntity entity = event.getEntity();
        World world = entity.getWorld();

        final boolean pigWartFortress = CFG.getBoolean(RootNode.FORTRESS_PIGS_DROP_WART, world.getName());
        final int pigWartDropEveryWherePercent = CFG.getInt(RootNode.NETHER_PIGS_DROP_WART, world.getName());

        // FEATURE: pig zombies drop nether wart when slain in nether fortresses
        if (world.getEnvironment().equals(World.Environment.NETHER) && entity instanceof PigZombie)
        {
            int dropScale = 1;
            //info("Drop Scale is: 1");
            Entity damager = event.getEntity().getKiller();
            //info("Damager is: "+damager.toString());
            if (damager instanceof Player){

                //info("Damager is Player");
            	ItemStack inHand = ((Player) damager).getItemInHand();
                //info("Weapon is: "+ inHand);
                //if(inHand!=null)
                //{
                //	info("Enchantments: "+inHand.getEnchantments());
                //	info("Looting Level: "+inHand.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS));
                //}
                //boolean hasLootBonus = (inHand!=null && inHand.getEnchantments().containsKey(Enchantment.LOOT_BONUS_MOBS));
                //nfo("Weapon has Looting? "+ hasLootBonus);
            	if(inHand!=null && inHand.getEnchantments().containsKey(Enchantment.LOOT_BONUS_MOBS))
            	{
                    //info("Hand has Looting");
            		dropScale += inHand.getEnchantments().get(Enchantment.LOOT_BONUS_MOBS);
                    //info("Drop Scale is now: "+ dropScale);
            	}
            }
            Block underBlock = entity.getLocation().getBlock().getRelative(BlockFace.DOWN);
            if (pigWartFortress && underBlock.getType() == Material.NETHER_BRICK){
            	ItemStack drop = new ItemStack(Material.NETHER_STALK);
                //info("Setting Itemstack amount to: "+ dropScale);
            	drop.setAmount(dropScale);
                event.getDrops().add(drop);
            }
            // FEATURE: pig zombies sometimes drop nether wart when slain elsewhere
            else if (pigWartDropEveryWherePercent > 0 /*&& plugin.random(pigWartDropEveryWherePercent)*/) {
            	for(int i=0;i<dropScale;i++)
            	{
            		if(plugin.random(pigWartDropEveryWherePercent))
            		{
                        //info("Adding netherstalk");
                        event.getDrops().add(new ItemStack(Material.NETHER_STALK));
            		}
            	}
            	//ItemStack drop = new ItemStack(Material.NETHER_STALK);
                //info("Setting Itemstack amount to: "+ dropScale);
            	//drop.setAmount(dropScale);
                //event.getDrops().add(new ItemStack(Material.NETHER_STALK));
            }
            
        }
    }


    /**
     * When an Entity spawns
     * <p/>
     * Makes Pigmen always angry
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onEntitySpawn(CreatureSpawnEvent event)
    {
        Location location = event.getLocation();
        World world = location.getWorld();
        LivingEntity entity = event.getEntity();

        final boolean pigsAlwaysAggro = CFG.getBoolean(RootNode.ALWAYS_ANGRY_PIG_ZOMBIES, world.getName());

        // FEATURE: always-angry pig zombies
        if (pigsAlwaysAggro)
        {
            if (entity instanceof PigZombie)
            {
                PigZombie pigZombie = (PigZombie) entity;
                pigZombie.setAnger(Integer.MAX_VALUE);
            }
        }
    }


    /**
     * when a chunk loads... Always angry pigzombies
     *
     * @param event - Event that occurred.
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event)
    {
        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();

        final boolean pigAlwaysAggro = CFG.getBoolean(RootNode.ALWAYS_ANGRY_PIG_ZOMBIES, world.getName());

        // FEATURE: always-angry pig zombies
        if (pigAlwaysAggro)
        {
            for (Entity entity : chunk.getEntities())
            {
                if (entity instanceof PigZombie)
                {
                    PigZombie pigZombie = (PigZombie) entity;
                    pigZombie.setAnger(Integer.MAX_VALUE);
                }
            }
        }
    }


    /**
     * When a lightning strikes
     * <p/>
     * spawn pigmen
     */
    @EventHandler
    public void onLightingStrike(LightningStrikeEvent event)
    {
        LightningStrike strike = event.getLightning();

        Location loc = strike.getLocation();
        World world = loc.getWorld();

        final boolean spawnPigsOnLightning = CFG.getBoolean(RootNode.LIGHTNING_SPAWNS_PIGMEN, world.getName());

        if (spawnPigsOnLightning && EntityHelper.simpleIsLocSafeSpawn(loc))
        {
            int rdm = plugin.getRandom().nextInt(10);
            int amount = 1;
            switch (rdm)
            {
                case 0:
                case 1: //20%
                {
                    amount = 2;
                    break;
                }
                case 2:
                case 3: //20%
                {
                    amount = 3;
                    break;
                }
                default:       //60%
                {
                    amount = 1;
                }
            }
            for (int i = 0; i < amount; i++)
            {
                PigZombie pigZombie = world.spawn(loc, PigZombie.class);
                pigZombie.setAnger(Integer.MAX_VALUE);
            }
        }
    }
}