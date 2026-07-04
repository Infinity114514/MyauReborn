package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.LivingUpdateEvent;
import myau.events.MoveInputEvent;
import myau.events.PacketEvent;
import myau.events.StrafeEvent;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer;

public class Stasis extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private double savedMotionX;
    private double savedMotionY;
    private double savedMotionZ;
    private int tickCounter;
    private int phase;
    private final IntProperty STASIS_TICKS ;
    private final IntProperty RELEASE_TICKS ;

    public Stasis() {
        super("Stasis", false);
        this.STASIS_TICKS = new IntProperty("STASIS_TICKS", 2, 1, 5);
        this.RELEASE_TICKS = new IntProperty("RELEASE_TICKS", 40, 1, 60);
    }

    @Override
    public void onEnabled() {
        if (mc.thePlayer != null) {
            savedMotionX = mc.thePlayer.motionX;
            savedMotionY = mc.thePlayer.motionY;
            savedMotionZ = mc.thePlayer.motionZ;
        }
        tickCounter = 0;
        phase = 0;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) return;

        tickCounter++;

        if (phase == 0 && tickCounter >= STASIS_TICKS.getValue()) {
            phase = 1;
            tickCounter = 0;
            mc.thePlayer.motionX = savedMotionX;
            mc.thePlayer.motionY = savedMotionY;
            mc.thePlayer.motionZ = savedMotionZ;
        } else if (phase == 1 && tickCounter >= RELEASE_TICKS.getValue()) {
            phase = 0;
            tickCounter = 0;
            savedMotionX = mc.thePlayer.motionX;
            savedMotionY = mc.thePlayer.motionY;
            savedMotionZ = mc.thePlayer.motionZ;
        }

        if (phase == 0) {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionZ = 0.0;
            mc.thePlayer.motionY = 0.0;
        }
        if (mc.thePlayer != null && mc.thePlayer.onGround) {
            this.setEnabled(false);
            return;
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled() && phase == 0) {
            mc.thePlayer.movementInput.moveForward = 0.0f;
            mc.thePlayer.movementInput.moveStrafe = 0.0f;
            mc.thePlayer.movementInput.jump = false;
            mc.thePlayer.movementInput.sneak = false;
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && phase == 0) {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionY = 0.0;
            mc.thePlayer.motionZ = 0.0;
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled() && phase == 0) {
            event.setForward(0.0f);
            event.setStrafe(0.0f);
        }
    }

    @EventTarget
    public void onSendPacket(PacketEvent e) {
        if (!this.isEnabled()) return;
        if (e.getType() != EventType.SEND) return;
        if (!(e.getPacket() instanceof C03PacketPlayer)) return;

        if (phase == 1) return;

        if (mc.thePlayer == null || mc.thePlayer.hurtTime != 0) {
            return;
        }

        if (!(e.getPacket() instanceof C03PacketPlayer.C05PacketPlayerLook)) {
            e.setCancelled(true);
        }
    }

    @Override
    public void onDisabled() {
        if (mc.thePlayer != null) {
            mc.thePlayer.motionX = savedMotionX;
            mc.thePlayer.motionY = savedMotionY;
            mc.thePlayer.motionZ = savedMotionZ;
        }
        tickCounter = 0;
        phase = 0;
    }
}