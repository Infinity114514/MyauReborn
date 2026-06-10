package myau.module.modules;

import net.minecraft.client.Minecraft;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.PercentProperty;

public class KeepSprint extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final BooleanProperty prediction = new BooleanProperty("Prediction",false);
    public final PercentProperty slowdown = new PercentProperty("slowdown", 0);
    public final BooleanProperty groundOnly = new BooleanProperty("ground-only", false);
    public final BooleanProperty reachOnly = new BooleanProperty("reach-only", false);

    public KeepSprint() {
        super("KeepSprint", false);
    }
    private boolean can = false;
    @EventTarget
    public void onUpdate(UpdateEvent event){
        if (isEnabled()){
            if (event.getType() == EventType.PRE){
                can = false;
            }
            if (event.getType() == EventType.POST){
                can = true;
            }
        }
    }

    public boolean shouldKeepSprint() {
        if (this.prediction.getValue() && !can){
            return false;
        }
        else if (this.groundOnly.getValue() && !mc.thePlayer.onGround) {
            return false;
        } else {
            return !this.reachOnly.getValue() || mc.objectMouseOver.hitVec.distanceTo(mc.getRenderViewEntity().getPositionEyes(1.0F)) > 3.0;
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{slowdown.getValue() + "%"};
    }
}
