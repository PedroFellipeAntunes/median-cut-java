package Data.Models;

/**
 * Defines the supported color space operations for image processing.
 * Each operation type is associated with its corresponding order enum class.
 */
public enum OperationEnum {
    GRAYSCALE(GrayscaleOrderEnum.class),
    RGB(RgbOrderEnum.class),
    HSL(HslOrderEnum.class),
    HSB(HsbOrderEnum.class),
    OKLAB(OkLabOrderEnum.class);

    private final Class<? extends Enum<?>> orderEnumClass;

    /**
     * Associates each operation type with its channel order enumeration class.
     *
     * @param orderEnumClass class representing the order enum for the operation
     */
    OperationEnum(Class<? extends Enum<?>> orderEnumClass) {
        this.orderEnumClass = orderEnumClass;
    }

    /**
     * Returns the class of the enum that defines the channel order for this operation.
     *
     * @return enum class representing the order definition
     */
    public Class<? extends Enum<?>> getOrderEnumClass() {
        return orderEnumClass;
    }

    /**
     * Returns the possible order values for this operation.
     *
     * @return array of enum constants representing order values
     */
    public Enum<?>[] getOrderValues() {
        return orderEnumClass.getEnumConstants();
    }
}