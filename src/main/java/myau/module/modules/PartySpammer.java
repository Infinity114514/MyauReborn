package myau.module.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import myau.event.EventTarget;
import myau.events.TickEvent;
import myau.event.types.EventType;
import myau.module.Module;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.TextProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PartySpammer extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"All", "Single"});
    private final IntProperty delayTicks = new IntProperty("DelayTicks", 5, 1, 10);
    private final TextProperty ign = new TextProperty("IGN", "", () -> mode.getValue() == 1);

    private List<String> allPlayers = new ArrayList<>();
    private List<String> invitedThisRound = new ArrayList<>();
    private List<String> currentBatch = new ArrayList<>();
    private int step = 0;
    private int tickCounter = 0;

    public PartySpammer() {
        super("PartySpammer", false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{mode.getModeString()};
    }

    @Override
    public void onEnabled() {
        resetState();
        refreshPlayerList();
        tickCounter = 0;
        step = 0;
    }

    @Override
    public void onDisabled() {
        resetState();
    }

    private void resetState() {
        allPlayers.clear();
        invitedThisRound.clear();
        currentBatch.clear();
        step = 0;
        tickCounter = 0;
    }

    private void refreshPlayerList() {
        if (mc.theWorld == null) return;
        allPlayers = mc.theWorld.playerEntities.stream()
                .filter(entity -> entity != mc.thePlayer)
                .map(EntityPlayer::getName)
                .collect(Collectors.toList());
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) return;
        if (!isEnabled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        tickCounter++;
        if (tickCounter < delayTicks.getValue()) return;
        tickCounter = 0;

        if (mode.getValue() == 1) {
            executeSingle();
        } else {
            executeAll();
        }
    }

    private void executeSingle() {
        String target = ign.getValue().trim();
        if (target.isEmpty()) return;
        sendCommand("/party invite " + target);
        sendCommand("/party disband");
    }

    private void executeAll() {
        if (currentBatch.isEmpty() || step >= currentBatch.size()) {
            if (!currentBatch.isEmpty() && step == currentBatch.size()) {
                sendCommand("/party disband");
                step++;
                return;
            }
            prepareNextBatch();
            if (currentBatch.isEmpty()) return;
            step = 0;
        }

        if (step < currentBatch.size()) {
            String playerName = currentBatch.get(step);
            sendCommand("/party invite " + playerName);
            invitedThisRound.add(playerName);
            step++;
        } else if (step == currentBatch.size()) {
            sendCommand("/party disband");
            step++;
        }
    }

    private void prepareNextBatch() {
        if (allPlayers.isEmpty()) {
            refreshPlayerList();
            if (allPlayers.isEmpty()) return;
        }

        List<String> available = allPlayers.stream()
                .filter(name -> !invitedThisRound.contains(name))
                .collect(Collectors.toList());

        if (available.isEmpty()) {
            invitedThisRound.clear();
            available = new ArrayList<>(allPlayers);
        }

        currentBatch.clear();
        int batchSize = Math.min(4, available.size());
        for (int i = 0; i < batchSize; i++) {
            currentBatch.add(available.get(i));
        }
    }

    private void sendCommand(String command) {
        if (mc.thePlayer != null) {
            mc.thePlayer.sendChatMessage(command);
        }
    }
}