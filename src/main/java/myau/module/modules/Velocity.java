package myau.module.modules;

import com.google.common.base.CaseFormat;
import myau.Myau;
import myau.enums.DelayModules;
import myau.event.EventManager;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.*;
import myau.management.RotationState;
import myau.mixin.IAccessorEntity;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.util.ChatUtil;
import myau.util.MoveUtil;
import myau.util.RayCastUtil;
import myau.util.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;

import java.util.Objects;

public class Velocity extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static boolean attack = false;
    private static boolean inventory = false;
    public static boolean hasReceivedVelocity;
    public static boolean attacking;
    private static boolean slot = false;
    private static boolean attacked = false;
    private static boolean swing = false;
    private static boolean block = false;
    private static boolean inventory1 = false;
    private static boolean dig = false;

    private int reduceTicks = 0;
    private int reduceAnInt = 0;
    private static boolean b_slot = false;
    private static boolean b_attack = false;
    private static boolean b_swing = false;
    private static boolean b_block = false;
    private static boolean b_inventory = false;
    private static boolean b_dig = false;

    // Leader's prediction variables
    private int ticksSinceVelocity = -1;
    private boolean handleReset = false;
    private int reduceTick = -1;
    private boolean extraAttacked = false;

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"VANILLA", "Prediction", "ReduceA", "ReduceB"});
    private final BooleanProperty noBlink = new BooleanProperty("NoBlinking", true, () -> mode.getValue() > 1);
    private final BooleanProperty noBlocking = new BooleanProperty("NoBlocking", true, () -> mode.getValue() == 2); // 仅在 ReduceA 下可用

    // Prediction & ReduceB Shared/Specific Properties
    public final BooleanProperty reduce = new BooleanProperty("Reduce", true, () -> mode.getValue() == 1 || mode.getValue() == 3);
    public final ModeProperty reduceMode = new ModeProperty("ReduceMode", 0, new String[]{"Attack", "ReleaseWhenCanAttack","ReleaseBeforeCanAttack"}, () -> mode.getValue() == 1 && reduce.getValue());
    private final BooleanProperty extraAttack = new BooleanProperty("ExtraAttack", false, () -> mode.getValue() == 1 && reduce.getValue() && reduceMode.getValue() == 1);
    private final BooleanProperty reduceWhenCanAttack = new BooleanProperty("Reduce When Can Attack", true, () -> mode.getValue() == 1 && reduce.getValue() && reduceMode.getValue() == 0);
    private final BooleanProperty onlySprinting = new BooleanProperty("Only Sprinting", true, () -> mode.getValue() == 1 && reduceMode.getValue() == 0);
    public final IntProperty attackTimes = new IntProperty("Attack Times", 1, 1, 5, () -> this.mode.getValue() == 1 && this.reduce.getValue() && reduceMode.getValue() == 0);

    public final BooleanProperty jump = new BooleanProperty("Jump", true, () -> mode.getValue() == 1);
    public final BooleanProperty delay = new BooleanProperty("Delay", false, () -> mode.getValue() == 1);
    public final BooleanProperty airBuffer = new BooleanProperty("Delay Till On Ground", true, () -> mode.getValue() == 1 && delay.getValue());
    public final IntProperty delayTicks = new IntProperty("Delay Ticks", 1, 1, 5, () -> mode.getValue() == 1 && delay.getValue() && !this.airBuffer.getValue());
    public final BooleanProperty groundDelay = new BooleanProperty("Ground Delay", false, () -> mode.getValue() == 1 && delay.getValue() && !airBuffer.getValue());
    public final BooleanProperty rotate = new BooleanProperty("Rotate", false, () -> this.mode.getValue() == 1);
    public final IntProperty rotateTick = new IntProperty("Rotate Ticks", 3, 1, 12, () -> this.mode.getValue() == 1 && this.rotate.getValue());
    public final BooleanProperty autoMove = new BooleanProperty("Auto Move", false, () -> this.mode.getValue() == 1 && this.rotate.getValue());

    // Vanilla/Other properties
    public final PercentProperty chance = new PercentProperty("chance", 100, () -> mode.getValue() == 0);
    public final PercentProperty horizontal = new PercentProperty("horizontal", 100, () -> mode.getValue() == 0);
    public final PercentProperty vertical = new PercentProperty("vertical", 100, () -> mode.getValue() == 0);
    public final PercentProperty explosionHorizontal = new PercentProperty("explosions-horizontal", 100, () -> mode.getValue() == 0);
    public final PercentProperty explosionVertical = new PercentProperty("explosions-vertical", 100, () -> mode.getValue() == 0);
    public final BooleanProperty fakeCheck = new BooleanProperty("fake-check", true);
    public final BooleanProperty debug = new BooleanProperty("debug", false);

    public final BooleanProperty tickExactEnable;
    public final IntProperty tick500;
    public final IntProperty tick1000;
    public final IntProperty tick2000;
    public final IntProperty tick3000;
    public final IntProperty tick4000;
    public final IntProperty tick5000;
    public final IntProperty tick6000;
    public final IntProperty tick7000;
    public final IntProperty tick8000;
    public final IntProperty tick9000;
    public final IntProperty tick10000;

    public boolean knockback = false;
    private int chanceCounter = 0;
    private int rotatoTickCounter = 0;
    private boolean pendingExplosion = false;
    private boolean allowNext = true;
    private boolean delayFlag = false;
    private boolean isFallDamage;
    private boolean jumpFlag = false;
    private double knockbackX = 0;
    private float[] targetRotation = null;
    private double knockbackZ = 0;

    public Velocity() {
        super("Velocity", false);

        this.tickExactEnable = new BooleanProperty("TickExact", true, () -> mode.getValue() == 3);
        this.tick500 = new IntProperty("500", 3, 0, 20, () -> mode.getValue() == 3);
        this.tick1000 = new IntProperty("1000", 4, 0, 20, () -> mode.getValue() == 3);
        this.tick2000 = new IntProperty("2000", 4, 0, 20, () -> mode.getValue() == 3);
        this.tick3000 = new IntProperty("3000", 5, 0, 20, () -> mode.getValue() == 3);
        this.tick4000 = new IntProperty("4000", 6, 0, 20, () -> mode.getValue() == 3);
        this.tick5000 = new IntProperty("5000", 6, 0, 20, () -> mode.getValue() == 3);
        this.tick6000 = new IntProperty("6000", 7, 0, 20, () -> mode.getValue() == 3);
        this.tick7000 = new IntProperty("7000", 7, 0, 20, () -> mode.getValue() == 3);
        this.tick8000 = new IntProperty("8000", 8, 0, 20, () -> mode.getValue() == 3);
        this.tick9000 = new IntProperty("9000", 8, 0, 20, () -> mode.getValue() == 3);
        this.tick10000 = new IntProperty("10000", 9, 0, 20, () -> mode.getValue() == 3);
    }

    private boolean isInLiquidOrWeb() {
        return mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || ((IAccessorEntity) mc.thePlayer).getIsInWeb();
    }

    @EventTarget
    public void onKnockback(KnockbackEvent event) {
        if (!allowNext || !(Boolean) fakeCheck.getValue()) {
            allowNext = true;
            if (pendingExplosion) {
                if (mode.getValue() == 0) {
                    pendingExplosion = false;
                    if (explosionHorizontal.getValue() > 0) {
                        event.setX(event.getX() * (double) explosionHorizontal.getValue() / 100.0);
                        event.setZ(event.getZ() * (double) explosionHorizontal.getValue() / 100.0);
                    } else {
                        event.setX(mc.thePlayer.motionX);
                        event.setZ(mc.thePlayer.motionZ);
                    }
                    if (explosionVertical.getValue() > 0) {
                        event.setY(event.getY() * (double) explosionVertical.getValue() / 100.0);
                    } else {
                        event.setY(mc.thePlayer.motionY);
                    }
                }
            } else {
                if (!isEnabled() || event.isCancelled()) {
                    pendingExplosion = false;
                    allowNext = true;
                    return;
                }

                if (this.mode.getValue() == 1 && this.rotate.getValue() && event.getY() > 0.0) {
                    this.knockbackX = event.getX();
                    this.knockbackZ = event.getZ();
                    if (Math.abs(this.knockbackX) > 0.01 || Math.abs(this.knockbackZ) > 0.01) {
                        this.rotatoTickCounter = 1;
                    }
                }
                if (this.mode.getValue() == 1 && !delay.getValue()) {
                    ticksSinceVelocity = 0;
                }

                if (mode.getValue() != 1) {
                    double packetDirection = Math.atan2(event.getX(), event.getZ());
                    double degreePlayer = getDirection();
                    double degreePacket = Math.floorMod((int) Math.toDegrees(packetDirection), 360);
                    double angle = Math.abs(degreePacket + degreePlayer);
                    angle = Math.floorMod((int) angle, 360);
                    double threshold = 120.0;
                    boolean inRange = angle >= (180.0 - threshold / 2.0) && angle <= (180.0 + threshold / 2.0);
                    if (inRange) {
                        this.jumpFlag = (this.mode.getValue() == 1 && jump.getValue()) && event.getY() > 0.0;
                    }
                }

                chanceCounter = chanceCounter % 100 + chance.getValue();
                if (chanceCounter >= 100) {
                    if (mode.getValue() == 0) {
                        if (horizontal.getValue() > 0) {
                            event.setX(event.getX() * (double) horizontal.getValue() / 100.0);
                            event.setZ(event.getZ() * (double) horizontal.getValue() / 100.0);
                        } else {
                            event.setX(mc.thePlayer.motionX);
                            event.setZ(mc.thePlayer.motionZ);
                        }
                        if (vertical.getValue() > 0) {
                            event.setY(event.getY() * (double) vertical.getValue() / 100.0);
                        } else {
                            event.setY(mc.thePlayer.motionY);
                        }
                    }
                }
            }
        }
    }

    private boolean badPackets() {
        return (attack) || (inventory);
    }

    private void resetBadPackets() {
        attack = false;
        inventory = false;
    }

    private boolean badPacketsB() {
        return b_slot || b_attack || b_swing || b_block || b_inventory || b_dig;
    }

    private void resetBadPacketsB() {
        b_slot = false;
        b_swing = false;
        b_attack = false;
        b_block = false;
        b_inventory = false;
        b_dig = false;
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && this.jumpFlag) {
            if (mc.thePlayer.onGround && MoveUtil.isForwardPressed() && !mc.thePlayer.isPotionActive(Potion.jump) && !this.isInLiquidOrWeb()) {
                mc.thePlayer.movementInput.jump = true;
            }
            this.jumpFlag = false;
        }
    }

    @EventTarget
    public void onTick(TickEvent event){
        if (this.isEnabled()){
            if (mc.thePlayer == null || mc.thePlayer.isDead || mc.thePlayer.getHealth() <= 0) {
                ticksSinceVelocity = -1;
                handleReset = false;
                return;
            }
            if (ticksSinceVelocity >= 0) {
                ticksSinceVelocity++;
            }
            if (ticksSinceVelocity >= 10) {
                ticksSinceVelocity = -1;
            }
            if (jump.getValue() && this.mode.getValue() == 1){
                handleJumpReset();
            }
        }
    }

    private void handleJumpReset() {
        boolean scaffoldEnabled = false;
        for (Module m : Myau.moduleManager.modules.values()) {
            if (m.getName().equalsIgnoreCase("Scaffold") && m.isEnabled()) scaffoldEnabled = true;
        }

        if (mc.thePlayer == null || mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiInventory || scaffoldEnabled) return;

        if (ticksSinceVelocity >= 0) {
            handleReset = true;
            if (ticksSinceVelocity <= 2 && mc.thePlayer.onGround) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);
            }
        }
        if (ticksSinceVelocity >= 4 && ticksSinceVelocity <= 9) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
            handleReset = false;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!isEnabled()) return;
        if (mc.thePlayer == null || mc.thePlayer.isDead || mc.thePlayer.getHealth() <= 0) {
            hasReceivedVelocity = false;
            extraAttacked = false;
            ticksSinceVelocity = -1;
            handleReset = false;
            delayFlag = false;
            this.rotatoTickCounter = 0;
            this.targetRotation = null;
            return;
        }
        if (mode.getValue() == 1) {
            if (event.getType() == EventType.PRE) {
                int maxTick = this.rotateTick.getValue();
                if (this.rotatoTickCounter > 0 && this.rotatoTickCounter <= maxTick) {
                    if (this.rotatoTickCounter == 1) {
                        double deltaX = -this.knockbackX;
                        double deltaZ = -this.knockbackZ;
                        this.targetRotation = RotationUtil.getRotationsTo(deltaX, 0, deltaZ, event.getYaw(), event.getPitch());
                    }
                    if (this.targetRotation != null) {
                        event.setRotation(this.targetRotation[0], this.targetRotation[1], 2);
                        event.setPervRotation(this.targetRotation[0], 2);
                    }
                }
            }
            if (event.getType() == EventType.POST) {
                int maxTick = this.rotateTick.getValue();
                if (this.rotatoTickCounter > 0 && this.rotatoTickCounter <= maxTick) {
                    this.rotatoTickCounter++;
                    if (this.rotatoTickCounter > maxTick) {
                        this.rotatoTickCounter = 0;
                        this.targetRotation = null;
                        this.knockbackX = 0;
                        this.knockbackZ = 0;
                    }
                }
                KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
                if (delayFlag && ((delay.getValue()
                        && (isInLiquidOrWeb() || Myau.delayManager.getDelay() >= (long) delayTicks.getValue() && !airBuffer.getValue()) || (mc.thePlayer.onGround && !groundDelay.getValue() && !airBuffer.getValue()))
                        || (airBuffer.getValue() && mc.thePlayer.onGround && delayFlag))  || (killAura != null && reduceMode.getValue() == 1 && killAura.autoBlock.getValue() == 2 && killAura.blockTick == 0 && killAura.shouldAutoBlock()  && reduce.getValue()) || (killAura != null && reduceMode.getValue() == 2 && killAura.autoBlock.getValue() == 2 && killAura.blockTick == 2 && killAura.shouldAutoBlock()) && reduce.getValue()) {
                    ticksSinceVelocity = 0;
                    if (killAura != null && killAura.getTarget() != null) {
                        if (extraAttack.getValue() && reduceMode.getValue() == 1 && reduce.getValue() && killAura.blockTick == 0) {
                            extraAttacked = true;
                        }
                    }
                    hasReceivedVelocity = true;
                    dbg(Myau.clientName + "Delay/Buffer " + Myau.delayManager.getDelay() + " Ticks");
                    Myau.delayManager.setDelayState(false, DelayModules.VELOCITY);
                    delayFlag = false;
                }
            }

            if (reduce.getValue() && event.getType() == EventType.PRE) {
                if (extraAttacked){
                    KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
                    if (killAura != null && killAura.getTarget() != null) {
                        EventManager.call(new AttackEvent(killAura.getTarget()));
                        mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
                        if (killAura.getTarget() != mc.thePlayer) {
                            mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(killAura.getTarget(), C02PacketUseEntity.Action.ATTACK));
                        } else {
                            mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(Objects.requireNonNull(killAura.getTarget()), C02PacketUseEntity.Action.ATTACK));
                        }
                        mc.thePlayer.motionX *= 0.6D;
                        mc.thePlayer.motionZ *= 0.6D;
                        mc.thePlayer.setSprinting(false);
                    }
                    extraAttacked = false;
                }
                if (hasReceivedVelocity){
                    if (reduceTick >= attackTimes.getValue()){
                        reduceTick = 0;
                        hasReceivedVelocity = false;
                    }
                    RayCastUtil.RayCastResult targetA = RayCastUtil.rayCast(new RotationUtil.RotationVec(event.getYaw(),event.getPitch()),3);
                    if (targetA != null && reduceMode.getValue() == 0) {
                        if (targetA.entityHit instanceof EntityPlayer && targetA.entityHit != mc.thePlayer) {
                            if (mc.thePlayer.isSprinting() || !this.onlySprinting.getValue()) {
                                KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
                                if (killAura != null && killAura.getTarget() != null) {
                                    if (!reduceWhenCanAttack.getValue() || (killAura.blockTick == 0 && killAura.autoBlock.getValue() == 2) || (killAura.autoBlock.getValue() == 6 && killAura.autoBlock.getValue() == 5 && killAura.blockTick == 0)) {
                                        EventManager.call(new AttackEvent(killAura.getTarget()));
                                        mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
                                        if (killAura.getTarget() != mc.thePlayer) {
                                            mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(killAura.getTarget(), C02PacketUseEntity.Action.ATTACK));
                                        } else {
                                            mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(Objects.requireNonNull(killAura.getTarget()), C02PacketUseEntity.Action.ATTACK));
                                        }
                                        mc.thePlayer.motionX *= 0.6D;
                                        mc.thePlayer.motionZ *= 0.6D;
                                        mc.thePlayer.setSprinting(false);
                                    }
                                } else {
                                    EventManager.call(new AttackEvent(targetA.entityHit));
                                    mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
                                    if (targetA.entityHit != mc.thePlayer) {
                                        mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(targetA.entityHit, C02PacketUseEntity.Action.ATTACK));
                                    } else {
                                        mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(Objects.requireNonNull(targetA.entityHit), C02PacketUseEntity.Action.ATTACK));
                                    }
                                    mc.thePlayer.motionX *= 0.6D;
                                    mc.thePlayer.motionZ *= 0.6D;
                                    mc.thePlayer.setSprinting(false);
                                }
                            }
                        }
                    }
                    reduceTick++;
                }
            }
        }

        // Mode 2 (ReduceA) Logic
        if (mode.getValue() == 2) {
            if (event.getType() == EventType.PRE) {
                if (hasReceivedVelocity) {
                    RayCastUtil.RayCastResult targetA = RayCastUtil.rayCast(new RotationUtil.RotationVec(event.getYaw(), event.getPitch()), 3.2);
                    if (targetA != null) {
                        if (targetA.entityHit instanceof EntityPlayer && targetA.entityHit != mc.thePlayer) {
                            if (mc.thePlayer.isSprinting()) {
                                attacking = true;
                                KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
                                if (killAura != null && killAura.getTarget() != null) {
                                    if (!noBlink.getValue() || !Myau.blinkManager.isBlinking()) {
                                        if (!noBlocking.getValue() || !mc.thePlayer.isBlocking()) {
                                            EventManager.call(new AttackEvent(killAura.getTarget()));
                                            mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
                                            if (killAura.getTarget() != mc.thePlayer) {
                                                mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(killAura.getTarget(), C02PacketUseEntity.Action.ATTACK));
                                            } else {
                                                mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(Objects.requireNonNull(killAura.getTarget()), C02PacketUseEntity.Action.ATTACK));
                                            }
                                            mc.thePlayer.motionX *= 0.6D;
                                            mc.thePlayer.motionZ *= 0.6D;
                                            mc.thePlayer.setSprinting(false);
                                            ChatUtil.sendFormatted("Reduced");
                                        }
                                    }
                                    attacking = false;
                                } else {
                                    if (!noBlink.getValue() || !Myau.blinkManager.isBlinking()) {
                                        if (!noBlocking.getValue() || !mc.thePlayer.isBlocking()) {
                                            EventManager.call(new AttackEvent(targetA.entityHit));
                                            mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
                                            if (targetA.entityHit != mc.thePlayer) {
                                                mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(targetA.entityHit, C02PacketUseEntity.Action.ATTACK));
                                            } else {
                                                mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(Objects.requireNonNull(targetA.entityHit), C02PacketUseEntity.Action.ATTACK));
                                            }
                                            mc.thePlayer.motionX *= 0.6D;
                                            mc.thePlayer.motionZ *= 0.6D;
                                            mc.thePlayer.setSprinting(false);
                                            ChatUtil.sendFormatted("Reduced");
                                        }
                                        attacking = false;
                                    }
                                }
                            }
                        }
                    }
                    hasReceivedVelocity = false;
                }
            }
        }

        // Mode 3 (ReduceB) Logic -> Integrated exactly from Attackreduce99.52.java
        if (mode.getValue() == 3) {
            if (event.getType() != EventType.PRE) return;
            if (reduceTicks <= 0 || !reduce.getValue()) return;

            reduceTicks--;

            KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
            if (killAura == null || !killAura.isEnabled()) return;

            EntityLivingBase target = killAura.getTarget();
            if (target == null) return;

            if (((IAccessorEntity) mc.thePlayer).getIsInWeb()) return;
            if (!mc.thePlayer.isSprinting()) return;
            if (!MoveUtil.isMoving()) return;
            if (target == mc.thePlayer) return;
            if (badPacketsB()) return;

            if (mc.getNetHandler() != null) {
                EventManager.call(new AttackEvent(target));
                mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
                mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
            }

            mc.thePlayer.motionX *= 0.6;
            mc.thePlayer.motionZ *= 0.6;
            mc.thePlayer.setSprinting(false);

            reduceAnInt++;
            ChatUtil.sendRaw("Reduce" + reduceAnInt);
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (isEnabled() && event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
                if (packet.getEntityID() == mc.thePlayer.getEntityId()) {

                    if (mode.getValue() == 3) {
                        this.reduceTicks = getReduceTicks(packet.getMotionX(), packet.getMotionZ());
                    }

                    double motionX = packet.getMotionX();
                    double motionZ = packet.getMotionZ();
                    double packetDirection = Math.atan2(motionX, motionZ);
                    double degreePlayer = getDirection();
                    double degreePacket = Math.floorMod((int) Math.toDegrees(packetDirection), 360);
                    double angle = Math.abs(degreePacket + degreePlayer);
                    angle = Math.floorMod((int) angle, 360);
                    double threshold = 120.0;
                    boolean inRange = angle >= (180.0 - threshold / 2.0) && angle <= (180.0 + threshold / 2.0);
                    if (inRange) {
                        isFallDamage = false;
                    }

                    if (mode.getValue() == 1) {
                        if (!delay.getValue()) {
                            hasReceivedVelocity = true;
                        }
                    } else {
                        hasReceivedVelocity = true;
                    }

                    Module longJumpModule = null;
                    for (Module module : Myau.moduleManager.modules.values()) {
                        if (module.getName().equalsIgnoreCase("LongJump")) {
                            longJumpModule = module;
                            break;
                        }
                    }

                    if (mode.getValue() == 1
                            && !delayFlag
                            && !isInLiquidOrWeb()
                            && !pendingExplosion
                            && (!allowNext || !(Boolean) fakeCheck.getValue())
                            && (longJumpModule == null || !longJumpModule.isEnabled())) {
                        if ((airBuffer.getValue() && !mc.thePlayer.onGround) || (delay.getValue() && !mc.thePlayer.onGround) || (delay.getValue() && groundDelay.getValue() && !airBuffer.getValue())) {
                            Myau.delayManager.setDelayState(true, DelayModules.VELOCITY);
                            dbg(Myau.clientName + "Delay/Buffer Active");
                            Myau.delayManager.delayedPacket.offer(packet);
                            event.setCancelled(true);
                            delayFlag = true;
                        }
                    }
                }
            } else if (!(event.getPacket() instanceof S27PacketExplosion)) {
                if (event.getPacket() instanceof S19PacketEntityStatus) {
                    S19PacketEntityStatus packet = (S19PacketEntityStatus) event.getPacket();
                    Entity entity = packet.getEntity(mc.theWorld);
                    if (entity != null && entity.equals(mc.thePlayer) && packet.getOpCode() == 2) {
                        allowNext = false;
                    }
                }
            } else if (mode.getValue() == 0) {
                S27PacketExplosion packet = (S27PacketExplosion) event.getPacket();
                if (packet.func_149149_c() != 0.0F || packet.func_149144_d() != 0.0F || packet.func_149147_e() != 0.0F) {
                    pendingExplosion = true;
                    if (explosionHorizontal.getValue() == 0 || explosionVertical.getValue() == 0) {
                        event.setCancelled(true);
                    }
                }
            }
        }
        if (event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity velocityPacket = (S12PacketEntityVelocity) event.getPacket();
                if (velocityPacket.getEntityID() == mc.thePlayer.getEntityId()) {
                    knockback = true;
                }
            }
        }
        if (event.getType() == EventType.SEND && !event.isCancelled()) {
            Packet<?> packet = event.getPacket();

            if (mode.getValue() == 3) {
                if (packet instanceof C09PacketHeldItemChange) {
                    b_slot = true;
                } else if (packet instanceof C0APacketAnimation) {
                    b_swing = true;
                } else if (packet instanceof C02PacketUseEntity) {
                    C02PacketUseEntity useEntity = (C02PacketUseEntity) packet;
                    if (useEntity.getAction() == C02PacketUseEntity.Action.ATTACK) {
                        b_attack = true;
                    }
                } else if (packet instanceof C08PacketPlayerBlockPlacement) {
                    b_block = true;
                } else if (packet instanceof C07PacketPlayerDigging) {
                    b_block = true;
                    b_dig = true;
                } else if (packet instanceof C0DPacketCloseWindow ||
                        packet instanceof C0EPacketClickWindow ||
                        (packet instanceof C16PacketClientStatus &&
                                ((C16PacketClientStatus) packet).getStatus() == C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT)) {
                    b_inventory = true;
                } else if (packet instanceof C03PacketPlayer) {
                    resetBadPacketsB();
                }
            }

            if (packet instanceof C02PacketUseEntity) {
                C02PacketUseEntity useEntity = (C02PacketUseEntity) packet;
                if (useEntity.getAction() == C02PacketUseEntity.Action.ATTACK) {
                    attack = true;
                }
            } else if (packet instanceof C0DPacketCloseWindow || packet instanceof C0EPacketClickWindow ||
                    (packet instanceof C16PacketClientStatus && ((C16PacketClientStatus) packet).getStatus() == C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT)) {
                inventory = true;
            } else if (packet instanceof C03PacketPlayer) {
                resetBadPackets();
            }
        }
    }

    private int getReduceTicks(int motionX, int motionZ) {
        double kb = Math.hypot(motionX, motionZ);

        if (!tickExactEnable.getValue()) {
            double ticks = 6.43153527E-4 * kb + 2.9419087136;
            int result = (int) Math.round(ticks);
            if (result < 1) result = 1;
            if (result > 10) result = 10;
            return result;
        }

        if (kb <= 500) return tick500.getValue();
        if (kb <= 1000) return tick1000.getValue();
        if (kb <= 2000) return tick2000.getValue();
        if (kb <= 3000) return tick3000.getValue();
        if (kb <= 4000) return tick4000.getValue();
        if (kb <= 5000) return tick5000.getValue();
        if (kb <= 6000) return tick6000.getValue();
        if (kb <= 7000) return tick7000.getValue();
        if (kb <= 8000) return tick8000.getValue();
        if (kb <= 9000) return tick9000.getValue();
        return tick10000.getValue();
    }

    @EventTarget
    public void onMove(MoveInputEvent event) {
        if (this.isEnabled()){
            if (handleReset) {
                mc.thePlayer.movementInput.moveForward = 1.0F;
            }
        }
        if (this.isEnabled() && this.rotatoTickCounter > 0 && this.rotatoTickCounter <= this.rotateTick.getValue()) {
            if (this.autoMove.getValue()) {
                mc.thePlayer.movementInput.moveForward = 1.0F;
            }
            if (this.targetRotation != null && RotationState.isActived() && RotationState.getPriority() == 2.0F && MoveUtil.isForwardPressed()) {
                MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
            }
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (mode.getValue() == 2) {
            boolean shouldJump = mc.thePlayer.hurtTime == 9 && mc.thePlayer.isSprinting() && !isFallDamage;
            if (shouldJump && mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown() && !isInLiquidOrWeb()) {
                mc.thePlayer.jump();
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        onDisabled();
    }

    public void dbg(String msg) {
        if (debug.getValue()) ChatUtil.sendFormatted(msg);
    }

    @Override
    public void onEnabled() {
        knockback = false;
        hasReceivedVelocity = false;
        attacking = false;
        this.rotatoTickCounter = 0;
        this.targetRotation = null;
        this.knockbackX = 0;
        this.knockbackZ = 0;
        this.reduceTicks = 0;
        this.reduceAnInt = 0;
        this.ticksSinceVelocity = -1;
        this.handleReset = false;
        this.reduceTick = -1;
        this.extraAttacked = false;
        resetBadPacketsB();
    }

    @Override
    public void onDisabled() {
        pendingExplosion = false;
        allowNext = true;
        hasReceivedVelocity = false;
        attacking = false;
        knockback = false;
        this.reduceTicks = 0;
        this.reduceAnInt = 0;
        this.ticksSinceVelocity = -1;
        this.handleReset = false;
        this.reduceTick = -1;
        this.extraAttacked = false;
        resetBadPacketsB();
    }

    @Override
    public String[] getSuffix() {
        if ((Integer) this.mode.getValue() == 3) {
            return new String[]{"Reduce B"};
        }
        if ((Integer) this.mode.getValue() == 2) {
            return new String[]{"Reduce A"};
        }
        if ((Integer) this.mode.getValue() == 1) {
            return new String[]{"Prediction"};
        }
        if ((Integer) this.mode.getValue() == 0) {
            return new String[]{"Vanilla"};
        }
        return super.getSuffix();
    }

    private double getDirection() {
        float moveYaw = mc.thePlayer.rotationYaw;
        if (mc.thePlayer.moveForward != 0f && mc.thePlayer.moveStrafing == 0f) {
            moveYaw += (mc.thePlayer.moveForward > 0) ? 0 : 180;
        } else if (mc.thePlayer.moveForward != 0f && mc.thePlayer.moveStrafing != 0f) {
            if (mc.thePlayer.moveForward > 0) {
                moveYaw += (mc.thePlayer.moveStrafing > 0) ? -45 : 45;
            } else {
                moveYaw -= (mc.thePlayer.moveStrafing > 0) ? -45 : 45;
            }
            moveYaw += (mc.thePlayer.moveForward > 0) ? 0 : 180;
        } else if (mc.thePlayer.moveStrafing != 0f && mc.thePlayer.moveForward == 0f) {
            moveYaw += (mc.thePlayer.moveStrafing > 0) ? -90 : 90;
        }
        return Math.floorMod((int) moveYaw, 360);
    }
}