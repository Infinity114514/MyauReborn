package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.UpdateEvent;
import myau.events.Render2DEvent;
import myau.mixin.IAccessorMinecraft;
import myau.module.Module;
import myau.property.properties.FloatProperty;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class Timer extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final FloatProperty speed = new FloatProperty("speed", 0.01F, 0.01F, 10.0F);

    private boolean wasKeyDown;

    public Timer() {
        super("Timer", false);
    }

    @Override
    public void onEnabled() {
        // 开启时立刻应用速度，确保冻结 / 正常均生效
        net.minecraft.util.Timer timer = ((IAccessorMinecraft) mc).getTimer();
        if (timer != null) {
            timer.timerSpeed = this.speed.getValue();
        }
    }

    @Override
    public void onDisabled() {
        net.minecraft.util.Timer timer = ((IAccessorMinecraft) mc).getTimer();
        if (timer != null) {
            timer.timerSpeed = 1.0F;
        }
        wasKeyDown = false;
    }

    @EventTarget(Priority.HIGHEST)
    public void onUpdate(UpdateEvent event) {
        // 正常速度下还需要 UpdateEvent 来持续设置速度，防止被其他模组重置
        if (!this.isEnabled() || event.getType() != EventType.PRE) {
            return;
        }
        net.minecraft.util.Timer timer = ((IAccessorMinecraft) mc).getTimer();
        if (timer != null) {
            timer.timerSpeed = this.speed.getValue();
        }
    }

    // 完全接管热键：在任何游戏速度下都由渲染线程处理开关
    @EventTarget
    public void onRender2D(Render2DEvent event) {
        // 没有设置热键则什么都不做
        if (this.getKey() == 0) {
            wasKeyDown = false;
            return;
        }

        net.minecraft.util.Timer timer = ((IAccessorMinecraft) mc).getTimer();
        if (timer == null) return;

        // 持续应用速度值（当模块启用时），保证冻结 / 正常时速度永远是你设定的值
        if (this.isEnabled()) {
            timer.timerSpeed = this.speed.getValue();
        }

        // 热键检测 + 去抖
        boolean keyDown = Keyboard.isKeyDown(this.getKey());
        if (keyDown && !wasKeyDown) {
            this.toggle();   // 完全绕过 ModuleManager，自行开关
        }
        wasKeyDown = keyDown;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{String.format("%.1fx", this.speed.getValue())};
    }
}