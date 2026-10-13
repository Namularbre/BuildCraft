package buildcraft.builders.filler.pattern;


import buildcraft.api.filler.IFilledTemplate;
import buildcraft.api.filler.IFillerPatternShape;
import buildcraft.api.statements.IStatementParameter;



public class PatternNone extends Pattern implements IFillerPatternShape {
    public PatternNone() {
        super("none");
    }


    @Override
    public boolean fillTemplate(IFilledTemplate filledTemplate, IStatementParameter[] params) {
        return false;
    }
}
