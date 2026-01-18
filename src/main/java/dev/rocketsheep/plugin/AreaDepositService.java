package dev.rocketsheep.plugin;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ListTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerBlockState;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;

import java.util.ArrayList;
import java.util.List;

/**
 * Service class containing the core Area Deposit logic.
 * Used by both the /ad command and the Area Depositor block.
 */
public class AreaDepositService {

    /**
     * Record holding a container and its block position.
     */
    public record ContainerInfo(ItemContainer container, int x, int y, int z) {}

    private static final double MAX_RADIUS = 32.0;

    /**
     * Executes the area deposit functionality for a player.
     * Finds all nearby containers and performs quick stack to each one.
     *
     * @param entityRef Reference to the player entity
     * @param radius The search radius in blocks
     */
    public static void executeDeposit(Ref<EntityStore> entityRef, double radius) {
        // Clamp radius
        if (radius > MAX_RADIUS) {
            radius = MAX_RADIUS;
        }
        if (radius < 1.0) {
            radius = 1.0;
        }

        final double finalRadius = radius;

        // Execute on the world thread
        Universe.get().getDefaultWorld().execute(() -> {
            executeDepositInternal(entityRef, finalRadius);
        });
    }

    /**
     * Executes the area deposit functionality with access to the world store.
     *
     * @param entityRef Reference to the player entity
     * @param store The entity store
     * @param world The world
     * @param radius The search radius
     */
    public static void executeDepositWithStore(
            Ref<EntityStore> entityRef,
            Store<EntityStore> store,
            World world,
            double radius) {

        // Validate entity reference
        if (!entityRef.isValid()) {
            return;
        }

        // Clamp radius
        if (radius > MAX_RADIUS) {
            radius = MAX_RADIUS;
        }
        if (radius < 1.0) {
            radius = 1.0;
        }

        // Get player component
        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }

        // Get player position
        TransformComponent transform = player.getTransformComponent();
        Vector3d playerPos = transform.getPosition();

        // Get player's inventory storage container
        Inventory playerInventory = player.getInventory();
        if (playerInventory == null) {
            player.sendMessage(Message.raw("Error: Could not access inventory."));
            return;
        }
        ItemContainer playerStorage = playerInventory.getStorage();

        // Find all nearby container blocks
        List<ItemContainer> nearbyContainers = findNearbyContainers(world, playerPos, radius);

        if (nearbyContainers.isEmpty()) {
            player.sendMessage(Message.raw("No containers found within " + (int) radius + " blocks."));
            return;
        }

        // Execute quick stack to each container individually (matches vanilla behavior)
        int containersWithDeposits = 0;
        int totalOperations = 0;

        for (ItemContainer container : nearbyContainers) {
            ListTransaction<?> transaction = playerStorage.quickStackTo(container);
            if (transaction.succeeded() && transaction.size() > 0) {
                containersWithDeposits++;
                totalOperations += transaction.size();
            }
        }

