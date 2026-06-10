package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.events.PacketEvent;
import myau.module.Module;
import myau.util.ChatUtil;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

import static myau.config.Config.mc;

public class FlagDetector extends Module {
    public FlagDetector() {
        super("FlagDetector", false, true);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled())
            return;

        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            ChatUtil.sendFormatted(String.format(
                    Myau.clientName +
                    "Lag back detected!",
                    mc.thePlayer.ticksExisted
            ));
        }
    }
}