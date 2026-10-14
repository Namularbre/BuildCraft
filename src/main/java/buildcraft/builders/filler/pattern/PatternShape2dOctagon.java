package buildcraft.builders.filler.pattern;

/** An octagon: a rectangle with its corners cut off. (The 1.12 version had no shape.) */
public class PatternShape2dOctagon extends PatternShape2d {
    /** How much of each side is cut off at both ends, for a regular octagon in a square: 1 / (2 + sqrt(2)). */
    private static final double CUT = 1 / (2 + Math.sqrt(2));

    public PatternShape2dOctagon() {
        super("2d_octagon");
    }

    @Override
    protected void genShape(int maxA, int maxB, LineList list) {
        int cutA = (int) Math.round(maxA * CUT);
        int cutB = (int) Math.round(maxB * CUT);
        list.moveTo(cutA, 0);
        list.lineTo(maxA - cutA, 0);
        list.lineTo(maxA, cutB);
        list.lineTo(maxA, maxB - cutB);
        list.lineTo(maxA - cutA, maxB);
        list.lineTo(cutA, maxB);
        list.lineTo(0, maxB - cutB);
        list.lineTo(0, cutB);
        list.lineTo(cutA, 0);
    }
}
