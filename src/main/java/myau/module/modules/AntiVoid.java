package myau.module.modules;

import com.google.common.base.CaseFormat;
import myau.enums.BlinkModules;
import myau.event.EventTarget;
import myau.event.types.Priority;
import myau.events.KeyEvent;
import myau.events.PlayerUpdateEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class AntiVoid extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private boolean isInVoid = false;
    private boolean wasInVoid = false;
    private double[] lastSafePosition = null;
    private int stuckTickCounter = 0;
    private int scaffoldDelayCounter = 0;
    private double targetY = 0.0;
    private BlockPos targetPos = null;
    private boolean isStuckModeActive = false;
    private boolean shouldActivateScaffold = false;

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"BLINK", "STUCK"});
    public final FloatProperty distance = new FloatProperty("distance", 5.0F, 0.0F, 50.0F);
    public final IntProperty stuckTickThreshold = new IntProperty("stuck-tick-threshold", 20, 1, 100);
    public final FloatProperty searchRange = new FloatProperty("search-range", 4.0F, 1.0F, 8.0F);
    public final IntProperty minBlocks = new IntProperty("min-blocks", 3, 1, 64);
    public final BooleanProperty debug = new BooleanProperty("debug", false);
    public final BooleanProperty blinkWhenStuck = new BooleanProperty("blink-when-stuck", true);
    private boolean blinkEnabledByStuck = false; // 标记 Blink 是否由 stuck 模式启用

    private void resetBlink() {
        myau.Myau.blinkManager.setBlinkState(false, BlinkModules.ANTI_VOID);
        this.lastSafePosition = null;
    }
    private boolean canUseAntiVoid() {
        LongJump longJump = (LongJump) myau.Myau.moduleManager.modules.get(LongJump.class);
        return longJump == null || !longJump.isJumping();
    }

    private static final net.minecraft.block.Block[] VOID_BLOCKS_BLACKLIST = new net.minecraft.block.Block[] {
            net.minecraft.init.Blocks.air,
            net.minecraft.init.Blocks.water,
            net.minecraft.init.Blocks.flowing_water,
            net.minecraft.init.Blocks.lava,
            net.minecraft.init.Blocks.flowing_lava,
            net.minecraft.init.Blocks.tallgrass,
            net.minecraft.init.Blocks.double_plant,
            net.minecraft.init.Blocks.yellow_flower,
            net.minecraft.init.Blocks.red_flower,
            net.minecraft.init.Blocks.reeds // 甘蔗
    };

    private boolean isSafeBlock(BlockPos pos) {
        net.minecraft.block.state.IBlockState state = mc.theWorld.getBlockState(pos);
        net.minecraft.block.Block block = state.getBlock();

        for (net.minecraft.block.Block blacklisted : VOID_BLOCKS_BLACKLIST) {
            if (block == blacklisted) return false;
        }
        return block.getCollisionBoundingBox(mc.theWorld, pos, state) != null;
    }

    private boolean isPlayerStuckInAir() {
        if (mc.thePlayer.onGround || mc.thePlayer.capabilities.isFlying) return false;

        AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox().expand(-0.05, 0.0, -0.05);
        int minX = (int) Math.floor(bb.minX);
        int maxX = (int) Math.floor(bb.maxX);
        int minZ = (int) Math.floor(bb.minZ);
        int maxZ = (int) Math.floor(bb.maxZ);
        int playerY = (int) Math.floor(mc.thePlayer.posY);

        for (int y = playerY; y >= playerY - (int)distance.getValue().floatValue(); y--) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (isSafeBlock(new BlockPos(x, y, z))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private BlockPos findPlaceablePosition() {
        int playerX = (int) Math.floor(mc.thePlayer.posX);
        int playerY = (int) Math.floor(mc.thePlayer.posY);
        int playerZ = (int) Math.floor(mc.thePlayer.posZ);
        int range = (int) searchRange.getValue().floatValue();
        for (int y = playerY - 1; y >= playerY - 20; y--) {
            for (int x = playerX - range; x <= playerX + range; x++) {
                for (int z = playerZ - range; z <= playerZ + range; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!BlockUtil.isReplaceable(pos)) {
                        for (EnumFacing facing : EnumFacing.VALUES) {
                            if (facing != EnumFacing.DOWN) {
                                BlockPos placePos = pos.offset(facing);
                                if (BlockUtil.isReplaceable(placePos)) {
                                    return placePos;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void activateStuckMode() {
        isStuckModeActive = true;
        scaffoldDelayCounter = 1;
        targetPos = findPlaceablePosition();
        if (targetPos != null) {
            targetY = targetPos.getY() + 0.75;
        }

        Stuck stuck = (Stuck) myau.Myau.moduleManager.modules.get(Stuck.class);
        if (stuck != null) {
            stuck.setEnabled(true);
        } else {
            ChatUtil.sendFormatted("Module Stuck is null");
        }

        Blink blink = (Blink) myau.Myau.moduleManager.modules.get(Blink.class);
        if (blink != null) {
            if (blink != null) {
                if (!blink.isEnabled()) {
                    blink.setEnabled(true);
                    blinkEnabledByStuck = true; // 记录是由我们启用的
                }
            } else {
                ChatUtil.sendFormatted("Module Blink is null");
            }
        } else {
            blinkEnabledByStuck = false; // 不启用，重置标记
        }
    }

    private void deactivateStuckMode() {
        isStuckModeActive = false;
        shouldActivateScaffold = false;
        stuckTickCounter = 0;
        scaffoldDelayCounter = 0;
        targetY = 0.0;
        targetPos = null;

        // 总是禁用 Stuck（因为它总是由我们启用）
        Stuck stuck = (Stuck) myau.Myau.moduleManager.modules.get(Stuck.class);
        if (stuck != null) {
            stuck.setEnabled(false);
        } else {
            ChatUtil.sendFormatted("Module Stuck is null");
        }

        // 仅当 Blink 是由我们启用时才禁用它
        if (blinkEnabledByStuck) {
            Blink blink = (Blink) myau.Myau.moduleManager.modules.get(Blink.class);
            if (blink != null) {
                blink.setEnabled(false);
            } else {
                ChatUtil.sendFormatted("Module Blink is null");
            }
            blinkEnabledByStuck = false;
        }
    }

    // 检查是否需要寻找新目标
    private boolean shouldFindNewTarget() {
        return targetY != 0.0 && mc.thePlayer.posY < targetY - 0.5;
    }

    // 检查玩家是否在地面上
    private boolean isPlayerOnGround() {
        BlockPos playerPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.1, mc.thePlayer.posZ);
        return !BlockUtil.isReplaceable(playerPos);
    }

    // 构造函数
    public AntiVoid() {
        super("AntiVoid", false);
    }

    // 获取背包中方块数量
    private int getBlockCount() {
        int count = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.stackSize > 0) {
                net.minecraft.item.Item item = stack.getItem();
                if (item instanceof net.minecraft.item.ItemBlock) {
                    net.minecraft.block.Block block = ((net.minecraft.item.ItemBlock) item).getBlock();
                    if (!BlockUtil.isInteractable(block) && BlockUtil.isSolid(block)) {
                        count += stack.stackSize;
                    }
                }
            }
        }
        return count;
    }

    @EventTarget(Priority.LOWEST)
    public void onUpdate(PlayerUpdateEvent event) {
        if (this.isEnabled()) {
            Scaffold scaffoldModule = (Scaffold) myau.Myau.moduleManager.modules.get(Scaffold.class);
            if (scaffoldModule != null && scaffoldModule.isEnabled() && scaffoldModule.isTowering()) {
                return;
            }
            this.isInVoid = !mc.thePlayer.capabilities.allowFlying &&
                    !mc.thePlayer.onGround &&
                    isPlayerStuckInAir();

            if (this.mode.getValue() == 0) { //
                if (!this.isInVoid) {
                    this.resetBlink();
                }

                if (this.lastSafePosition != null) {
                    float subWidth = mc.thePlayer.width / 2.0F;
                    float height = mc.thePlayer.height;
                    if (PlayerUtil.checkInWater(
                            new AxisAlignedBB(
                                    this.lastSafePosition[0] - (double) subWidth,
                                    this.lastSafePosition[1],
                                    this.lastSafePosition[2] - (double) subWidth,
                                    this.lastSafePosition[0] + (double) subWidth,
                                    this.lastSafePosition[1] + (double) height,
                                    this.lastSafePosition[2] + (double) subWidth
                            )
                    )) {
                        this.resetBlink();
                    }
                }

                if (!this.wasInVoid && this.isInVoid && this.canUseAntiVoid()) {

                    myau.Myau.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);

                    if (myau.Myau.blinkManager.setBlinkState(true, BlinkModules.ANTI_VOID)) {
                        this.lastSafePosition = new double[]{
                                mc.thePlayer.prevPosX,
                                mc.thePlayer.prevPosY,
                                mc.thePlayer.prevPosZ
                        };
                    }
                }

                if (myau.Myau.blinkManager.getBlinkingModule() == BlinkModules.ANTI_VOID
                        && this.lastSafePosition != null
                        && this.lastSafePosition[1] - (double) this.distance.getValue() > mc.thePlayer.posY) {

                    myau.Myau.blinkManager
                            .blinkedPackets
                            .offerFirst(
                                    new C04PacketPlayerPosition(
                                            this.lastSafePosition[0],
                                            this.lastSafePosition[1] - RandomUtil.nextDouble(10.0, 20.0),
                                            this.lastSafePosition[2],
                                            false
                                    )
                            );
                    this.resetBlink();
                }
            } else if (this.mode.getValue() == 1) {
                if (scaffoldModule != null && scaffoldModule.isEnabled() && scaffoldModule.isTowering()) {
                    if (isStuckModeActive) {
                        deactivateStuckMode();
                    }
                    return; // 跳过后续所有 stuck 逻辑
                }// STUCK模式
                if (isPlayerStuckInAir()) {
                    if (getBlockCount() < minBlocks.getValue()) {
                        stuckTickCounter = 0;
                        targetPos = null;
                        targetY = 0.0;
                        if (isStuckModeActive) deactivateStuckMode();
                        return;
                    }

                    stuckTickCounter++;
                    if (stuckTickCounter >= stuckTickThreshold.getValue() && !isStuckModeActive) {
                        targetPos = findPlaceablePosition();
                        if (targetPos != null) {
                            targetY = targetPos.getY() + 0.35;
                        }
                    }

                    if (shouldFindNewTarget() && !isStuckModeActive) {
                        targetPos = findPlaceablePosition();
                        if (targetPos != null) {
                            targetY = targetPos.getY() + 0.35;
                        }
                    }

                    if (targetY != 0.0 &&
                            mc.thePlayer.posY >= targetY - 0.15 &&
                            mc.thePlayer.posY <= targetY + 0.35 &&
                            !isStuckModeActive) {
                        activateStuckMode();
                    }

                    if (isStuckModeActive) {
                        if (scaffoldDelayCounter > 0) {
                            scaffoldDelayCounter--;
                            if (scaffoldDelayCounter == 0) {
                                shouldActivateScaffold = true;
                            }
                        }

                        if (shouldActivateScaffold) {
                            // 获取Scaffold模块
                            Scaffold scaffold = (Scaffold) myau.Myau.moduleManager.modules.get(Scaffold.class);
                            if (scaffold != null && !scaffold.isEnabled()) {
                                scaffold.setEnabled(true);
                            }

                            if (isPlayerOnGround()) {
                                if (debug.getValue()) {
                                    ChatUtil.sendFormatted("Successfully Clutched!");
                                }
                                deactivateStuckMode();
                            } else {
                                return;
                            }
                        }
                    }
                } else {
                    stuckTickCounter = 0;
                    targetPos = null;
                    targetY = 0.0;
                    if (isStuckModeActive) {
                        deactivateStuckMode();
                    }
                }
            }

            this.wasInVoid = this.isInVoid;
        }
    }


    @EventTarget
    public void onKey(KeyEvent event) {
        if (event.getKey() == mc.gameSettings.keyBindUseItem.getKeyCode()) {
            ItemStack currentItem = mc.thePlayer.inventory.getCurrentItem();
            if (currentItem != null && currentItem.getItem() instanceof ItemEnderPearl) {
                this.resetBlink();
            }
        }
    }

    @Override
    public void onEnabled() {
        this.isInVoid = false;
        this.wasInVoid = false;
        this.targetPos = null;
        this.resetBlink();
    }

    @Override
    public void onDisabled() {
        this.targetPos = null;
        myau.Myau.blinkManager.setBlinkState(false, BlinkModules.ANTI_VOID);
        deactivateStuckMode(); // 清理 stuck 模式，关闭可能启用的模块
    }

    @Override
    public void verifyValue(String mode) {
        if (this.isEnabled()) {
            this.onDisabled();
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }
}