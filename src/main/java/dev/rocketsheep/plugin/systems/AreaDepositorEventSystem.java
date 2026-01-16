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
import com.hypixel.hytale.server.core.modules.interaction.InteractionSimulationHandler;
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

        var context = event.getContext();

        // Get the block position from the event
        Vector3i eventTargetBlock = event.getTargetBlock();

        // Debug: Log interaction info to help diagnose simulation detection
        var interactionManager = context.getInteractionManager();

        // Get player early for debug messages
        var entityRef = context.getEntity();
        Player debugPlayer = null;
        if (entityRef != null && entityRef.isValid()) {
            debugPlayer = (Player) store.getComponent(entityRef, Player.getComponentType());
        }

        System.out.println("[AreaDeposit DEBUG] UseBlockEvent.Pre for Area Depositor");

        // Log InteractionContext info - simulated events use forProxyEntity()
        System.out.println("[AreaDeposit DEBUG] Context class: " + context.getClass().getName());
        System.out.println("[AreaDeposit DEBUG] Context fields:");
        for (var field : context.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                var value = field.get(context);
                System.out.println("[AreaDeposit DEBUG]   - " + field.getName() + ": " + value);
            } catch (Exception e) {
                System.out.println("[AreaDeposit DEBUG]   - " + field.getName() + ": (error reading)");
            }
        }

        // Use the player we already got for debug (or get if debug was skipped)
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        Player player = debugPlayer;
        if (player == null) {
            return;
        }

        Vector3d blockCenter = new Vector3d(eventTargetBlock.x + 0.5, eventTargetBlock.y + 0.5, eventTargetBlock.z + 0.5);

        // Get the world
        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }

        // Execute the deposit logic centered on the block
        AreaDepositService.executeDepositFromBlock(player, world, blockCenter, DEFAULT_RADIUS);
    }
}
