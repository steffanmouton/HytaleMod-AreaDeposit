package dev.rocketsheep.plugin;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.rocketsheep.plugin.commands.AreaDepositCommand;

import javax.annotation.Nonnull;

/**
 * AreaDeposit Plugin
 *
 * Quickly deposits items from your inventory into nearby containers that already
 * have matching items - like the Quick Stack button on chests, but for all
 * containers in range at once.
 *
 * Commands:
 *   /ad [radius] - Deposit to all nearby containers (default radius: 8 blocks)
 */
public class AreaDeposit extends JavaPlugin {

    public AreaDeposit(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new AreaDepositCommand());
    }
}
