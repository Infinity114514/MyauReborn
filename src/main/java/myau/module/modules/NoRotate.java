package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.module.Module;
import myau.util.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S08PacketPlayerPosLook.EnumFlags;

import java.lang.reflect.Field;

public class NoRotate extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private boolean reset = false;

    public NoRotate() {
        super("NoRotate", false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.isCancelled()) return;

        if (event.getType() == EventType.RECEIVE) {

            // 处理起床战争等小游戏的死亡/重生重置逻辑
            if (event.getPacket() instanceof S02PacketChat) {
                String msg = ((S02PacketChat) event.getPacket()).getChatComponent().getUnformattedText();
                if (msg.contains("Protect your bed and destroy the enemy beds.") || msg.contains("You will respawn in")) {
                    this.reset = true;
                }
            }

            // 处理服务端下发的位置与视角包 (S08)
            if (event.getPacket() instanceof S08PacketPlayerPosLook) {
                if (this.reset) {
                    this.reset = false;
                    return; // 重生时让服务端正常转头，防止卡死
                }

                S08PacketPlayerPosLook packet = (S08PacketPlayerPosLook) event.getPacket();
                event.setCancelled(true); // 拦截原版处理，防止客户端发生视角强扭

                // 1. 获取服务端期望的参数并处理相对位移 (EnumFlags)
                double x = packet.getX();
                double y = packet.getY();
                double z = packet.getZ();
                float serverYaw = packet.getYaw();
                float serverPitch = packet.getPitch();

                if (packet.func_179834_f().contains(EnumFlags.X)) x += mc.thePlayer.posX;
                if (packet.func_179834_f().contains(EnumFlags.Y)) y += mc.thePlayer.posY;
                if (packet.func_179834_f().contains(EnumFlags.Z)) z += mc.thePlayer.posZ;
                if (packet.func_179834_f().contains(EnumFlags.X_ROT)) serverPitch += mc.thePlayer.rotationPitch;
                if (packet.func_179834_f().contains(EnumFlags.Y_ROT)) serverYaw += mc.thePlayer.rotationYaw;

                // 2. 仅更新本地玩家坐标，保持本地真实视角 (Yaw/Pitch) 不变
                mc.thePlayer.setPosition(x, y, z);
                mc.thePlayer.motionX = 0.0;
                mc.thePlayer.motionY = 0.0;
                mc.thePlayer.motionZ = 0.0;

                // 3. 原样奉还：用服务端期望的视角回发 C06 (ACK)，并且不要加任何随机抖动！
                // 反作弊需要收到与 S08 完全一致的确认包，加了抖动(RandomUtil)会直接拉闸。
                PacketUtil.sendPacketNoEvent(
                        new C06PacketPlayerPosLook(
                                x, mc.thePlayer.getEntityBoundingBox().minY, z,
                                serverYaw, serverPitch, false
                        )
                );

                // 4. 核心修复：同步 lastReported 状态，防止下一个 Tick 发送断骨视角的 C03
                syncLastReported(serverYaw, serverPitch);
            }
        }
    }

    /**
     * 通过反射强制同步客户端的 lastReportedYaw 和 lastReportedPitch。
     * 如果你的端有 Accessor/Mixin，建议直接替换为 mc.thePlayer.setLastReportedYaw(yaw) 以提高性能。
     */
    private void syncLastReported(float yaw, float pitch) {
        try {
            // MCP 1.8.9 中这俩字段通常是 private
            Field yawField = EntityPlayerSP.class.getDeclaredField("lastReportedYaw"); // 如果混淆了，可能叫 field_71159_c
            Field pitchField = EntityPlayerSP.class.getDeclaredField("lastReportedPitch"); // 可能叫 field_71158_b

            yawField.setAccessible(true);
            pitchField.setAccessible(true);

            yawField.setFloat(mc.thePlayer, yaw);
            pitchField.setFloat(mc.thePlayer, pitch);
        } catch (Exception e) {
            // 开发环境下忽略，实际发版时如果崩溃请检查你的 Mapping 名称
            e.printStackTrace();
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.reset = false;
    }

    @Override
    public void onDisabled() {
        this.reset = false;
    }
}