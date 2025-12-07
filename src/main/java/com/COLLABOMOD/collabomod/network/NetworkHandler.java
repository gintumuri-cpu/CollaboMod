package com.COLLABOMOD.collabomod.network;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CollaboMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(id++,
                PacketSyncMagicStats.class,
                PacketSyncMagicStats::encode,
                PacketSyncMagicStats::new,
                PacketSyncMagicStats::handle);
        INSTANCE.registerMessage(id++,
                PacketMaterialBurst.class,
                PacketMaterialBurst::toBytes,
                PacketMaterialBurst::new,
                PacketMaterialBurst::handle);
        INSTANCE.registerMessage(id++,
                PacketInstallSpell.class,
                PacketInstallSpell::toBytes,
                PacketInstallSpell::new,
                PacketInstallSpell::handle);
    }
}
