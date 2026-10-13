package buildcraft.builders.filler.pattern;



public class PatternShape2dSemiCircle extends PatternShape2d {
    public PatternShape2dSemiCircle() {
        super("2d_semi_circle");
    }


    @Override
    protected void genShape(int maxA, int maxB, LineList list) {
        if (maxA == 0 || maxB == 0) {
            list.moveTo(0, 0);
            list.lineTo(maxA, maxB);
            return;
        }
        int halfA = maxA / 2;
        int halfAUpper = maxA - halfA;
        list.setFillPoint(halfA, maxB);
        list.arc(halfA, maxB, maxA / 2.0, maxB, halfAUpper - halfA, 0, ArcType.SEMI_CIRCLE);
    }
}
