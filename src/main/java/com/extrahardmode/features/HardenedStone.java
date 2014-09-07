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

package com.extrahardmode.features;


import com.extrahardmode.ExtraHardMode;
import com.extrahardmode.config.RootConfig;
import com.extrahardmode.config.RootNode;
import com.extrahardmode.config.messages.MessageNode;
import com.extrahardmode.events.EhmHardenedStoneEvent;
import com.extrahardmode.module.BlockModule;
import com.extrahardmode.module.MsgModule;
import com.extrahardmode.module.PlayerModule;
import com.extrahardmode.module.UtilityModule;
import com.extrahardmode.service.Feature;
import com.extrahardmode.service.ListenerModule;
import com.extrahardmode.service.PermissionNode;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Hardened Stone is there to make branchmining harder/impossible
 * <p/>
 * Only Iron/Diamond Picks can break stone , Tools break faster when breaking stone , Breaking ore causes surounding
 * stone to fall , Various Fixes to prevent working around the hardened stone
 */
public class HardenedStone extends ListenerModule
{
    private static final Logger log_ = Logger.getLogger("ExtraHardMode");

    private static void info(String message)
    {
    	log_.info("[ExtraHardMode] "+message);
    }
    private RootConfig CFG;

    private MsgModule messenger;

    private BlockModule blockModule;

    private PlayerModule playerModule;


    public HardenedStone(ExtraHardMode plugin)
    {
        super(plugin);
    }


    @Override
    public void starting()
    {
        super.starting();
        CFG = plugin.getModuleForClass(RootConfig.class);
        messenger = plugin.getModuleForClass(MsgModule.class);
        blockModule = plugin.getModuleForClass(BlockModule.class);
        playerModule = plugin.getModuleForClass(PlayerModule.class);
    }


    /**
     * When a player breaks stone
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event)
    {
        Block block = event.getBlock();
        World world = block.getWorld();
        Player player = event.getPlayer();

        final boolean hardStoneEnabled = CFG.getBoolean(RootNode.SUPER_HARD_STONE, world.getName());
        final boolean hardStonePhysix = CFG.getBoolean(RootNode.SUPER_HARD_STONE_PHYSICS, world.getName());
        final boolean playerBypasses = playerModule.playerBypasses(player, Feature.HARDENEDSTONE);
        final boolean cancelled = event.isCancelled();

        final Map<Integer, List<Byte>> tools = CFG.getMappedNode(RootNode.SUPER_HARD_STONE_TOOLS, world.getName());
        final Map<Integer, List<Byte>> physicsBlocks = CFG.getMappedNode(RootNode.SUPER_HARD_STONE_PHYSICS_BLOCKS, world.getName());

        // FEATURE: stone breaks tools much quicker
        if (hardStoneEnabled && block.getType() == Material.STONE && !playerBypasses)
        {
            ItemStack inHandStack = player.getItemInHand();

            if (inHandStack != null)
            {
                int toolId = inHandStack.getType().getId();
                EhmHardenedStoneEvent hardEvent = new EhmHardenedStoneEvent(player, inHandStack, tools.get(toolId) != null ? (short) tools.get(toolId).get(0) : 0);

                if (tools.containsKey(toolId) && !cancelled)
                {
                    if (!tools.get(toolId).isEmpty())
                    {
                        /* Broadcast an Event for other Plugins to change if the tool can break stone and the amount of blocks */
                        plugin.getServer().getPluginManager().callEvent(hardEvent);

