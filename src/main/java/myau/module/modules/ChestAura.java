package myau.module.modules;

import net.minecraft.block.BlockChest;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S24PacketBlockAction;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.*;
import myau.mixin.IAccessorRenderManager;
import myau.module.Module;
import myau.module.modules.KillAura;
import myau.module.modules.Scaffold;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import myau.util.MoveUtil;
import myau.util.RenderUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ChestAura extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final FloatProperty range = new FloatProperty("range", 4.0f, 1.0f, 6.0f);
    public final BooleanProperty throughWalls = new BooleanProperty("through-walls", true);
    public final ModeProperty moveFix = new ModeProperty("move-fix", 1, new String[]{"None", "Silent", "Strict"});
    public final BooleanProperty noWorkWhenScaffold = new BooleanProperty("no-work-when-scaffold", true);

    private final List<BlockPos> openedChests = new ArrayList<>();
    private TileEntityChest targetChest;
    private float[] rotations;
    private boolean isRotating;

    public ChestAura() {
        super("ChestAura", false);
    }

    @EventTarget
    public void onWorldLoad(LoadWorldEvent event) {
        openedChests.clear();
    }

    private void addOpenedChest(BlockPos pos) {
        if (!openedChests.contains(pos)) {
            openedChests.add(pos);
        }
        net.minecraft.block.Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block instanceof BlockChest) {
            for (EnumFacing facing : EnumFacing.HORIZONTALS) {
                BlockPos neighbor = pos.offset(facing);
                if (mc.theWorld.getBlockState(neighbor).getBlock() == block) {
                    if (!openedChests.contains(neighbor)) {
                        openedChests.add(neighbor);
                    }
                }
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{moveFix.getModeString()};
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!isEnabled()) return;
        if (event.getPacket() instanceof S24PacketBlockAction) {
            S24PacketBlockAction packet = (S24PacketBlockAction) event.getPacket();
            if (packet.getData2() == 1) {
                addOpenedChest(packet.getBlockPosition());
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!isEnabled()) return;
        if (event.getType() != EventType.PRE) return;

        // 检查 Scaffold 是否启用
        if (noWorkWhenScaffold.getValue()) {
            Scaffold scaffold = (Scaffold) Myau.moduleManager.getModule(Scaffold.class);
            if (scaffold != null && scaffold.isEnabled()) {
                targetChest = null;
                isRotating = false;
                return;
            }
        }

        // KillAura 检查
        KillAura killAura = (KillAura) Myau.moduleManager.getModule(KillAura.class);
        if (killAura != null && killAura.isEnabled() && killAura.getTarget() != null) {
            targetChest = null;
            isRotating = false;
            return;
        }

        // 更新已开启的箱子列表
        for (TileEntity tileEntity : mc.theWorld.loadedTileEntityList) {
            if (tileEntity instanceof TileEntityChest) {
                TileEntityChest chest = (TileEntityChest) tileEntity;
                if (chest.numPlayersUsing > 0) {
                    addOpenedChest(chest.getPos());
                }
            }
        }

        targetChest = getClosestChest();
        isRotating = false;

        if (targetChest != null) {
            double x = targetChest.getPos().getX() + 0.5 - mc.thePlayer.posX;
            double y = targetChest.getPos().getY() + 0.5 - mc.thePlayer.posY - mc.thePlayer.getEyeHeight();
            double z = targetChest.getPos().getZ() + 0.5 - mc.thePlayer.posZ;
            double dist = Math.sqrt(x * x + z * z);

            float yaw = (float) (Math.atan2(z, x) * 180.0 / Math.PI) - 90.0f;
            float pitch = (float) -(Math.atan2(y, dist) * 180.0 / Math.PI);

            rotations = new float[]{yaw, pitch};

            event.setRotation(rotations[0], rotations[1], 1);
            mc.thePlayer.rotationYawHead = rotations[0];
            mc.thePlayer.renderYawOffset = rotations[0];
            isRotating = true;

            if (this.moveFix.getValue() != 0) {
                event.setPervRotation(rotations[0], 1);
            }

            // 尝试打开箱子
            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld,
                    mc.thePlayer.inventory.getCurrentItem(),
                    targetChest.getPos(), EnumFacing.UP,
                    new Vec3(targetChest.getPos().getX(), targetChest.getPos().getY(), targetChest.getPos().getZ()))) {
                mc.thePlayer.swingItem();
                addOpenedChest(targetChest.getPos());
            }
        }
    }

    @EventTarget
    public void onMove(MoveInputEvent event) {
        if (!isEnabled()) return;

        if (noWorkWhenScaffold.getValue()) {
            Scaffold scaffold = (Scaffold) Myau.moduleManager.getModule(Scaffold.class);
            if (scaffold != null && scaffold.isEnabled()) return;
        }

        KillAura killAura = (KillAura) Myau.moduleManager.getModule(KillAura.class);
        if (killAura != null && killAura.isEnabled() && killAura.getTarget() != null) return;

        if (isRotating && targetChest != null) {
            if (this.moveFix.getValue() == 1 && MoveUtil.isForwardPressed()) {
                MoveUtil.fixStrafe(rotations[0]);
            }
        }
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (!isEnabled()) return;

        RenderUtil.enableRenderState();
        for (TileEntity tileEntity : mc.theWorld.loadedTileEntityList) {
            if (tileEntity instanceof TileEntityChest) {
                TileEntityChest chest = (TileEntityChest) tileEntity;
                BlockPos pos = chest.getPos();

                boolean isOpened = openedChests.contains(pos);
                Color color = isOpened ? new Color(255, 0, 0, 100) : new Color(0, 255, 0, 100);

                double minX = 0.0625;
                double minZ = 0.0625;
                double maxX = 0.9375;
                double maxZ = 0.9375;

                if (mc.theWorld.getBlockState(pos).getBlock() instanceof BlockChest) {
                    BlockChest block = (BlockChest) mc.theWorld.getBlockState(pos).getBlock();
                    EnumFacing facing = mc.theWorld.getBlockState(pos).getValue(BlockChest.FACING);

                    switch (facing) {
                        case NORTH:
                            if (mc.theWorld.getBlockState(pos.east()).getBlock() == block) continue;
                            else if (mc.theWorld.getBlockState(pos.west()).getBlock() == block) minX -= 1;
                            break;
                        case SOUTH:
                            if (mc.theWorld.getBlockState(pos.west()).getBlock() == block) continue;
                            else if (mc.theWorld.getBlockState(pos.east()).getBlock() == block) minX -= 1;
                            break;
                        case EAST:
                            if (mc.theWorld.getBlockState(pos.south()).getBlock() == block) continue;
                            else if (mc.theWorld.getBlockState(pos.north()).getBlock() == block) minZ -= 1;
                            break;
                        case WEST:
                            if (mc.theWorld.getBlockState(pos.north()).getBlock() == block) continue;
                            else if (mc.theWorld.getBlockState(pos.south()).getBlock() == block) minZ -= 1;
                            break;
                    }
                }

                double renderX = ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
                double renderY = ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY();
                double renderZ = ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();

                AxisAlignedBB aabb = new AxisAlignedBB(
                        pos.getX() + minX,
                        pos.getY(),
                        pos.getZ() + minZ,
                        pos.getX() + maxX,
                        pos.getY() + 0.875,
                        pos.getZ() + maxZ
                ).offset(-renderX, -renderY, -renderZ);

                drawFilledBoxAlpha(aabb, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
            }
        }
        RenderUtil.disableRenderState();
    }

    private TileEntityChest getClosestChest() {
        List<TileEntityChest> chests = mc.theWorld.loadedTileEntityList.stream()
                .filter(e -> e instanceof TileEntityChest)
                .map(e -> (TileEntityChest) e)
                .filter(e -> !openedChests.contains(e.getPos()))
                .filter(e -> mc.thePlayer.getDistanceSq(e.getPos()) <= range.getValue() * range.getValue())
                .filter(e -> throughWalls.getValue() || mc.thePlayer.canEntityBeSeen(
                        new net.minecraft.entity.item.EntityItem(mc.theWorld, e.getPos().getX(), e.getPos().getY(), e.getPos().getZ())))
                .sorted(Comparator.comparingDouble(e -> mc.thePlayer.getDistanceSq(e.getPos())))
                .collect(Collectors.toList());

        return chests.isEmpty() ? null : chests.get(0);
    }

    private static void drawFilledBoxAlpha(AxisAlignedBB bb, int red, int green, int blue, int alpha) {
        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);

        worldRenderer.pos(bb.minX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(bb.minX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(bb.maxX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(bb.maxX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();

        worldRenderer.pos(bb.minX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(bb.minX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(bb.maxX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(bb.maxX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();

        worldRenderer.pos(bb.minX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(bb.minX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(bb.maxX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(bb.maxX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();

        worldRenderer.pos(bb.minX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(bb.minX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(bb.maxX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(bb.maxX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();

        worldRenderer.pos(bb.minX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(bb.minX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(bb.minX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(bb.minX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();

        worldRenderer.pos(bb.maxX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(bb.maxX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(bb.maxX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(bb.maxX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();

        tessellator.draw();
    }
}