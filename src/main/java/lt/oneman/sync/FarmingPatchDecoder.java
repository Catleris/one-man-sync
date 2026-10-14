package lt.oneman.sync;

import net.runelite.client.plugins.timetracking.farming.CropState;
import net.runelite.client.plugins.timetracking.farming.Produce;

public class FarmingPatchDecoder {
    public static DecodedPatch decode(FarmingPatchType implementation, String storedValue) {
        if (implementation == null || storedValue == null) return null;
        String[] parts = storedValue.trim().split(":");
        if (parts.length != 2) return null;
        try {
            int value = Integer.parseInt(parts[0]);
            long seen = Long.parseLong(parts[1]);
            if (value < 0 || value > 255 || seen <= 0) return null;
            return implementation.decode(value, seen);
        } catch (NumberFormatException error) { return null; }
    }

    public static class DecodedPatch {
        private final Produce produce;
        private final CropState cropState;
        private final int stage;
        private final int stages;
        private final int tickRate;
        private final long unixTime;

        public DecodedPatch(Produce produce, CropState cropState, int stage, long seen) {
            this(produce, cropState, stage,
                cropState == CropState.HARVESTABLE || cropState == CropState.FILLING ? produce.getHarvestStages() : produce.getStages(),
                cropState == CropState.GROWING ? produce.getTickrate() : cropState == CropState.HARVESTABLE ? produce.getRegrowTickrate() : 0,
                seen);
        }

        public DecodedPatch(Produce produce, CropState cropState, int stage, int stages, int tickRate, long unixTime) {
            this.produce = produce;
            this.cropState = cropState;
            this.stage = stage;
            this.stages = stages;
            this.tickRate = tickRate;
            this.unixTime = unixTime;
        }

        public Produce getProduce() {
            return this.produce;
        }

        public CropState getCropState() {
            return this.cropState;
        }

        public int getStage() {
            return this.stage;
        }

        public int getStages() {
            return this.stages;
        }

        public int getTickRate() {
            return this.tickRate;
        }

        public long getUnixTime() {
            return this.unixTime;
        }

        public boolean isReady(long nowSec) {
            if (this.produce == Produce.WEEDS) return false;
            if (this.cropState == CropState.HARVESTABLE) {
                return true;
            }
            if (this.cropState == CropState.GROWING && this.tickRate > 0 && this.stages > 0) {
                long readyAt = this.estimateReadyEpochSec();
                return readyAt > 0L && nowSec >= readyAt;
            }
            return false;
        }

        public long estimateReadyEpochSec() {
            if (this.produce == Produce.WEEDS) return 0;
            if (this.cropState == CropState.HARVESTABLE) {
                return this.unixTime;
            }
            if (this.cropState == CropState.GROWING && this.tickRate > 0 && this.stages > 0 && this.unixTime > 0L) {
                int stagesRemaining = Math.max(0, this.stages - 1 - this.stage);
                return this.unixTime + (long)stagesRemaining * (long)this.tickRate * 60L;
            }
            return 0L;
        }

        public String getDisplayStatus(long nowSec) {
            if (this.cropState == null) {
                return "Unknown";
            }
            switch (this.cropState) {
                case HARVESTABLE: {
                    return this.produce != null ? this.produce.getName() + " \u2014 Ready to harvest / check health" : "Ready to harvest / check health";
                }
                case GROWING: {
                    String cropName;
                    if (this.produce == Produce.WEEDS) {
                        return "Empty / Weeds (stage " + this.stage + "/" + this.stages + ")";
                    }
                    long readySec = this.estimateReadyEpochSec();
                    cropName = this.produce != null ? this.produce.getName() : "Growing";
                    if (readySec > 0L) {
                        if (nowSec >= readySec) {
                            return cropName + " \u2014 Estimated ready (visit to confirm)";
                        }
                        long left = readySec - nowSec;
                        long hours = left / 3600L;
                        long mins = left % 3600L / 60L;
                        if (hours > 0L) {
                            return cropName + " \u2014 Growing (" + hours + "h " + mins + "m left)";
                        }
                        return cropName + " \u2014 Growing (" + mins + "m left)";
                    }
                    return cropName + " \u2014 Growing (stage " + this.stage + "/" + this.stages + ")";
                }
                case DISEASED: {
                    return (this.produce != null ? this.produce.getName() : "Crop") + " \u2014 Diseased!";
                }
                case DEAD: {
                    return (this.produce != null ? this.produce.getName() : "Crop") + " \u2014 Dead";
                }
                case EMPTY: {
                    return "Empty";
                }
                case FILLING: {
                    return (this.produce != null ? this.produce.getName() : "Bin") + " \u2014 Filling";
                }
            }
            return this.cropState.name();
        }
    }
}

