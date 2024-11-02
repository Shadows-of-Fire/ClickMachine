package dev.shadowsoffire.clickmachine;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;

import dev.shadowsoffire.placebo.config.Configuration;
import dev.shadowsoffire.placebo.network.PayloadProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClickMachineConfig {

    public static int[] speeds = new int[] { 500, 200, 100, 50, 20, 10, 5, 2, 1 };
    public static boolean usesRF = false;
    public static int maxPowerStorage = 50000;
    public static int[] powerPerSpeed = new int[] { 0, 3, 5, 10, 25, 50, 100, 250, 500 };
    public static Set<Item> blacklistedItems = new HashSet<>();

    public static void init(Configuration cfg) {

        String[] def = new String[9];
        for (int i = 0; i < 9; i++) {
            def[i] = ((Integer) speeds[i]).toString();
        }

        String[] unparsed = cfg.getStringList("Speeds", Configuration.CATEGORY_GENERAL, def, "The possible speeds of the auto clicker, in ticks between clicks (ex: 100 = 1 click every 5s). Must have 9 values.");

        for (int i = 0; i < 9; i++) {
            try {
                speeds[i] = Integer.parseInt(unparsed[i]);
            }
            catch (ArrayIndexOutOfBoundsException | NumberFormatException ex) {
                ClickMachine.LOG.error("Failed to parse Speeds value for index {}, reverting to default.", i);
                ex.printStackTrace();
                speeds[i] = Integer.parseInt(def[i]);
            }
        }

        usesRF = cfg.getBoolean("Uses RF", Configuration.CATEGORY_GENERAL, usesRF, "If the auto clicker uses RF");

        def = new String[9];
        for (int i = 0; i < 9; i++) {
            def[i] = ((Integer) powerPerSpeed[i]).toString();
        }

        unparsed = cfg.getStringList("RF Costs", Configuration.CATEGORY_GENERAL, def, "The RF cost per tick for each speed, from 0-8.  Must have 9 values.  Unused if \"Uses RF\" = false");

        for (int i = 0; i < 9; i++) {
            try {
                powerPerSpeed[i] = Integer.parseInt(unparsed[i]);
            }
            catch (ArrayIndexOutOfBoundsException | NumberFormatException ex) {
                ClickMachine.LOG.error("Failed to parse RF Costs value for index {}, reverting to default.", i);
                ex.printStackTrace();
                powerPerSpeed[i] = Integer.parseInt(def[i]);
            }
        }

        maxPowerStorage = cfg.getInt("Max Power Storage", Configuration.CATEGORY_GENERAL, maxPowerStorage, 0, Integer.MAX_VALUE, "How much power the auto clicker can store.  Also the max input rate.  Unused if \"Uses RF\" = false");

        ImmutableSet.Builder<Item> set = ImmutableSet.builder();
        String[] blacklist = cfg.getStringList("Item Blacklist", Configuration.CATEGORY_GENERAL, new String[] { "minecraft:bedrock" }, "Items that may not be held by the clicker");
        for (String s : blacklist) {
            try {
                Item i = BuiltInRegistries.ITEM.get(ResourceLocation.parse(s));
                if (i == Items.AIR) {
                    throw new NullPointerException("Unknown Item: " + s);
                }
                set.add(i);
            }
            catch (Exception ex) {
                ClickMachine.LOG.error("Failed to parse unknown item \"{}\" in Click Machine Blacklist.", s);
                ex.printStackTrace();
            }
        }
        blacklistedItems = set.build();

        if (cfg.hasChanged()) cfg.save();
    }

    public static record ConfigPayload(List<Integer> speeds, boolean usesRF, int maxPower, List<Integer> powerPerSpeed, Set<Item> blacklist) implements CustomPacketPayload {

        public static final Type<ConfigPayload> TYPE = new Type<>(ClickMachine.loc("config"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ConfigPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), ConfigPayload::speeds,
            ByteBufCodecs.BOOL, ConfigPayload::usesRF,
            ByteBufCodecs.INT, ConfigPayload::maxPower,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), ConfigPayload::powerPerSpeed,
            ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.registry(Registries.ITEM)), ConfigPayload::blacklist,
            ConfigPayload::new);

        public ConfigPayload() {
            this(Ints.asList(ClickMachineConfig.speeds), ClickMachineConfig.usesRF, ClickMachineConfig.maxPowerStorage, Ints.asList(ClickMachineConfig.powerPerSpeed), ClickMachineConfig.blacklistedItems);
        }

        @Override
        public Type<ConfigPayload> type() {
            return TYPE;
        }

        public static class Provider implements PayloadProvider<ConfigPayload> {

            @Override
            public Type<ConfigPayload> getType() {
                return TYPE;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, ConfigPayload> getCodec() {
                return CODEC;
            }

            @Override
            public void handle(ConfigPayload msg, IPayloadContext ctx) {
                ClickMachineConfig.speeds = Ints.toArray(msg.speeds);
                ClickMachineConfig.usesRF = msg.usesRF;
                ClickMachineConfig.maxPowerStorage = msg.maxPower;
                ClickMachineConfig.powerPerSpeed = Ints.toArray(msg.powerPerSpeed);
                ClickMachineConfig.blacklistedItems = msg.blacklist;
            }

            @Override
            public List<ConnectionProtocol> getSupportedProtocols() {
                return List.of(ConnectionProtocol.PLAY);
            }

            @Override
            public Optional<PacketFlow> getFlow() {
                return Optional.of(PacketFlow.CLIENTBOUND);
            }

            @Override
            public String getVersion() {
                return "1";
            }

        }

    }
}
