package myau.module.modules;

import io.netty.buffer.Unpooled;
import myau.Myau;
import myau.enums.BlinkModules;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PostMotionEvent;
import myau.events.RightClickMouseEvent;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.util.BlockUtil;
import myau.util.ItemUtil;
import myau.util.PacketUtil;
import myau.util.PlayerUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.util.BlockPos;

import java.util.Random;

public class NoSlow extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int delay = 0;
    private boolean post = false;
    private int count;

    public final ModeProperty swordMode = new ModeProperty("sword-mode", 1, new String[]{"NONE", "VANILLA", "GRIM", "PREDICTION"});
    public final PercentProperty swordMotion = new PercentProperty("sword-motion", 100, () -> this.swordMode.getValue() == 1);
    public final BooleanProperty swordSprint = new BooleanProperty("sword-sprint", true, () -> this.swordMode.getValue() != 0);
    public final BooleanProperty killauraonly = new BooleanProperty("killaura-only", false, () -> this.swordMode.getValue() != 0);
    public final IntProperty swapDelay = new IntProperty("swap-delay", 0, 0, 3, () -> swordMode.getValue() == 3);
    public final BooleanProperty test = new BooleanProperty("test", false, () -> swordMode.getValue() == 3);
    public final BooleanProperty c17 = new BooleanProperty("c17-packet", false, () -> swordMode.getValue() == 3);
    public final BooleanProperty noAttack = new BooleanProperty("no-attack", false, () -> swordMode.getValue() == 3);

    public final ModeProperty foodMode = new ModeProperty("food-mode", 0, new String[]{"NONE", "VANILLA", "GRIM"});
    public final PercentProperty foodMotion = new PercentProperty("food-motion", 100, () -> this.foodMode.getValue() == 1);
    public final BooleanProperty foodSprint = new BooleanProperty("food-sprint", true, () -> this.foodMode.getValue() != 0);
    public final ModeProperty bowMode = new ModeProperty("bow-mode", 0, new String[]{"NONE", "VANILLA", "GRIM"});
    public final PercentProperty bowMotion = new PercentProperty("bow-motion", 100, () -> this.bowMode.getValue() == 1);
    public final BooleanProperty bowSprint = new BooleanProperty("bow-sprint", true, () -> this.bowMode.getValue() != 0);

    public NoSlow() {
        super("NoSlow", false);
    }

    public boolean isSwordActive() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killauraonly.getValue()) {
            if (!killAura.isEnabled()) return false;
            if (killAura.getTarget() == null) return false;
        }
        return this.swordMode.getValue() != 0 && ItemUtil.isHoldingSword();
    }

    public boolean isFoodActive() {
        return this.foodMode.getValue() != 0 && ItemUtil.isEating();
    }

    public boolean isBowActive() {
        return this.bowMode.getValue() != 0 && ItemUtil.isUsingBow();
    }

    public boolean isAnyActive() {
        if (this.swordMode.getValue() == 3 && isSwordActive()) {
            KillAura killAura = (KillAura) Myau.moduleManager.getModule(KillAura.class);
            if (!noAttack.getValue() || !((killAura.blockTick == 0 && killAura.autoBlock.getValue() == 2)
                    || (killAura.autoBlock.getValue() == 5 && killAura.blockTick == 0)
                    && killAura.isEnabled() && killAura.isPlayerBlocking())) {
                return delay == 0;
            }
            return false;
        }
        return mc.thePlayer.isUsingItem() && (this.isSwordActive() || this.isFoodActive() || this.isBowActive());
    }

    public boolean canSprint() {
        return (this.isSwordActive() && this.swordSprint.getValue())
                || (this.isFoodActive() && this.foodSprint.getValue())
                || (this.isBowActive() && this.bowSprint.getValue());
    }

    public int getMotionMultiplier() {
        count++;
        if (ItemUtil.isHoldingSword()) {
            if (swordMode.getValue() == 2) {
                return count % 2 == 0 ? 100 : 20;
            }
            return this.swordMotion.getValue();
        } else if (ItemUtil.isEating()) {
            if (foodMode.getValue() == 2) {
                return count % 2 == 0 ? 100 : 20;
            }
            return this.foodMotion.getValue();
        } else if (ItemUtil.isUsingBow()) {
            if (bowMode.getValue() == 2) {
                return count % 2 == 0 ? 100 : 20;
            }
            return this.bowMotion.getValue();
        }
        return 100;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) return;

        if (ItemUtil.isHoldingSword() && mc.thePlayer.isUsingItem()) {
            if (isSwordActive() && this.swordMode.getValue() == 3) {
                if (event.getType() == EventType.PRE) {
                    delay--;
                    if (delay < 0) {
                        KillAura killAura = (KillAura) Myau.moduleManager.getModule(KillAura.class);
                        if (!noAttack.getValue() || !((killAura.blockTick == 0 && killAura.autoBlock.getValue() == 4)
                                || (killAura.autoBlock.getValue() == 3 && killAura.blockTick == 0)
                                && killAura.isEnabled() && killAura.isPlayerBlocking())) {

                            int randomSlot = new Random().nextInt(9);
                            while (randomSlot == mc.thePlayer.inventory.currentItem) {
                                randomSlot = new Random().nextInt(9);
                            }

                            if (test.getValue()) {
                                Myau.blinkManager.setBlinkState(true, BlinkModules.NOSLOW);
                            }
                            PacketUtil.sendPacket(new C09PacketHeldItemChange(randomSlot));
                            if (c17.getValue()) {
                                PacketUtil.sendPacket(new C17PacketCustomPayload("woshijiejue", new PacketBuffer(Unpooled.buffer())));
                            }
                            PacketUtil.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                        }
                        post = true;
                        delay = swapDelay.getValue();
                    }
                }
            }
        } else {
            if (post) {
                if (test.getValue()) {
                    int randomSlot = new Random().nextInt(9);
                    while (randomSlot == mc.thePlayer.inventory.currentItem) {
                        randomSlot = new Random().nextInt(9);
                    }
                    Myau.blinkManager.setBlinkState(false, BlinkModules.NOSLOW);
                    PacketUtil.sendPacket(new C09PacketHeldItemChange(randomSlot));
                    if (c17.getValue()) {
                        PacketUtil.sendPacket(new C17PacketCustomPayload("woshijiejue", new PacketBuffer(Unpooled.buffer())));
                    }
                    PacketUtil.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                }
                post = false;
            }
        }
    }

    @EventTarget
    public void onMotion(PostMotionEvent event) {
        if (!this.isEnabled()) return;
        if (!ItemUtil.isHoldingSword() || !mc.thePlayer.isUsingItem()) return;

        if (isSwordActive() && this.swordMode.getValue() == 3) {
            if (post) {
                post = false;
                if (test.getValue()) {
                    Myau.blinkManager.setBlinkState(false, BlinkModules.NOSLOW);
                }
            }
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled()) {
            if (mc.objectMouseOver != null) {
                switch (mc.objectMouseOver.typeOfHit) {
                    case BLOCK:
                        BlockPos blockPos = mc.objectMouseOver.getBlockPos();
                        if (BlockUtil.isInteractable(blockPos) && !PlayerUtil.isSneaking()) {
                            return;
                        }
                        break;
                    case ENTITY:
                        Entity entityHit = mc.objectMouseOver.entityHit;
                        if (entityHit instanceof EntityVillager) {
                            return;
                        }
                        if (entityHit instanceof EntityLivingBase && TeamUtil.isShop((EntityLivingBase) entityHit)) {
                            return;
                        }
                        break;
                }
            }
        }
    }
}