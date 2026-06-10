//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package myau.util;

import net.minecraft.network.Packet;

public class LatePacket {
    private final Packet<?> packet;
    public long RequiredMs;

    public LatePacket(Packet<?> packet, long requiredMs) {
        this.packet = packet;
        this.RequiredMs = requiredMs;
    }

    public Packet<?> getPacket() {
        return this.packet;
    }

    public long getRequiredMs() {
        return this.RequiredMs;
    }
}
