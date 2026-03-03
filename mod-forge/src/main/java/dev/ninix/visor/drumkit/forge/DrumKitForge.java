package dev.ninix.visor.drumkit.forge;

import dev.ninix.visor.drumkit.core.client.ExampleAddonClient;
import dev.ninix.visor.drumkit.core.common.VisorExample;
import dev.ninix.visor.drumkit.core.handler.DrumKitHandler;
import dev.ninix.visor.drumkit.core.server.ExampleAddonServer;
import dev.ninix.visor.drumkit.forge.platform.ForgeClientTickRegistry;
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
