package dev.rocketsheep.plugin.listeners;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import dev.rocketsheep.plugin.AreaDepositService;

/**
 * Listens for player interact events and triggers Area Deposit when the
 * Area Depositor block is used.
 */
public class BlockUseListener {

    private static final String DEPOSITOR_BLOCK_ID = "RocketSheep_Area_Depositor";
    private static final double DEFAULT_RADIUS = 8.0;

    /**
     * Handles the PlayerInteractEvent.
     * When a player uses the Area Depositor block, triggers the area deposit functionality.
     */
    public static void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        // DEBUG: Send chat message to verify event is firing
        player.sendMessage(Message.raw("[DEBUG] PlayerInteractEvent: " + event.getActionType()));

        // Only handle Use interactions
        if (event.getActionType() != InteractionType.Use) {
            return;
        }

        // Get target block position
        Vector3i targetBlock = event.getTargetBlock();
        if (targetBlock == null) {
            player.sendMessage(Message.raw("[DEBUG] No target block"));
            return;
        }

        // Get the world to look up block type
        Ref<EntityStore> playerRef = event.getPlayerRef();
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }

        World world = playerRef.getStore().getExternalData().getWorld();
        if (world == null) {
            player.sendMessage(Message.raw("[DEBUG] No world"));
            return;
        }

        // Get block state at target position
        BlockState blockState = world.getState(targetBlock.x, targetBlock.y, targetBlock.z, false);
        if (blockState == null) {
            player.sendMessage(Message.raw("[DEBUG] No block state"));
            return;
        }

        String blockId = blockState.getBlockType().getId();
        player.sendMessage(Message.raw("[DEBUG] Block: " + blockId));

        // Check if the interacted block is our Area Depositor
        if (!DEPOSITOR_BLOCK_ID.equals(blockId)) {
            return;
        }

        // Execute the deposit logic
        AreaDepositService.executeDeposit(playerRef, DEFAULT_RADIUS);

        // Cancel the default interaction
        event.setCancelled(true);
    }
}
