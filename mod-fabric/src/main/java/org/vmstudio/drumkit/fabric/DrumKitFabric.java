package org.vmstudio.drumkit.fabric;

import org.vmstudio.drumkit.core.client.ExampleAddonClient;
import org.vmstudio.drumkit.core.handler.DrumKitHandler;
import org.vmstudio.drumkit.core.server.ExampleAddonServer;
import org.vmstudio.drumkit.fabric.platform.FabricClientTickRegistry;
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
