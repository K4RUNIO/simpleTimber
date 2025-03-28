package com.github.K4RUNIO.simpleTimber;


import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import java.util.*;

public class TreeBreakListener implements Listener {

    private final SimpleTimber plugin;
    private final Set<UUID> activeTimberPlayers = new HashSet<>();

    public TreeBreakListener(SimpleTimber plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (shouldTriggerTimber(block, tool, player)) {
            activeTimberPlayers.add(player.getUniqueId());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!activeTimberPlayers.contains(player.getUniqueId())) return;

        activeTimberPlayers.remove(player.getUniqueId());
        if (plugin.getConfigManager().isFallingAnimationEnabled()) {
            makeTreeFall(block, player);
        } else {
            breakConnectedLogs(block, player);
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockLand(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof FallingBlock) {
            FallingBlock fallingBlock = (FallingBlock) event.getEntity();
            if (plugin.getConfigManager().getLogTypes().contains(fallingBlock.getBlockData().getMaterial())) {
                event.setCancelled(true);
                fallingBlock.getWorld().dropItemNaturally(
                        fallingBlock.getLocation(),
                        new ItemStack(fallingBlock.getBlockData().getMaterial())
                );
                fallingBlock.remove();
            }
        }
    }

    private boolean shouldTriggerTimber(Block block, ItemStack tool, Player player) {
        return plugin.getConfigManager().getLogTypes().contains(block.getType()) &&
                isAxe(tool.getType()) &&
                !player.isSneaking();
    }

    private void makeTreeFall(Block startBlock, Player player) {
        if (!plugin.getConfigManager().isFallingAnimationEnabled()) {
            breakConnectedLogs(startBlock, player);
            return;
        }

        Set<Block> connectedLogs = findConnectedLogs(startBlock);
        startBlock.breakNaturally();

        for (Block log : connectedLogs) {
            if (log.equals(startBlock)) continue;

            FallingBlock fallingBlock = log.getWorld().spawnFallingBlock(
                    log.getLocation().add(0.5, 0, 0.5),
                    log.getBlockData()
            );

            fallingBlock.setVelocity(new Vector(
                    (Math.random() - 0.5) * 0.2,
                    0.1,
                    (Math.random() - 0.5) * 0.2
            ));

            log.setType(Material.AIR);
            player.playSound(log.getLocation(), Sound.BLOCK_WOOD_BREAK, 1.0f, 1.0f);
        }
    }

    private void breakConnectedLogs(Block startBlock, Player player) {
        Set<Block> connectedLogs = findConnectedLogs(startBlock);
        ItemStack tool = player.getInventory().getItemInMainHand();

        for (Block log : connectedLogs) {
            log.breakNaturally(tool);
        }
    }

    private Set<Block> findConnectedLogs(Block startBlock) {
        Set<Block> result = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        queue.add(startBlock);

        while (!queue.isEmpty()) {
            Block current = queue.poll();
            if (result.contains(current)) continue;

            if (plugin.getConfigManager().getLogTypes().contains(current.getType())) {
                result.add(current);
                queue.add(current.getRelative(1, 0, 0));
                queue.add(current.getRelative(-1, 0, 0));
                queue.add(current.getRelative(0, 0, 1));
                queue.add(current.getRelative(0, 0, -1));
                queue.add(current.getRelative(0, 1, 0));
            }
        }
        return result;
    }

    private boolean isAxe(Material material) {
        return material.name().endsWith("_AXE");
    }
}