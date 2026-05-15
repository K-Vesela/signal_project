package com.cardio_generator;

import com.data_management.DataStorage;

import java.io.IOException;

/**
 * Entry point that routes to either {@link HealthDataSimulator} or
 * {@link DataStorage} depending on the specified first command-line argument.
 * <p>
 * Usage:
 * java -jar 64380911_cardio_simulator.jar DataStorage
 * java -jar 64380911_cardio_simulator.jar             (defaults to HealthDataSimulator)
 */
public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length > 0 && args[0].equals("DataStorage")) {
            DataStorage.main(new String[]{});
        } else {
            HealthDataSimulator.main(args);
        }
    }
}

