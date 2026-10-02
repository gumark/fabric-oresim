package dev.gumark.oresim;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * OreSim by gumark - predicts and renders ore locations from the world seed.
 *
 * <p>Toggle with the X key, set the seed with {@code /setoresimseed <seed>}.
 */
public class OreSimClient implements ClientModInitializer {
    public static final String MOD_ID = "oresim";

    private final OreSim oreSim = new OreSim();
    private KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.oresim.toggle", InputConstants.Type.KEYBOARD, InputConstants.KEY_X, category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> onTick());
        ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> oreSim.onChunkLoad(chunk));
        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> FluidOverlay.onChunkUnload(chunk));
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> {
            oreSim.render();
            FluidOverlay.render();
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("oresim").executes(context -> {
                Minecraft.getInstance().setScreenAndShow(new OreSimScreen());
                return 1;
            }));

            dispatcher.register(ClientCommands.literal("setoresimseed")
                .then(ClientCommands.argument("seed", StringArgumentType.greedyString())
                    .executes(context -> {
                        String input = StringArgumentType.getString(context, "seed");
                        long seed = SeedStore.parseSeed(input);
                        SeedStore.setSeed(seed);
                        oreSim.onSeedChanged();
                        context.getSource().sendFeedback(Component.literal("OreSim seed set to: " + seed));
                        return 1;
                    }))
            );
        });
    }

    private void onTick() {
        while (toggleKey.consumeClick()) {
            oreSim.toggle();
        }
        oreSim.tick();
    }
}
