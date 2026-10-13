package buildcraft.builders.filler.pattern;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import buildcraft.api.filler.IFilledTemplate;
import buildcraft.api.filler.IFillerPatternShape;
import buildcraft.api.statements.IStatementParameter;

import buildcraft.builders.filler.Template;
import buildcraft.builders.filler.pattern.parameter.PatternParameterFacing;
import buildcraft.builders.filler.pattern.parameter.PatternParameterHollow;
import buildcraft.builders.filler.pattern.parameter.PatternParameterRotation;

public final class PatternSpherePart extends Pattern implements IFillerPatternShape {
    public enum SpherePartType {
        EIGHTH(3),
        QUARTER(2),
        HALF(1);

        public final String lowerCaseName = name().toLowerCase(Locale.ROOT);
        final int openFaces;

        SpherePartType(int numOpenFaces) {
            this.openFaces = numOpenFaces;
        }
    }

    private final SpherePartType type;

    public PatternSpherePart(SpherePartType type) {
        super("sphere_" + type.lowerCaseName);
        this.type = type;
    }

    @Override
    public int minParameters() {
        return type.openFaces == 1 ? 2 : 3;
    }

    @Override
    public int maxParameters() {
        return minParameters();
    }

    @Override
    public IStatementParameter createParameter(int index) {
        if (index >= minParameters()) {
            return null;
        }
        switch (index) {
            case 0:
                return PatternParameterHollow.FILLED_INNER;
            case 1:
                return PatternParameterFacing.DOWN;
            case 2:
                return PatternParameterRotation.NONE;
            default:
                return null;
        }
    }


