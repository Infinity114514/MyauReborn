
package myau.module.modules;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.module.Module;
import myau.property.properties.ModeProperty;
import myau.util.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;

public class Disabler extends Module {
   private static final Minecraft mc = Minecraft.getMinecraft();
   public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"PredictionInventory"});
   private final List<Packet<?>> inventoryPackets = new ArrayList();

   public Disabler() {
      super("Disabler", false);
   }

   public void onEnabled() {
      String currentMode = this.mode.getModeString();
      if (currentMode.equals("PredictionInventory")) {
      }

      this.resetStates();
   }

   public void onDisabled() {
      if (!this.inventoryPackets.isEmpty()) {
         Iterator var1 = this.inventoryPackets.iterator();

         while(var1.hasNext()) {
            Packet<?> p = (Packet)var1.next();
            PacketUtil.sendPacketNoEvent(p);
         }

         this.inventoryPackets.clear();
      }

      this.resetStates();
   }

   private void resetStates() {
      this.inventoryPackets.clear();
   }

   @EventTarget
   public void onPacket(PacketEvent event) {
      if (this.isEnabled()) {
         if (event.getType() == EventType.SEND) {
            this.handlePredictionInventory(event);
         }

      }
   }

   public String[] getSuffix() {
      return new String[]{"Prediction"};
   }

   private void handlePredictionInventory(PacketEvent event) {
      if (this.mode.getModeString().equals("PredictionInventory")) {
         Packet<?> packet = event.getPacket();
         if (!(packet instanceof C16PacketClientStatus) && !(packet instanceof C0EPacketClickWindow)) {
            if (packet instanceof C0DPacketCloseWindow) {
               Iterator var3 = this.inventoryPackets.iterator();

               while(var3.hasNext()) {
                  Packet<?> p = (Packet)var3.next();
                  PacketUtil.sendPacketNoEvent(p);
               }

               this.inventoryPackets.clear();
            }
         } else {
            event.setCancelled(true);
            this.inventoryPackets.add(packet);
         }

      }
   }
}
