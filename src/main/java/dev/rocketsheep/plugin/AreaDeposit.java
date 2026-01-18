package dev.rocketsheep.plugin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.rocketsheep.plugin.commands.AreaDepositCommand;
import dev.rocketsheep.plugin.interaction.AreaDepositorInteraction;

import javax.annotation.Nonnull;

/**
 * AreaDeposit Plugin
 *
 * Quickly deposits items from your inventory into nearby containers that already
 * have matching items - like the Quick Stack button on chests, but for all
 * containers in range at once.
 *
 * Features:
 *   - /ad [radius] command - Deposit to all nearby containers
 *   - Area Depositor block - Place and interact to deposit items
 */
public class AreaDeposit extends JavaPlugin {

    public AreaDeposit(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        // Register the /ad command
        this.getCommandRegistry().registerCommand(new AreaDepositCommand());

        // Register the custom interaction type for the Area Depositor block
        this.getCodecRegistry(Interaction.CODEC).register(
            "RocketSheep_AreaDepositor_Activate",
            AreaDepositorInteraction.class,
            AreaDepositorInteraction.CODEC
        );
    }

    @Override
    protected void start() {
        // TEST: Register connect event to verify event system works
        this.getEventRegistry().registerGlobal(
            PlayerConnectEvent.class,
            event -> {
                event.getPlayer().sendMessage(Message.raw("[AreaDeposit] Event system working!"));
            }
        );
    }
}
