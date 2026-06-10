
package myau.util;

import myau.Myau;
import myau.management.RotationState;
import myau.module.modules.TargetStrafe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovementInput;

public class MoveUtil {
   private static final Minecraft mc = Minecraft.getMinecraft();

   public static boolean isMoving() {
      return isMoving(mc.thePlayer);
   }

   public static boolean isMoving(EntityLivingBase entity) {
      return entity != null && (entity.moveForward != 0.0F || entity.moveStrafing != 0.0F);
   }

   public static boolean isForwardPressed() {
      if (mc.gameSettings.keyBindForward.isKeyDown() != mc.gameSettings.keyBindBack.isKeyDown()) {
         return true;
      } else {
         return mc.gameSettings.keyBindLeft.isKeyDown() != mc.gameSettings.keyBindRight.isKeyDown();
      }
   }

   public static int getForwardValue() {
      int forwardValue = 0;
      if (mc.gameSettings.keyBindForward.isKeyDown()) {
         ++forwardValue;
      }

      if (mc.gameSettings.keyBindBack.isKeyDown()) {
         --forwardValue;
      }

      return forwardValue;
   }

   public static int getLeftValue() {
      int leftValue = 0;
      if (mc.gameSettings.keyBindLeft.isKeyDown()) {
         ++leftValue;
      }

      if (mc.gameSettings.keyBindRight.isKeyDown()) {
         --leftValue;
      }

      return leftValue;
   }

   public static float getMoveYaw() {
      return adjustYaw(RotationState.isActived() ? RotationState.getSmoothedYaw() : mc.thePlayer.rotationYaw, mc.thePlayer.movementInput.moveForward, mc.thePlayer.movementInput.moveStrafe);
   }

   public static float adjustYaw(float yaw, float forward, float strafe) {
      TargetStrafe targetStrafe = (TargetStrafe)Myau.moduleManager.modules.get(TargetStrafe.class);
      if (targetStrafe.isEnabled() && !Float.isNaN(targetStrafe.getTargetYaw())) {
         return targetStrafe.getTargetYaw();
      } else {
         if (forward < 0.0F) {
            yaw += 180.0F;
         }

         if (strafe != 0.0F) {
            float multiplier = forward == 0.0F ? 1.0F : 0.5F * Math.signum(forward);
            yaw += -90.0F * multiplier * Math.signum(strafe);
         }

         return MathHelper.wrapAngleTo180_float(yaw);
      }
   }

