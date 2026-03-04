package org.vmstudio.drumkit.forge;

import org.vmstudio.drumkit.core.client.ExampleAddonClient;
import org.vmstudio.drumkit.core.common.VisorExample;
import org.vmstudio.drumkit.core.handler.DrumKitHandler;
import org.vmstudio.drumkit.core.server.ExampleAddonServer;
import org.vmstudio.drumkit.forge.platform.ForgeClientTickRegistry;
import net.minecraftforge.fml.common.Mod;
import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;

@Mod(VisorExample.MOD_ID)
public class DrumKitForge {

    public DrumKitForge() {
        ForgeClientTickRegistry.init(new DrumKitHandler()::onTick);

        if (ModLoader.get().isDedicatedServer()) {
            VisorAPI.registerAddon(new ExampleAddonServer());
        } else {
            VisorAPI.registerAddon(new ExampleAddonClient());
        }
    }
}