        // Report actual results
        if (totalOperations > 0) {
            player.sendMessage(Message.raw(
                "Deposited items into " + containersWithDeposits + " container(s)."
            ));
        } else {
            player.sendMessage(Message.raw(
                "No matching items to deposit. Found " + nearbyContainers.size() + " container(s)."
            ));
        }
    }

    private static final String PARTICLE_SYSTEM_ID = "RocketSheep_AreaDeposit_Indicator";

    /**
     * Executes the area deposit functionality centered on a block position.
     * Used by the Area Depositor block.
     *
     * @param player The player whose inventory to deposit from
     * @param store The entity store (for spawning particles)
     * @param world The world to search in
     * @param blockCenter The center position (the block's position)
     * @param radius The search radius in blocks
     * @return The number of deposit operations performed (0 if none)
     */
    public static int executeDepositFromBlock(
            Player player,
            Store<EntityStore> store,
            World world,
            Vector3d blockCenter,
            double radius) {

        // Clamp radius
        if (radius > MAX_RADIUS) {
            radius = MAX_RADIUS;
        }
        if (radius < 1.0) {
            radius = 1.0;
        }

        // Get player's inventory storage container
        Inventory playerInventory = player.getInventory();
        if (playerInventory == null) {
            player.sendMessage(Message.raw("Error: Could not access inventory."));
            return 0;
        }
        ItemContainer playerStorage = playerInventory.getStorage();

        // Find all nearby container blocks centered on the block (with positions)
        List<ContainerInfo> nearbyContainers = findNearbyContainersWithPositions(world, blockCenter, radius);

        if (nearbyContainers.isEmpty()) {
            player.sendMessage(Message.raw("No containers found within " + (int) radius + " blocks."));
            return 0;
        }

        // Spawn particles above ALL containers in range
        for (ContainerInfo info : nearbyContainers) {
            Vector3d particlePos = new Vector3d(info.x(), info.y(), info.z());
            ParticleUtil.spawnParticleEffect(PARTICLE_SYSTEM_ID, particlePos, store);
        }

        // Execute quick stack to each container individually
        int containersWithDeposits = 0;
        int totalOperations = 0;

        for (ContainerInfo info : nearbyContainers) {
            ListTransaction<?> transaction = playerStorage.quickStackTo(info.container());
            if (transaction.succeeded() && transaction.size() > 0) {
                containersWithDeposits++;
                totalOperations += transaction.size();
            }
        }

        // Report actual results
        if (totalOperations > 0) {
            player.sendMessage(Message.raw(
                "Deposited items into " + containersWithDeposits + " container(s)."
            ));
        } else {
            player.sendMessage(Message.raw(
                "No matching items to deposit. Found " + nearbyContainers.size() + " container(s)."
            ));
        }

        return totalOperations;
    }

    /**
     * Internal method to execute deposit when we don't have direct store access.
     */
    private static void executeDepositInternal(Ref<EntityStore> entityRef, double radius) {
        // This method is called from the world execute() context
        // We need to get the store from the entity ref
        // For now, log that this path is not fully implemented
        // The command path (executeDepositWithStore) should be used instead
    }

    /**
     * Finds all storage container blocks within the specified radius of a position.
     * Excludes processing benches (furnaces, tanners, etc.) which have output containers
     * but are not intended for item storage.
     *
     * @param world The world to search in
     * @param center The center position to search from
     * @param radius The search radius in blocks
     * @return List of ItemContainers found in range
     */
    public static List<ItemContainer> findNearbyContainers(World world, Vector3d center, double radius) {
        List<ItemContainer> containers = new ArrayList<>();
        for (ContainerInfo info : findNearbyContainersWithPositions(world, center, radius)) {
            containers.add(info.container());
        }
        return containers;
    }

    /**
     * Finds all storage container blocks within the specified radius of a position,
     * including their block positions.
     * Excludes processing benches (furnaces, tanners, etc.) which have output containers
     * but are not intended for item storage.
     *
     * @param world The world to search in
     * @param center The center position to search from
     * @param radius The search radius in blocks
     * @return List of ContainerInfo objects containing containers and their positions
     */
    public static List<ContainerInfo> findNearbyContainersWithPositions(World world, Vector3d center, double radius) {
        List<ContainerInfo> containers = new ArrayList<>();

        int centerX = (int) Math.floor(center.x);
        int centerY = (int) Math.floor(center.y);
        int centerZ = (int) Math.floor(center.z);
        int radiusInt = (int) Math.ceil(radius);

        // Iterate through all blocks in the cubic area
        for (int x = centerX - radiusInt; x <= centerX + radiusInt; x++) {
            for (int y = centerY - radiusInt; y <= centerY + radiusInt; y++) {
                for (int z = centerZ - radiusInt; z <= centerZ + radiusInt; z++) {
                    // Check if within spherical radius
                    double distance = center.distanceTo(x + 0.5, y + 0.5, z + 0.5);
                    if (distance > radius) {
                        continue;
                    }

                    // Get the block state at this position (false = don't follow filler blocks)
                    BlockState blockState = world.getState(x, y, z, false);

                    // Check if it's a container block
                    if (blockState instanceof ItemContainerBlockState) {
                        // Skip processing benches (furnaces, tanners, etc.)
                        // These have output containers but aren't storage containers
                        String blockTypeId = blockState.getBlockType().getId();
                        if (blockTypeId.startsWith("Bench_")) {
                            continue;
                        }

                        ItemContainerBlockState containerState = (ItemContainerBlockState) blockState;
                        ItemContainer container = containerState.getItemContainer();
                        if (container != null) {
                            containers.add(new ContainerInfo(container, x, y, z));
                        }
                    }
                }
            }
        }

        return containers;
    }
}
