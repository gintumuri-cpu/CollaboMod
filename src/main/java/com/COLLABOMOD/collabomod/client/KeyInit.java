package com.COLLABOMOD.collabomod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyInit {

    public static final KeyMapping ELEMENTAL_SIGHT_KEY = new KeyMapping(
            "key.collabo_mod.elemental_sight",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.category.collabo_mod"
    );

    public static void register() {
        ClientRegistry.registerKeyBinding(ELEMENTAL_SIGHT_KEY);
    }
}
