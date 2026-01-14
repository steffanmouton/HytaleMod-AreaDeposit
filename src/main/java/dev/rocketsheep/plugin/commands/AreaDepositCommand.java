package dev.rocketsheep.plugin.commands;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ListTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerBlockState;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.ArrayList;
import java.util.List;

/**
 * Area Deposit command - deposits items from player inventory into all nearby
 * containers that already contain matching items.
 *
 * This calls the vanilla Quick Stack functionality on each container found
 * within range, saving you from having to open each chest individually.
 *
 * Usage: /ad [radius]
 * Default radius: 8 blocks
 */
public class AreaDepositCommand extends AbstractTargetPlayerCommand {

    private static final double DEFAULT_RADIUS = 8.0;
    private static final double MAX_RADIUS = 32.0;

    @NonNullDecl
    private final DefaultArg<Double> radiusArg;

    public AreaDepositCommand() {
        super("ad", "Deposit items to all nearby containers");

        this.radiusArg = this.withDefaultArg(
            "radius",
            "dev.rocketsheep.areadeposit.commands.ad.arg.radius",
            ArgTypes.DOUBLE,
            DEFAULT_RADIUS,
            "dev.rocketsheep.areadeposit.commands.ad.arg.radius.default"
        );
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext context,
            @NullableDecl Ref<EntityStore> sourceRef,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world,
            @NonNullDecl Store<EntityStore> store) {

        // Validate entity reference per documentation best practices
        if (!ref.isValid()) {
            context.sendMessage(Message.raw("Error: Invalid player reference."));
            return;
        }

        // Get the radius argument, clamped to max
        double radius = context.get(this.radiusArg);
        if (radius > MAX_RADIUS) {
            radius = MAX_RADIUS;
        }
        if (radius < 1.0) {
            radius = 1.0;
        }

        // Get player component with null check
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            context.sendMessage(Message.raw("Error: Could not access player data."));
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
        // The vanilla Quick Stack button operates on one container at a time
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

    /**
     * Finds all container blocks within the specified radius of a position.
     *
     * @param world The world to search in
     * @param center The center position to search from
     * @param radius The search radius in blocks
     * @return List of ItemContainers found in range
     */
    private List<ItemContainer> findNearbyContainers(World world, Vector3d center, double radius) {
        List<ItemContainer> containers = new ArrayList<>();

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
                        ItemContainerBlockState containerState = (ItemContainerBlockState) blockState;
                        ItemContainer container = containerState.getItemContainer();
                        if (container != null) {
                            containers.add(container);
                        }
                    }
                }
            }
        }

        return containers;
    }
}
