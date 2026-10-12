package dev.gumark.oresim;

/** Everything the overlay can show; all types come from the seed simulation. */
public enum OreType {
    COAL("Coal", 0xFF2F2C36),
    IRON("Iron", 0xFFECAD77),
    GOLD("Gold", 0xFFF7E51E),
    REDSTONE("Redstone", 0xFFF50717),
    DIAMOND("Diamond", 0xFF21F4FF),
    LAPIS("Lapis", 0xFF081ABD),
    COPPER("Copper", 0xFFEF9700),
    EMERALD("Emerald", 0xFF1BD12D),
    QUARTZ("Quartz", 0xFFCDCDCD),
    DEBRIS("Ancient Debris", 0xFFD11BF5);

    public final String label;
    public final int color;

    OreType(String label, int color) {
        this.label = label;
        this.color = color;
    }
}