                        // otherwise, drastically reduce tool durability when breaking stone
                        if (hardEvent.getNumOfBlocks() > 0)
                        {
                        	ItemStack inHand = hardEvent.getTool();
                        	int unbreaking = 1;
                        	//info("Enchants: "+ inHand.getEnchantments());
                        	if(inHand!=null && inHand.getEnchantments().containsKey(Enchantment.DURABILITY))
                        	{
                                //info("Hand has Looting");
                        		unbreaking += inHand.getEnchantments().get(Enchantment.DURABILITY);
                                //info("Drop Scale is now: "+ dropScale);
                        	}
                        	//info("Unbreaking: "+ unbreaking);
                        	int chanceToBreak = 100/unbreaking;
                        	//info("ChanceToBreak: "+ chanceToBreak);
                        	if(plugin.random(chanceToBreak)){
                            	player.setItemInHand(UtilityModule.damage(hardEvent.getTool(), hardEvent.getNumOfBlocks()));
                        	}
                        }
                    }
                }
                if (hardEvent.getNumOfBlocks() == 0)
                {
                    messenger.send(player, MessageNode.STONE_MINING_HELP, PermissionNode.SILENT_STONE_MINING_HELP);
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // when ore is broken, it softens adjacent stone important to ensure players can reach the ore they break
        if (hardStonePhysix && physicsBlocks.containsKey(block.getType().getId()))
        {
            //TODO HIGH EhmOrePhysicsEvent
            for (BlockFace face : blockModule.getTouchingFaces())
            {
                Block adjacentBlock = block.getRelative(face);
                if (adjacentBlock.getType() == Material.STONE)
                    adjacentBlock.setType(Material.COBBLESTONE);
            }
        }
    }


    /**
     * FIX: prevent players from placing ore as an exploit to work around the hardened stone rule
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent placeEvent)
    {
        Player player = placeEvent.getPlayer();
        Block block = placeEvent.getBlock();
        World world = block.getWorld();

        final boolean playerBypasses = playerModule.playerBypasses(player, Feature.HARDENEDSTONE);
        final boolean hardstoneEnabled = CFG.getBoolean(RootNode.SUPER_HARD_STONE, world.getName());
        final Map<Integer, List<Byte>> physicsBlocks = CFG.getMappedNode(RootNode.SUPER_HARD_STONE_PHYSICS_BLOCKS, world.getName());

        //TODO EhmBlockOrePlacementEvent
        if (hardstoneEnabled && !playerBypasses && physicsBlocks.containsKey(block.getTypeId()))
        {
            ArrayList<Block> adjacentBlocks = new ArrayList<Block>();
            for (BlockFace face : blockModule.getTouchingFaces())
                adjacentBlocks.add(block.getRelative(face));

            for (Block adjacentBlock : adjacentBlocks)
            {
                if (adjacentBlock.getType() == Material.STONE)
                {
                    messenger.send(player, MessageNode.NO_PLACING_ORE_AGAINST_STONE);
                    placeEvent.setCancelled(true);
                    return;
                }
            }
        }
    }


    /**
     * When a piston extends prevent players from circumventing hardened stone rules by placing ore, then pushing the
     * ore next to stone before breaking it
     *
     * @param event - Event that occurred
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPistonExtend(BlockPistonExtendEvent event)
    {
        List<Block> blocks = event.getBlocks();
        World world = event.getBlock().getWorld();

        final boolean superHardStone = CFG.getBoolean(RootNode.SUPER_HARD_STONE, world.getName());
        final Map<Integer, List<Byte>> physicsBlocks = CFG.getMappedNode(RootNode.SUPER_HARD_STONE_PHYSICS_BLOCKS, world.getName());

        //TODO EhmBlockOrePlacementEvent
        if (superHardStone)
        {
            // which blocks are being pushed?
            for (Block block : blocks)
            {
                // if any are ore or stone, don't push
                Material material = block.getType();
                if (material == Material.STONE || physicsBlocks.containsKey(material.getId()))
                {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }


    /**
     * When a piston pulls... prevent players from circumventing hardened stone rules by placing ore, then pulling the
     * ore next to stone before breaking it
     *
     * @param event - Event that occurred.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPistonRetract(BlockPistonRetractEvent event)
    {
        Block block = event.getRetractLocation().getBlock();
        World world = block.getWorld();

        final boolean hardStoneEnabled = CFG.getBoolean(RootNode.SUPER_HARD_STONE, world.getName());
        final Map<Integer, List<Byte>> physicsBlocks = CFG.getMappedNode(RootNode.SUPER_HARD_STONE_PHYSICS_BLOCKS, world.getName());

        //TODO EhmBlockOrePlacementEvent
        // we only care about sticky pistons
        if (event.isSticky() && hardStoneEnabled)
        {
            Material material = block.getType();
            if (material == Material.STONE || physicsBlocks.containsKey(material.getId()))
            {
                event.setCancelled(true);
                return;
            }
        }
    }
}