   public static float getDirectionYaw() {
      return getSpeed() == 0.0D ? MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) : MathHelper.wrapAngleTo180_float((float)Math.toDegrees(Math.atan2(mc.thePlayer.motionZ, mc.thePlayer.motionX)) - 90.0F);
   }

   public static double getBaseMoveSpeed() {
      double baseSpeed = 0.28015D;
      if (getSpeedTime() > 0) {
         baseSpeed = 0.28015D * (1.0D + 0.15D * (double)getSpeedLevel());
      }

      return baseSpeed;
   }

   public static double getBaseJumpHigh(int speedLevel) {
      double jumpHeight = 0.452D;
      if (speedLevel == 1) {
         jumpHeight = 0.49720000000000003D;
      } else if (speedLevel >= 2) {
         jumpHeight *= 1.2D;
      }

      return jumpHeight;
   }

   public static double getJumpMotion() {
      int speedLevel = 0;
      if (getSpeedTime() > 0) {
         speedLevel = getSpeedLevel();
      }

      return getBaseJumpHigh(speedLevel);
   }

   public static double getSpeed() {
      return getSpeed(mc.thePlayer.motionX, mc.thePlayer.motionZ);
   }

   public static double getSpeed(double motionX, double motionZ) {
      return Math.hypot(motionX, motionZ);
   }

   public static void setSpeed(double speed) {
      setSpeed(speed, getDirectionYaw());
   }

   public static void setSpeed(double speed, float yaw) {
      mc.thePlayer.motionX = -Math.sin(Math.toRadians((double)yaw)) * speed;
      mc.thePlayer.motionZ = Math.cos(Math.toRadians((double)yaw)) * speed;
   }

   public static void setSpeed(double speed, float yaw, double strafe, double forward) {
      if (forward != 0.0D) {
         if (strafe > 0.0D) {
            yaw += (float)(forward > 0.0D ? -45 : 45);
         } else if (strafe < 0.0D) {
            yaw += (float)(forward > 0.0D ? 45 : -45);
         }

         strafe = 0.0D;
         if (forward > 0.0D) {
            forward = 1.0D;
         } else if (forward < 0.0D) {
            forward = -1.0D;
         }
      }

      if (strafe > 0.0D) {
         strafe = 1.0D;
      } else if (strafe < 0.0D) {
         strafe = -1.0D;
      }

      double cos = Math.cos(Math.toRadians((double)(yaw + 90.0F)));
      double sin = Math.sin(Math.toRadians((double)(yaw + 90.0F)));
      mc.thePlayer.motionX = forward * speed * cos + strafe * speed * sin;
      mc.thePlayer.motionZ = forward * speed * sin - strafe * speed * cos;
   }

   public static void stop() {
      mc.thePlayer.motionX = 0.0D;
      mc.thePlayer.motionZ = 0.0D;
   }

   public static float getRawDirection() {
      return getRawDirectionRotation(mc.thePlayer.rotationYaw, mc.thePlayer.movementInput.moveStrafe, mc.thePlayer.movementInput.moveForward);
   }

   public static float getRawDirectionRotation(float yaw, float strafe, float forward) {
      float direction = yaw;
      if (forward < 0.0F) {
         direction = yaw + 180.0F;
      }

      float multiplier = 1.0F;
      if (forward < 0.0F) {
         multiplier = -0.5F;
      } else if (forward > 0.0F) {
         multiplier = 0.5F;
      }

      if (strafe > 0.0F) {
         direction -= 90.0F * multiplier;
      }

      if (strafe < 0.0F) {
         direction += 90.0F * multiplier;
      }

      return direction;
   }

   public static void addSpeed(double speed, float yaw) {
      EntityPlayerSP var10000 = mc.thePlayer;
      var10000.motionX += -Math.sin(Math.toRadians((double)yaw)) * speed;
      var10000 = mc.thePlayer;
      var10000.motionZ += Math.cos(Math.toRadians((double)yaw)) * speed;
   }

   public static int getSpeedLevel() {
      int speedLevel = 0;
      if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
         speedLevel = mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1;
      }

      return speedLevel;
   }

   public static int getSpeedTime() {
      return mc.thePlayer.isPotionActive(Potion.moveSpeed) ? mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getDuration() : 0;
   }

   public static float getAllowedHorizontalDistance() {
      float slipperiness = mc.thePlayer.worldObj.getBlockState(new BlockPos(MathHelper.floor_double(mc.thePlayer.posX), MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().minY) - 1, MathHelper.floor_double(mc.thePlayer.posZ))).getBlock().slipperiness * 0.91F;
      return mc.thePlayer.getAIMoveSpeed() * (0.16277136F / (slipperiness * slipperiness * slipperiness));
   }

   public static double[] predictMovement() {
      float strafeInput = (float)getLeftValue() * 0.98F;
      float forwardInput = (float)getForwardValue() * 0.98F;
      float inputMagnitude = strafeInput * strafeInput + forwardInput * forwardInput;
      if (inputMagnitude >= 1.0E-4F) {
         inputMagnitude = MathHelper.sqrt_float(inputMagnitude);
         if (inputMagnitude < 1.0F) {
            inputMagnitude = 1.0F;
         }

         inputMagnitude = getAllowedHorizontalDistance() / inputMagnitude;
         float sinYaw = MathHelper.sin(mc.thePlayer.rotationYaw * 3.1415927F / 180.0F);
         float cosYaw = MathHelper.cos(mc.thePlayer.rotationYaw * 3.1415927F / 180.0F);
         strafeInput *= inputMagnitude;
         forwardInput *= inputMagnitude;
         return new double[]{(double)(strafeInput * cosYaw - forwardInput * sinYaw), (double)(forwardInput * cosYaw + strafeInput * sinYaw)};
      } else {
         return new double[]{0.0D, 0.0D};
      }
   }

   public static void fixStrafe(float targetYaw) {
      float angle = MathHelper.wrapAngleTo180_float(adjustYaw(mc.thePlayer.rotationYaw, (float)getForwardValue(), (float)getLeftValue()) - targetYaw + 22.5F);
      switch((int)(angle + 180.0F) / 45 % 8) {
      case 0:
         mc.thePlayer.movementInput.moveForward = -1.0F;
         mc.thePlayer.movementInput.moveStrafe = 0.0F;
         break;
      case 1:
         mc.thePlayer.movementInput.moveForward = -1.0F;
         mc.thePlayer.movementInput.moveStrafe = 1.0F;
         break;
      case 2:
         mc.thePlayer.movementInput.moveForward = 0.0F;
         mc.thePlayer.movementInput.moveStrafe = 1.0F;
         break;
      case 3:
         mc.thePlayer.movementInput.moveForward = 1.0F;
         mc.thePlayer.movementInput.moveStrafe = 1.0F;
         break;
      case 4:
         mc.thePlayer.movementInput.moveForward = 1.0F;
         mc.thePlayer.movementInput.moveStrafe = 0.0F;
         break;
      case 5:
         mc.thePlayer.movementInput.moveForward = 1.0F;
         mc.thePlayer.movementInput.moveStrafe = -1.0F;
         break;
      case 6:
         mc.thePlayer.movementInput.moveForward = 0.0F;
         mc.thePlayer.movementInput.moveStrafe = -1.0F;
         break;
      case 7:
         mc.thePlayer.movementInput.moveForward = -1.0F;
         mc.thePlayer.movementInput.moveStrafe = -1.0F;
      }

      if (mc.thePlayer.movementInput.sneak) {
         MovementInput var10000 = mc.thePlayer.movementInput;
         var10000.moveForward *= 0.3F;
         var10000 = mc.thePlayer.movementInput;
         var10000.moveStrafe *= 0.3F;
      }

   }
}
