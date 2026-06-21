package com.client2end.addon.modules;

import com.client2end.addon.Client2End;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

public class TestModule extends Module {
    private boolean isRunning = false;
    private boolean sprintToggle = true;
    private long packetsSent = 0;
    private long lastLog = 0;

    public TestModule() {
        super(Client2End.CATEGORY, "test-module", "Stress test server with C2S packet spam");
    }

    private int sequence = 0;

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!isRunning || mc.player == null || mc.getNetworkHandler() == null) return;

        for (int i = 0; i < 50; i++) {
            sendBatch();
        }

        packetsSent += 50;
        long now = System.currentTimeMillis();
        if (now - lastLog > 1000) {
            mc.player.sendMessage(Text.literal("[§6Stress§r] §e" + packetsSent + " §rpkt/sec sent"), false);
            lastLog = now;
            packetsSent = 0;
        }
    }

    private void sendBatch() {
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(
            mc.player,
            sprintToggle
                ? ClientCommandC2SPacket.Mode.START_SPRINTING
                : ClientCommandC2SPacket.Mode.STOP_SPRINTING
        ));
        sprintToggle = !sprintToggle;

        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
            mc.player.getX() + 0.001, mc.player.getY(), mc.player.getZ() + 0.001,
            mc.player.getYaw(), mc.player.getPitch(), true, true
        ));

        if (mc.crosshairTarget instanceof BlockHitResult hit) {
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hit, sequence++));
        }
    }

    @Override
    public void onActivate() {
        isRunning = true;
        sprintToggle = true;
        packetsSent = 0;
        lastLog = 0;
        mc.player.sendMessage(Text.literal("[§6Stress§r] §aModule activated"), false);
    }

    @Override
    public void onDeactivate() {
        isRunning = false;
        mc.player.sendMessage(Text.literal("[§6Stress§r] §cModule deactivated"), false);
    }
}