    @Override
    public boolean fillTemplate(IFilledTemplate filledTemplate, IStatementParameter[] params) {
        PatternParameterFacing facing = getParam(1, params, PatternParameterFacing.DOWN);
        PatternParameterRotation rotation = getParam(2, params, PatternParameterRotation.NONE);
        PatternParameterHollow hollow = getParam(0, params, PatternParameterHollow.FILLED_INNER);

        Vec3 center;
        Vec3 radius;

        Set<Direction> innerSides = EnumSet.noneOf(Direction.class);

        Vec3 max = Vec3.atLowerCornerOf(filledTemplate.getMax());
        center = scale(max, 0.5);
        radius = center.add(0.5, 0.5, 0.5);

        innerSides.add(facing.face);

        Direction.Axis axis = facing.face.getAxis();
        Vec3 offset = relative(Vec3.ZERO, facing.face, getValue(radius, axis));
        center = center.add(offset);
        radius = replaceValue(radius, axis, getValue(radius, axis) * 2);

        if (type.openFaces > 1) {
            Direction.Axis secondaryAxis;
            if (rotation.rotationCount % 2 == 1) {
                secondaryAxis = next(axis);
            } else {
                secondaryAxis = previous(axis);
            }
            Direction secondaryFace = getFacing(secondaryAxis, rotation.rotationCount >= 2);
            innerSides.add(secondaryFace);

            offset = relative(Vec3.ZERO, secondaryFace, getValue(radius, secondaryAxis));
            center = center.add(offset);
            radius = replaceValue(radius, secondaryAxis, getValue(radius, secondaryAxis) * 2);

            if (type.openFaces > 2) {
                Direction.Axis tertiaryAxis;
                int rotationCount = (rotation.rotationCount + 1) & 3;
                if (rotationCount % 2 == 1) {
                    tertiaryAxis = next(axis);
                } else {
                    tertiaryAxis = previous(axis);
                }
                Direction tertiaryFace = getFacing(tertiaryAxis, rotationCount >= 2);
                innerSides.add(tertiaryFace);

                offset = relative(Vec3.ZERO, tertiaryFace, getValue(radius, tertiaryAxis));
                center = center.add(offset);
                radius = replaceValue(radius, tertiaryAxis, getValue(radius, tertiaryAxis) * 2);
            }
        }

        double cx = center.x;
        double cy = center.y;
        double cz = center.z;

        double rx = radius.x;
        double ry = radius.y;
        double rz = radius.z;

        BitSet data = null;
        if (hollow != PatternParameterHollow.FILLED_INNER) {
            data = new BitSet(Template.getDataSize(filledTemplate.getSize()));
        }
        for (int x = 0; x <= filledTemplate.getMax().getX(); x++) {
            double dx = Math.abs(x - cx) / rx;
            double dxx = dx * dx;
            for (int y = 0; y <= filledTemplate.getMax().getY(); y++) {
                double dy = Math.abs(y - cy) / ry;
                double dyy = dy * dy;
                for (int z = 0; z <= filledTemplate.getMax().getZ(); z++) {
                    double dz = Math.abs(z - cz) / rz;
                    double dzz = dz * dz;
                    if (dxx + dyy + dzz < 1) {
                        if (hollow != PatternParameterHollow.FILLED_INNER) {
                            // noinspection ConstantConditions
                            data.set(Template.posToIndex(filledTemplate.getSize(), x, y, z), true);
                        } else {
                            filledTemplate.set(x, y, z, true);
                        }
                    }
                }
            }
        }

        boolean outerFilled = hollow.outerFilled;

        if (hollow != PatternParameterHollow.FILLED_INNER) {
            // Z iteration
            for (int x = 0; x <= filledTemplate.getMax().getX(); x++) {
                for (int y = 0; y <= filledTemplate.getMax().getY(); y++) {
                    if (!innerSides.contains(Direction.NORTH)) {
                        for (int z = 0; z <= filledTemplate.getMax().getZ(); z++) {
                            if (data.get(Template.posToIndex(filledTemplate.getSize(), x, y, z))) {
                                filledTemplate.set(x, y, z, true);
                                break;
                            }
                            if (outerFilled) {
                                filledTemplate.set(x, y, z, true);
                            }
                        }
                    }

                    if (!innerSides.contains(Direction.SOUTH)) {
                        for (int z = filledTemplate.getMax().getZ(); z >= 0; z--) {
                            if (data.get(Template.posToIndex(filledTemplate.getSize(), x, y, z))) {
                                filledTemplate.set(x, y, z, true);
                                break;
                            }
                            if (outerFilled) {
                                filledTemplate.set(x, y, z, true);
                            }
                        }
                    }
                }
            }

            // Y iteration
            for (int x = 0; x <= filledTemplate.getMax().getX(); x++) {
                for (int z = 0; z <= filledTemplate.getMax().getZ(); z++) {
                    if (!innerSides.contains(Direction.DOWN)) {
                        for (int y = 0; y <= filledTemplate.getMax().getY(); y++) {
                            if (data.get(Template.posToIndex(filledTemplate.getSize(), x, y, z))) {
                                filledTemplate.set(x, y, z, true);
                                break;
                            }
                            if (outerFilled) {
                                filledTemplate.set(x, y, z, true);
                            }
                        }
                    }

                    if (!innerSides.contains(Direction.UP)) {
                        for (int y = filledTemplate.getMax().getY(); y >= 0; y--) {
                            if (data.get(Template.posToIndex(filledTemplate.getSize(), x, y, z))) {
                                filledTemplate.set(x, y, z, true);
                                break;
                            }
                            if (outerFilled) {
                                filledTemplate.set(x, y, z, true);
                            }
                        }
                    }
                }
            }

            // X iteration
            for (int y = 0; y <= filledTemplate.getMax().getY(); y++) {
                for (int z = 0; z <= filledTemplate.getMax().getZ(); z++) {
                    if (!innerSides.contains(Direction.WEST)) {
                        for (int x = 0; x <= filledTemplate.getMax().getX(); x++) {
                            if (data.get(Template.posToIndex(filledTemplate.getSize(), x, y, z))) {
                                filledTemplate.set(x, y, z, true);
                                break;
                            }
                            if (outerFilled) {
                                filledTemplate.set(x, y, z, true);
                            }
                        }
                    }

                    if (!innerSides.contains(Direction.EAST)) {
                        for (int x = filledTemplate.getMax().getX(); x >= 0; x--) {
                            if (data.get(Template.posToIndex(filledTemplate.getSize(), x, y, z))) {
                                filledTemplate.set(x, y, z, true);
                                break;
                            }
                            if (outerFilled) {
                                filledTemplate.set(x, y, z, true);
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private static Vec3 scale(Vec3 vec, double scale) {
        return vec.scale(scale);
    }

    private static Vec3 relative(Vec3 vec, Direction face, double by) {
        return vec.add(face.getStepX() * by, face.getStepY() * by, face.getStepZ() * by);
    }

    private static double getValue(Vec3 vec, Direction.Axis axis) {
        return axis.choose(vec.x, vec.y, vec.z);
    }

    private static Vec3 replaceValue(Vec3 vec, Direction.Axis axis, double value) {
        return new Vec3(axis == Direction.Axis.X ? value : vec.x, axis == Direction.Axis.Y ? value : vec.y,
            axis == Direction.Axis.Z ? value : vec.z);
    }

    private static Direction getFacing(Direction.Axis axis, boolean positive) {
        return Direction.fromAxisAndDirection(axis,
            positive ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
    }

    /** @return X -> Y -> Z -> X */
    private static Direction.Axis next(Direction.Axis axis) {
        return switch (axis) {
            case X -> Direction.Axis.Y;
            case Y -> Direction.Axis.Z;
            case Z -> Direction.Axis.X;
        };
    }

    /** @return X -> Z -> Y -> X */
    private static Direction.Axis previous(Direction.Axis axis) {
        return switch (axis) {
            case X -> Direction.Axis.Z;
            case Y -> Direction.Axis.X;
            case Z -> Direction.Axis.Y;
        };
    }
}
