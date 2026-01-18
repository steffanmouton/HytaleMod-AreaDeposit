package dev.rocketsheep.plugin.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.rocketsheep.plugin.AreaDepositService;

/**
 * ECS Event System that handles UseBlockEvent.Pre for the Area Depositor block.
 */
public class AreaDepositorEventSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {

    private static final String DEPOSITOR_BLOCK_ID = "RocketSheep_Area_Depositor";
    private static final double DEFAULT_RADIUS = 8.0;
    private static final String ACTIVATED_STATE = "Activated";

    public AreaDepositorEventSystem() {
        super(UseBlockEvent.Pre.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        // Match any entity - we filter by block type in handle()
        return Query.any();
    }

    @Override
    public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> commandBuffer, UseBlockEvent.Pre event) {

        // Check if it's our Area Depositor block
        String blockId = event.getBlockType().getId();
        if (!DEPOSITOR_BLOCK_ID.equals(blockId)) {
            return;
        }

        var context = event.getContext();

        // Skip simulated events (e.g., from mods checking if blocks are interactable)
        // Simulated events have context.entity=null, real interactions have the actual Player
        try {
            var entityField = context.getClass().getDeclaredField("entity");
            entityField.setAccessible(true);
            if (entityField.get(context) == null) {
                return;
            }
        } catch (Exception ignored) {
            // If reflection fails, continue with normal processing
        }

        // Get player from the interaction context
        var entityRef = context.getEntity();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        Player player = (Player) store.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }

        // Get the block position as the center for container search
        Vector3i blockPos = event.getTargetBlock();
        Vector3d blockCenter = new Vector3d(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5);

        // Get the world
        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }

        // Execute the deposit logic centered on the block
        int deposited = AreaDepositService.executeDepositFromBlock(player, store, world, blockCenter, DEFAULT_RADIUS);

        // Trigger animation only if items were deposited
        if (deposited > 0) {
            BlockAccessor accessor = world.getChunkIfLoaded(
                ((long) (blockPos.x >> 4) << 32) | ((blockPos.z >> 4) & 0xFFFFFFFFL)
            );
            if (accessor != null) {
                var blockType = accessor.getBlockType(blockPos.x, blockPos.y, blockPos.z);
                if (blockType != null) {
                    accessor.setBlockInteractionState(blockPos, blockType, ACTIVATED_STATE);
                }
            }
        }
    }
}
