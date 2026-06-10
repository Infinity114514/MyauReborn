package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.MoveInputEvent;
import myau.events.UpdateEvent;
import myau.management.RotationState;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.MoveUtil;
import myau.util.RayCastUtil;
import myau.util.RotationUtil.RotationVec;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class MLG extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final BooleanProperty autoSwitch = new BooleanProperty("Auto Switch", true);
    public final ModeProperty moveFix = new ModeProperty("Move Fix", 1, new String[]{"NONE", "SILENT"});
    public final IntProperty priority = new IntProperty("Priority", 2, 1, 10);

    private boolean active = false;
    private boolean onDistance = false;
    private boolean prevOnGround = false;
    private double highestY = 0.0;
    private float originalYaw = 0.0f;

    private boolean firstClickDone = false;
    private boolean secondClickDone = false;
    private int lastSlot = -1;

    public MLG() {
        super("MLG", false);
    }

    @Override
    public void onEnabled() {
        resetState();
        if (mc.thePlayer != null) {
            originalYaw = mc.thePlayer.rotationYaw;
            highestY = mc.thePlayer.posY;
            prevOnGround = mc.thePlayer.onGround;
        }
    }

    @Override
    public void onDisabled() {
        resetState();
        restoreSlot();
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        // 如果 Scaffold 模块启用，则不工作
        Module scaffold = Myau.moduleManager.getModule("Scaffold");
        if (scaffold != null && scaffold.isEnabled()) {
            if (active) {
                restoreSlot();
                resetState();
            }
            return;
        }

        if (!isEnabled() || event.getType() != EventType.PRE) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null) return;

        fallCheck();

        if (!active && onDistance && !mc.thePlayer.onGround) {
            active = true;
            originalYaw = mc.thePlayer.rotationYaw;
            firstClickDone = false;
            secondClickDone = false;

            if (autoSwitch.getValue()) {
                lastSlot = mc.thePlayer.inventory.currentItem;
                int bucketSlot = findWaterBucketSlot();
                if (bucketSlot != -1) {
                    mc.thePlayer.inventory.currentItem = bucketSlot;
                }
            }
        }

        if (active) {
            if (mc.thePlayer.onGround && !secondClickDone) {
                performRightClick();
                secondClickDone = true;
                active = false;
                restoreSlot();
                return;
            }

            if (autoSwitch.getValue()) {
                ItemStack held = mc.thePlayer.inventory.getCurrentItem();
                if (held == null || held.getItem() != Items.water_bucket) {
                    active = false;
                    restoreSlot();
                    return;
                }
            }

            event.setRotation(originalYaw, 90.0f, priority.getValue());
            event.setPervRotation(originalYaw, priority.getValue());

            if (!firstClickDone) {
                double dist = getDistanceToGround();
                if (dist >= 0 && dist <= 3.0) {
                    performRightClick();
                    firstClickDone = true;
                }
            }
        }
    }

    @EventTarget
    public void onMove(MoveInputEvent event) {
        // 如果 Scaffold 模块启用，则不工作
        Module scaffold = Myau.moduleManager.getModule("Scaffold");
        if (scaffold != null && scaffold.isEnabled()) {
            return;
        }

        if (isEnabled() && active && moveFix.getValue() == 1
                && RotationState.isActived()
                && RotationState.getPriority() == priority.getValue()
                && MoveUtil.isForwardPressed()) {
            MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
        }
    }

    private void fallCheck() {
        boolean onGround = mc.thePlayer.onGround;
        if (onGround) {
            onDistance = false;
            highestY = mc.thePlayer.posY;
        } else if (prevOnGround) {
            highestY = mc.thePlayer.posY;
        } else {
            if (highestY - mc.thePlayer.posY > 3.0) {
                onDistance = true;
            }
        }
        prevOnGround = onGround;
    }

    private double getDistanceToGround() {
        RotationVec rotation = new RotationVec(originalYaw, 90.0f);
        RayCastUtil.RayCastResult result = RayCastUtil.rayCast(rotation, 10.0, 0.0f);
        if (result != null && result.typeOfHit == RayCastUtil.RayCastResult.Type.BLOCK && result.hitVec != null) {
            double footY = mc.thePlayer.getEntityBoundingBox().minY;
            return footY - result.hitVec.yCoord;
        }
        return -1;
    }

    private void performRightClick() {
        RotationVec rotation = new RotationVec(originalYaw, 90.0f);
        RayCastUtil.RayCastResult result = RayCastUtil.rayCast(rotation, 10.0, 0.0f);
        if (result != null && result.typeOfHit == RayCastUtil.RayCastResult.Type.BLOCK && result.hitVec != null) {
            mc.playerController.onPlayerRightClick(
                    mc.thePlayer,
                    mc.theWorld,
                    mc.thePlayer.getHeldItem(),
                    result.getBlockPos(),
                    result.sideHit,
                    result.hitVec
            );
            mc.thePlayer.swingItem();
        }
    }

    private int findWaterBucketSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == Items.water_bucket) {
                return i;
            }
        }
        return -1;
    }

    private void restoreSlot() {
        if (autoSwitch.getValue() && lastSlot != -1 && mc.thePlayer != null
                && mc.thePlayer.inventory.currentItem != lastSlot) {
            mc.thePlayer.inventory.currentItem = lastSlot;
        }
    }

    private void resetState() {
        active = false;
        onDistance = false;
        prevOnGround = false;
        highestY = 0.0;
        firstClickDone = false;
        secondClickDone = false;
    }
}