package Windows;

import Data.ConfigData;
import Data.Models.OperationEnum;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the ordering controls for an operation and synchronizes the selection
 * with a ConfigData instance. Each operation can have a different set of
 * order-values; the manager remembers the last chosen order per operation.
 */
public class OperationOrderManager {
    private final ConfigData configData;

    private final JComboBox<OperationEnum> operationCombo;
    private final JComboBox<Enum<?>>[] orderCombos; // three child combos in fixed positions

    private final Map<OperationEnum, Enum<?>[]> lastOrders = new HashMap<>();
    private final Map<OperationEnum, Boolean> firstTime = new HashMap<>();
    private final Enum<?>[] previousValues;

    /**
     * Flag used to ignore events triggered programmatically.
     */
    private boolean internalChange = false;

    /**
     * Construct the manager and wire listeners.
     *
     * @param operationCombo top-level operation selector
     * @param firstCombo first order combo (position 0)
     * @param secondCombo second order combo (position 1)
     * @param thirdCombo third order combo (position 2)
     * @param configData configuration object to read/store current selection
     */
    public OperationOrderManager(JComboBox<OperationEnum> operationCombo,
                                 JComboBox<Enum<?>> firstCombo,
                                 JComboBox<Enum<?>> secondCombo,
                                 JComboBox<Enum<?>> thirdCombo,
                                 ConfigData configData) {
        this.operationCombo = operationCombo;
        this.orderCombos = new JComboBox[]{firstCombo, secondCombo, thirdCombo};
        this.configData = configData;
        this.previousValues = new Enum[this.orderCombos.length];

        initializeFirstTimeMap();
        seedLastOrdersFromConfig();
        initializeChildCombos();
        setupOperationComboListener();
        setupOrderComboListeners();
        loadInitialConfig();
    }

    private void initializeFirstTimeMap() {
        for (OperationEnum op : OperationEnum.values()) {
            firstTime.put(op, true);
        }
    }

    private void seedLastOrdersFromConfig() {
        // Seed lastOrders using configData; let JVM surface errors if config is malformed.
        lastOrders.put(this.configData.operation, this.configData.order);
        // Mark the config operation as already loaded so defaults are not applied.
        firstTime.put(this.configData.operation, false);
    }

    /**
     * Initialize each child combo with a default "none" value if available.
     * The default is assumed to be the first value returned by any operation's getOrderValues().
     */
    private void initializeChildCombos() {
        Enum<?> noneValue = findFirstNoneValue();

        for (JComboBox<Enum<?>> combo : orderCombos) {
            combo.removeAllItems();
            combo.addItem(noneValue);
            combo.setSelectedItem(noneValue);
        }
    }

    private Enum<?> findFirstNoneValue() {
        for (OperationEnum op : OperationEnum.values()) {
            Enum<?>[] values = op.getOrderValues();
            
            if (values != null && values.length > 0) {
                return values[0];
            }
        }
        
        return null;
    }

    private void setupOperationComboListener() {
        operationCombo.addActionListener(e -> {
            if (internalChange) return;

            internalChange = true;
            OperationEnum selectedOp = (OperationEnum) operationCombo.getSelectedItem();
            updateOrderCombos(selectedOp);
            internalChange = false;
        });
    }

    private void setupOrderComboListeners() {
        ActionListener orderListener = e -> {
            if (internalChange) return;

            internalChange = true;
            
            JComboBox<Enum<?>> changedCombo = (JComboBox<Enum<?>>) e.getSource();
            int changedIndex = indexOfCombo(changedCombo);

            Enum<?> newValue = (Enum<?>) changedCombo.getSelectedItem();
            
            if (changedIndex >= 0) {
                previousValues[changedIndex] = newValue;
            }

            saveCurrentOrder();
            internalChange = false;
        };

        for (JComboBox<Enum<?>> combo : orderCombos) {
            combo.addActionListener(orderListener);
        }
    }

    private int indexOfCombo(JComboBox<Enum<?>> combo) {
        for (int i = 0; i < orderCombos.length; i++) {
            if (orderCombos[i] == combo) return i;
        }
        
        return -1;
    }

    /**
     * Populate and enable/disable the child combos according to the selected operation.
     * If this is the first time the operation is selected, start with the operation's
     * default values; otherwise restore the last remembered order for that operation.
     *
     * @param operation the operation whose order-values should be loaded
     */
    private void updateOrderCombos(OperationEnum operation) {
        if (operation == null) return;

        Enum<?>[] values = operation.getOrderValues();
        boolean isFirstTime = firstTime.getOrDefault(operation, true);

        for (int i = 0; i < orderCombos.length; i++) {
            JComboBox<Enum<?>> combo = orderCombos[i];
            
            if (i < values.length) {
                combo.setEnabled(true);

                Enum<?> toSelect;
                
                if (isFirstTime) {
                    toSelect = values[0]; // default (NONE) for first time
                } else {
                    Enum<?>[] savedOrder = lastOrders.getOrDefault(operation, values.clone());
                    toSelect = (i < savedOrder.length && savedOrder[i] != null) ? savedOrder[i] : values[0];
                }

                internalChange = true;
                setComboValues(combo, values, toSelect);
                previousValues[i] = toSelect;
                internalChange = false;
            } else {
                internalChange = true;
                combo.removeAllItems();
                combo.setEnabled(false);
                previousValues[i] = null;
                internalChange = false;
            }
        }

        if (isFirstTime) {
            firstTime.put(operation, false);
        }

        saveCurrentOrder();
    }

    private void setComboValues(JComboBox<Enum<?>> combo, Enum<?>[] values, Enum<?> selected) {
        combo.removeAllItems();
        
        for (Enum<?> v : values) {
            combo.addItem(v);
        }
        
        combo.setSelectedItem(selected);
    }

    /**
     * Read the selected values from the child combos, store them in memory, and persist to configData.
     */
    private void saveCurrentOrder() {
        OperationEnum operation = (OperationEnum) operationCombo.getSelectedItem();
        
        if (operation == null) return;

        Enum<?>[] order = new Enum[orderCombos.length];
        
        for (int i = 0; i < orderCombos.length; i++) {
            order[i] = (Enum<?>) orderCombos[i].getSelectedItem();
        }

        lastOrders.put(operation, Arrays.copyOf(order, order.length));
        configData.operation = operation;
        configData.order = Arrays.copyOf(order, order.length);
    }

    /**
     * Set initial UI state from config data and ensure the order combos reflect that state.
     */
    private void loadInitialConfig() {
        internalChange = true;
        operationCombo.setSelectedItem(configData.operation);
        internalChange = false;

        lastOrders.put(configData.operation, configData.order);
        updateOrderCombos((OperationEnum) operationCombo.getSelectedItem());
    }
}