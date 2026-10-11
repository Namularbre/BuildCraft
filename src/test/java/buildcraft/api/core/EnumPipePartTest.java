package buildcraft.api.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.minecraft.core.Direction;
import net.minecraft.nbt.ByteTag;

class EnumPipePartTest {
    @Test
    void facingRoundTrip() {
        for (Direction dir : Direction.values()) {
            EnumPipePart part = EnumPipePart.fromFacing(dir);
            assertEquals(dir, part.face);
            assertEquals(dir.get3DDataValue(), part.getIndex());
        }
        assertEquals(EnumPipePart.CENTER, EnumPipePart.fromFacing(null));
    }

    @Test
    void opposite() {
        assertEquals(EnumPipePart.UP, EnumPipePart.DOWN.opposite());
        assertEquals(EnumPipePart.EAST, EnumPipePart.WEST.opposite());
        assertEquals(EnumPipePart.CENTER, EnumPipePart.CENTER.opposite());
    }

    @Test
    void nbtRoundTrip() {
        for (EnumPipePart part : EnumPipePart.VALUES) {
            assertEquals(part, EnumPipePart.readFromNBT(part.writeToNBT()));
        }
        // 1.12 also saved parts as their ordinal
        assertEquals(EnumPipePart.NORTH, EnumPipePart.readFromNBT(ByteTag.valueOf((byte) 2)));
        assertEquals(EnumPipePart.CENTER, EnumPipePart.readFromNBT(null));
    }
}
