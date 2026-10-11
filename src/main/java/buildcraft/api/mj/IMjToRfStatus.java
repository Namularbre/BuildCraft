package buildcraft.api.mj;

import java.util.Objects;

public interface IMjToRfStatus {
    static IMjToRfStatus get() {
        return MjToRfStatusHolder.status;
    }

    /** Replaces the active conversion status. BuildCraft Lib calls this with a config-backed implementation. */
    static void set(IMjToRfStatus status) {
        MjToRfStatusHolder.status = Objects.requireNonNull(status, "status");
    }

    MjRfConversion getConversion();

    boolean isAutoconvertEnabled();
}

final class MjToRfStatusHolder implements IMjToRfStatus {
    static volatile IMjToRfStatus status = new MjToRfStatusHolder();

    private final MjRfConversion defaultConversion = MjRfConversion.createDefault();

    @Override
    public MjRfConversion getConversion() {
        return defaultConversion;
    }

    @Override
    public boolean isAutoconvertEnabled() {
        return false;
    }
}
