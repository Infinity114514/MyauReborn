package myau.module.modules;

import myau.event.EventTarget;
import myau.events.LivingUpdateEvent;
import myau.events.MoveInputEvent;
import myau.events.PacketEvent;
import myau.events.StrafeEvent;
import myau.events.UpdateEvent;
import myau.event.types.EventType;
import myau.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer;

public class Stuck extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private double savedMotionX;
    private double savedMotionY;
    private double savedMotionZ;
    private boolean allowNextC03 = false;   // 放行一次完整C03的标志

    public Stuck() {
        super("Stuck", false);
    }

    @Override
    public void onEnabled() {
        if (mc.thePlayer != null) {
            savedMotionX = mc.thePlayer.motionX;
            savedMotionY = mc.thePlayer.motionY;
            savedMotionZ = mc.thePlayer.motionZ;
        }
        allowNextC03 = false;   // 每次开启模块时重置标志
    }

    // ========== 玩家运动归零（双重保险） ==========
    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled()) {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionZ = 0.0;
            mc.thePlayer.motionY = 0.0;
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled()) {
            mc.thePlayer.movementInput.moveForward = 0.0f;
            mc.thePlayer.movementInput.moveStrafe = 0.0f;
            mc.thePlayer.movementInput.jump = false;
            mc.thePlayer.movementInput.sneak = false;
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled()) {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionY = 0.0;
            mc.thePlayer.motionZ = 0.0;
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled()) {
            event.setForward(0.0f);
            event.setStrafe(0.0f);
        }
    }

    // ========== 核心：数据包拦截 ==========
    @EventTarget
    public void onSendPacket(PacketEvent e) {
        // 模块未启用时，不做任何拦截
        if (!this.isEnabled()) return;

        // 只处理发送出去的包（不处理接收包）
        if (e.getType() != EventType.SEND) return;

        // 只处理 C03PacketPlayer 及其所有子类
        if (!(e.getPacket() instanceof C03PacketPlayer)) return;

        // 1) 放行标志检查（用于偶尔发一个完整包，防止掉线）
        if (allowNextC03) {
            allowNextC03 = false;
            return;
        }

        // 2) 玩家无效或处于受击僵直时，放行所有C03包
        if (mc.thePlayer == null || mc.thePlayer.hurtTime != 0) {
            return;
        }

        // 3) 核心规则：只有纯视角包（C05PacketPlayerLook）才能通过，其余全部拦截
        if (!(e.getPacket() instanceof C03PacketPlayer.C05PacketPlayerLook)) {
            e.setCancelled(true);   // 拦截坐标移动包等
        }
    }

    @Override
    public void onDisabled() {
        if (mc.thePlayer != null) {
            mc.thePlayer.motionX = savedMotionX;
            mc.thePlayer.motionZ = savedMotionZ;
            mc.thePlayer.motionY = savedMotionY;
        }
        allowNextC03 = false;   // 清理标志
    }
}