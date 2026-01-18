package dev.rocketsheep.plugin.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.rocketsheep.plugin.AreaDepositService;

/**
 * Interaction handler for the Area Depositor block.
 * Extends SimpleBlockInteraction to properly handle real vs simulated interactions.
 */
public class AreaDepositorInteraction extends SimpleBlockInteraction {

    public static final BuilderCodec<AreaDepositorInteraction> CODEC =
        BuilderCodec.builder(AreaDepositorInteraction.class, AreaDepositorInteraction::new).build();

    private static final double DEFAULT_RADIUS = 8.0;
    private static final String ACTIVATED_STATE = "Activated";

    @Override
    protected void interactWithBlock(World world, CommandBuffer<EntityStore> commandBuffer,
            InteractionType interactionType, InteractionContext interactionContext,
            ItemStack itemStack, Vector3i blockPos, CooldownHandler cooldownHandler) {

        var ref = interactionContext.getEntity();
        var store = ref.getStore();
        var player = store.getComponent(ref, Player.getComponentType());

        if (player == null) return;

        Vector3d centerPos = new Vector3d(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5);
        int deposited = AreaDepositService.executeDepositFromBlock(player, store, world, centerPos, DEFAULT_RADIUS);

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

    @Override
    protected void simulateInteractWithBlock(InteractionType interactionType,
            InteractionContext interactionContext, ItemStack itemStack,
            World world, Vector3i blockPos) {
        // Empty - don't execute during simulations (e.g., from other mods checking if blocks are interactable)
    }
}
