package dev.rocketsheep.plugin;

import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.rocketsheep.plugin.commands.AreaDepositCommand;
import dev.rocketsheep.plugin.listeners.BlockUseListener;

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

        // Register the block use event listener for the Area Depositor block
        this.getEventRegistry().registerGlobal(
            UseBlockEvent.Pre.class,
            BlockUseListener::onBlockUse
        );
    }
}
