package dev.gumark.oresim;

/**
 * Everything the overlay can show. The ten ore types come from the seed
 * simulation; water and lava are revealed from the real world.
 */
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
    DEBRIS("Ancient Debris", 0xFFD11BF5),
    WATER("Water", 0xFF3D6EFF),
    LAVA("Lava", 0xFFFF6A00);

    public final String label;
    public final int color;

    OreType(String label, int color) {
        this.label = label;
        this.color = color;
    }
}
