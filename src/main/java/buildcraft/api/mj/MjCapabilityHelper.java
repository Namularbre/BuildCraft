package buildcraft.api.mj;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.Direction;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

/** Provides a quick way to return all types of a single {@link IMjConnector} for all the different capabilities.
 * <p>
 * Owners must call {@link #invalidate()} when the object exposing these capabilities is removed. */
public class MjCapabilityHelper implements ICapabilityProvider {
    private final LazyOptional<IMjConnector> connector;
    private final LazyOptional<IMjReceiver> receiver;
    private final LazyOptional<IMjRedstoneReceiver> rsReceiver;
    private final LazyOptional<IMjReadable> readable;
    private final LazyOptional<IMjPassiveProvider> provider;
    private final LazyOptional<IEnergyStorage> rfAutoConvert;

    public MjCapabilityHelper(@Nonnull IMjConnector mj) {
        this.connector = LazyOptional.of(() -> mj);
        this.receiver = optionalOf(mj instanceof IMjReceiver ? (IMjReceiver) mj : null);
        this.rsReceiver = optionalOf(mj instanceof IMjRedstoneReceiver ? (IMjRedstoneReceiver) mj : null);
        this.readable = optionalOf(mj instanceof IMjReadable ? (IMjReadable) mj : null);
        this.provider = optionalOf(mj instanceof IMjPassiveProvider ? (IMjPassiveProvider) mj : null);
        this.rfAutoConvert = LazyOptional.of(() -> new RfAdapter(mj));
    }

    private static <T> LazyOptional<T> optionalOf(@Nullable T value) {
        return value == null ? LazyOptional.empty() : LazyOptional.of(() -> value);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        if (capability == MjAPI.CAP_CONNECTOR) {
            return connector.cast();
        }
        if (capability == MjAPI.CAP_RECEIVER) {
            return receiver.cast();
        }
        if (capability == MjAPI.CAP_REDSTONE_RECEIVER) {
            return rsReceiver.cast();
        }
        if (capability == MjAPI.CAP_READABLE) {
            return readable.cast();
        }
        if (capability == MjAPI.CAP_PASSIVE_PROVIDER) {
            return provider.cast();
        }
        if (capability == ForgeCapabilities.ENERGY && MjAPI.isRfAutoConversionEnabled()) {
            return rfAutoConvert.cast();
        }
        return LazyOptional.empty();
    }

    public void invalidate() {
        connector.invalidate();
        receiver.invalidate();
        rsReceiver.invalidate();
        readable.invalidate();
        provider.invalidate();
        rfAutoConvert.invalidate();
    }

    /** Exposes an MJ connector as a Forge Energy storage. */
    private static final class RfAdapter implements IEnergyStorage {
        @Nullable
        private final IMjReceiver receiver;
        @Nullable
        private final IMjReadable readable;
        @Nullable
        private final IMjPassiveProvider provider;

        RfAdapter(IMjConnector mj) {
            this.receiver = mj instanceof IMjReceiver ? (IMjReceiver) mj : null;
            this.readable = mj instanceof IMjReadable ? (IMjReadable) mj : null;
            this.provider = mj instanceof IMjPassiveProvider ? (IMjPassiveProvider) mj : null;
        }

        @Override
        public int getEnergyStored() {
            IMjReadable read = readable;
            if (read != null) {
                long mjPerRf = MjAPI.getRfConversion().mjPerRf;
                return (int) Math.min(Integer.MAX_VALUE, read.getStored() / mjPerRf);
            } else {
                return 0;
            }
        }

        @Override
        public int getMaxEnergyStored() {
            IMjReadable read = readable;
            if (read != null) {
                long mjPerRf = MjAPI.getRfConversion().mjPerRf;
                return (int) Math.min(Integer.MAX_VALUE, read.getCapacity() / mjPerRf);
            } else {
                return 0;
            }
        }

        @Override
        public boolean canReceive() {
            return receiver != null && receiver.canReceive();
        }

        /** @return Amount of energy that was (or would have been, if simulated) accepted by the storage. */
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (maxReceive <= 0) {
                return 0;
            }
            IMjReceiver recv = receiver;
            if (recv == null || !recv.canReceive()) {
                return 0;
            }
            long mjPerRf = MjAPI.getRfConversion().mjPerRf;
            long maxReceiveMj = maxReceive * mjPerRf;
            long excess = recv.receivePower(maxReceiveMj, true);
            // Actual MJ that was accepted
            long acceptedMj = maxReceiveMj - excess;
            if (acceptedMj < mjPerRf) {
                return 0;
            }
            // Only accept an integer multiple of mjPerRf, so that the amount maps exactly back to RF
            long exactAcceptableMj = acceptedMj - acceptedMj % mjPerRf;
            int rf = (int) (exactAcceptableMj / mjPerRf);

            long excess2 = recv.receivePower(exactAcceptableMj, true);
            if (excess2 != 0) {
                // Odd. This means we can't actually accept the exact amount
                return 0;
            }
            if (!simulate) {
                long excess3 = recv.receivePower(exactAcceptableMj, false);
                if (excess3 != excess2) {
                    throw new IllegalStateException("Bad impl: " + recv.getClass() + " of receivePower");
                }
            }
            return rf;
        }

        @Override
        public boolean canExtract() {
            return provider != null;
        }

        /** @return Amount of energy that was (or would have been, if simulated) extracted from the storage. */
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            IMjPassiveProvider prov = provider;
            if (prov == null || maxExtract <= 0) {
                return 0;
            }
            long mjPerRf = MjAPI.getRfConversion().mjPerRf;
            long extracted = prov.extractPower(0, maxExtract * mjPerRf, true);
            // Only extract an integer multiple of mjPerRf
            long exactMj = extracted - extracted % mjPerRf;
            if (exactMj <= 0) {
                return 0;
            }
            if (!simulate) {
                exactMj = prov.extractPower(exactMj, exactMj, false);
            }
            return (int) (exactMj / mjPerRf);
        }
    }
}
