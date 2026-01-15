package dev.rocketsheep.plugin.listeners;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import dev.rocketsheep.plugin.AreaDepositService;

/**
 * Listens for block use events and triggers Area Deposit when the
 * Area Depositor block is used.
 */
public class BlockUseListener {

    private static final String DEPOSITOR_BLOCK_ID = "RocketSheep_Area_Depositor";
    private static final double DEFAULT_RADIUS = 8.0;

    /**
     * Handles the UseBlockEvent.Pre event.
     * When a player uses the Area Depositor block, triggers the area deposit functionality.
     */
    public static void onBlockUse(UseBlockEvent.Pre event) {
        // Check if the interacted block is our Area Depositor
        String blockId = event.getBlockType().getId();
        if (!DEPOSITOR_BLOCK_ID.equals(blockId)) {
            return;
        }

        // Get the entity reference from the interaction context
        Ref<EntityStore> entityRef = event.getContext().getEntity();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        // We need to execute the deposit logic
        // This will be handled by AreaDepositService
        AreaDepositService.executeDeposit(entityRef, DEFAULT_RADIUS);

        // Cancel the default interaction (we handle it ourselves)
        event.setCancelled(true);
    }
}
