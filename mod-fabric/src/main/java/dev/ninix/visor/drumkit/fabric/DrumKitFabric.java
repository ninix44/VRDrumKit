package dev.ninix.visor.drumkit.fabric;

import dev.ninix.visor.drumkit.core.client.ExampleAddonClient;
import dev.ninix.visor.drumkit.core.handler.DrumKitHandler;
import dev.ninix.visor.drumkit.core.server.ExampleAddonServer;
import dev.ninix.visor.drumkit.fabric.platform.FabricClientTickRegistry;
import net.fabricmc.api.ModInitializer;
import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;

public class DrumKitFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        FabricClientTickRegistry.init(new DrumKitHandler()::onTick);

        if (ModLoader.get().isDedicatedServer()) {
            VisorAPI.registerAddon(new ExampleAddonServer());
        } else {
            VisorAPI.registerAddon(new ExampleAddonClient());
        }
    }
}
