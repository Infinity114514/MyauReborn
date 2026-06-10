package myau.module.modules;


import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;

public class Animations extends Module {

    public final ModeProperty mode = new ModeProperty(
            "mode",
            2,
            new String[]{
                    "Test",
                    "1_8",
                    "Hide",
                    "Swing",
                    "Old",
                    "Push",
                    "Dash",
                    "Slash",
                    "Slide",
                    "Scale",
                    "Swank",
                    "Swang",
                    "Swonk",
                    "Stella",
                    "Small",
                    "Edit",
                    "Rhys",
                    "Stab",
                    "Float",
                    "Remix",
                    "Avatar",
                    "Xiv",
                    "Winter",
                    "Yamato",
                    "SlideSwing",
                    "SmallPush",
                    "Reverse",
                    "Invent",
                    "Leaked",
                    "Aqua",
                    "Astro",
                    "Fadeaway",
                    "Astolfo",
                    "AstolfoSpin",
                    "Moon",
                    "MoonPush",
                    "Smooth",
                    "Jigsaw",
                    "Tap1",
                    "Tap2",
                    "Sigma3",
                    "Sigma4"
            }
    );
    public final BooleanProperty cancelEquip = new BooleanProperty("cancelequip", false);
    public final BooleanProperty cancelEquipBlockingOnly = new BooleanProperty("cancelequip-blockingonly", true, () -> this.cancelEquip.getValue());
    public final FloatProperty itemSize = new FloatProperty("item-size", 0.0F, -0.5F, 0.5F);
    public final FloatProperty itemFov = new FloatProperty("item-fov", 0.0F, -5.0F, 5.0F);
    public final FloatProperty itemPosX = new FloatProperty("itempos-x", 0.0F, -1.0F, 1.0F);
    public final FloatProperty itemPosY = new FloatProperty("itempos-y", 0.0F, -1.0F, 1.0F);
    public final FloatProperty itemPosZ = new FloatProperty("itempos-z", 0.0F, -1.0F, 1.0F);
    public final FloatProperty blockPosX = new FloatProperty("blockpos-x", 0.0F, -1.0F, 1.0F);
    public final FloatProperty blockPosY = new FloatProperty("blockpos-y", 0.0F, -1.0F, 1.0F);
    public final FloatProperty blockPosZ = new FloatProperty("blockpos-z", 0.0F, -1.0F, 1.0F);

    public Animations() {
        super("Animations", false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
