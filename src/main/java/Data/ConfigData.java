package Data;

import Data.Models.OperationEnum;

/**
 * Holds configuration data for the current image processing operation,
 * including the selected operation type, channel order, and number of buckets.
 */
public class ConfigData {
    public OperationEnum operation;
    public Enum<?>[] order;
    public int buckets;

    /**
     * Constructs a configuration object with operation, channel order, and bucket count.
     *
     * @param operation the selected operation
     * @param order the array representing the order of channels or stages
     * @param buckets the number of buckets to use
     */
    public ConfigData(OperationEnum operation, Enum<?>[] order, int buckets) {
        this.operation = operation;
        this.order = order;
        this.buckets = buckets;
    }
}