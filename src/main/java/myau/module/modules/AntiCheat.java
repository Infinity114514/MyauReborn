

package myau.module.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import myau.Myau;
import myau.module.Module;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class AntiCheat extends Module {
   private static final String CHEAT_AUTOBLOCK = "AutoBlock";
   private static final String CHEAT_NOSLOW = "Noslow";
   private static final String CHEAT_KILLAURA = "KillAura";
   private static final String CHEAT_SCAFFOLD = "Scaffold";
   private static final Minecraft mc = Minecraft.getMinecraft();
   public static boolean acflag = true;
   public static final List<String> whitelist = new ArrayList();
   private static final Map<String, int[]> flagMap = new HashMap();

   public AntiCheat() {
      super("AntiCheat", false);
   }

   public static void receiveSignal(String playerName, String cheatName) {
      if (acflag) {
         if (!whitelist.contains(playerName)) {
            if (cheatName.equals("AutoBlock") || cheatName.equals("Noslow") || cheatName.equals("KillAura") || cheatName.equals("Scaffold")) {
               int[] flagData = (int[])flagMap.getOrDefault(playerName, new int[]{0, (int)(Minecraft.getMinecraft().theWorld.getTotalWorldTime() / 20L)});
               int var10002 = flagData[0]++;
               flagData[1] = (int)(Minecraft.getMinecraft().theWorld.getTotalWorldTime() / 20L);
               flagMap.put(playerName, flagData);
               int MAX_FLAG_COUNT = 2;
               if (cheatName.equals("AutoBlock")) {
                  MAX_FLAG_COUNT = 5;
               }

               if (cheatName.equals("Noslow")) {
                  MAX_FLAG_COUNT = 3;
               }

               if (cheatName.equals("KillAura")) {
                  MAX_FLAG_COUNT = 4;
               }

               if (cheatName.equals("Scaffold")) {
                  MAX_FLAG_COUNT = 4;
               }

               if (flagData[0] >= MAX_FLAG_COUNT) {
                  ChatUtil.sendFormatted(String.format("%s%s%s%s failed %s%s", Myau.clientName, EnumChatFormatting.RED, playerName, EnumChatFormatting.GRAY, EnumChatFormatting.RED, cheatName));
                  mc.thePlayer.playSound("random.orb", 0.3F, 1.0F);
                  String added = Myau.targetManager.add(playerName);
                  if (added != null) {
                     ChatUtil.sendFormatted(String.format("%sAdded &o%s&r to your enemy list&r", Myau.clientName, added));
                  }
               }

            }
         }
      }
   }

   @SubscribeEvent
   public void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END && Minecraft.getMinecraft().theWorld != null) {
         int currentTimeInSeconds = (int)(Minecraft.getMinecraft().theWorld.getTotalWorldTime() / 20L);

         Iterator<Entry<String, int[]>> it = flagMap.entrySet().iterator();
         while (it.hasNext()) {
            Entry<String, int[]> entry = it.next();
            int[] flagData = entry.getValue();

            int timeDiff = currentTimeInSeconds - flagData[1];
            if (timeDiff > 0) {
               flagData[0] -= timeDiff;          // 每秒减 1（若跳过秒则一次减多）
               flagData[1] = currentTimeInSeconds; // 更新时间基准
            }
            if (flagData[0] <= 0) {
               it.remove();
            }
         }
      }
   }
}
