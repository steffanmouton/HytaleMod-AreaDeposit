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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.rocketsheep.plugin.AreaDepositService;

/**
 * ECS Event System that handles UseBlockEvent.Pre for the Area Depositor block.
 */
public class AreaDepositorEventSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {

    private static final String DEPOSITOR_BLOCK_ID = "RocketSheep_Area_Depositor";
    private static final double DEFAULT_RADIUS = 8.0;

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

        // Get player from the interaction context
        var entityRef = event.getContext().getEntity();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        Player player = (Player) store.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }

        // Get the world
        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }

        // Get the block position as the center for container search
        Vector3i blockPos = event.getTargetBlock();
        Vector3d centerPos = new Vector3d(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5);

        // Execute the deposit logic centered on the block
        AreaDepositService.executeDepositFromBlock(player, world, centerPos, DEFAULT_RADIUS);
    }
}
