package com.COLLABOMOD.collabomod.magic.command;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class CommandType<T extends ICommand> {
    private static final Map<ResourceLocation, CommandType<?>> REGISTRY = new HashMap<>();

    public static final CommandType<ParticleCommand> PARTICLE = register("particle", ParticleCommand::new);
    public static final CommandType<SoundCommand> SOUND = register("sound", SoundCommand::new);
    public static final CommandType<MeshCommand> MESH = register("mesh", MeshCommand::new);
    public static final CommandType<TextureCommand> TEXTURE = register("texture", TextureCommand::new);

    private final Function<CompoundTag, T> factory;

    private CommandType(Function<CompoundTag, T> factory) {
        this.factory = factory;
    }

    public T deserialize(CompoundTag nbt) {
        return this.factory.apply(nbt);
    }

    private static <T extends ICommand> CommandType<T> register(String id, Function<CompoundTag, T> factory) {
        CommandType<T> type = new CommandType<>(factory);
        REGISTRY.put(new ResourceLocation("collabo_mod", id), type);
        return type;
    }

    public static ICommand fromNbt(CompoundTag nbt) {
        ResourceLocation id = new ResourceLocation(nbt.getString("id"));
        CommandType<?> type = REGISTRY.get(id);
        if (type != null) {
            return type.deserialize(nbt);
        }
        throw new IllegalArgumentException("Unknown command type: " + id);
    }

    public static ResourceLocation getId(CommandType<?> type) {
        return REGISTRY.entrySet().stream()
                .filter(entry -> entry.getValue() == type)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unregistered command type"));
    }
}