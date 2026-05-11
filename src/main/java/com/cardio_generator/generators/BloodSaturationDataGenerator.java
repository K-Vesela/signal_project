package com.cardio_generator.generators;

import java.util.Random;

import com.cardio_generator.outputs.OutputStrategy;

/**
 * A {@link PatientDataGenerator} that simulates blood oxygen saturation readings.
 * Each patient's saturation level fluctuates slightly over time around an initial baseline,
 * and is limited to the realistic range of 90%–100%.
 */
public class BloodSaturationDataGenerator implements PatientDataGenerator {
    private static final Random random = new Random();
    private int[] lastSaturationValues;


    /**
     * Constructs a {@code BloodSaturationDataGenerator} for the given number of patients.
     * Each patient is initialized with a baseline saturation value between 95% and 100%.
     *
     * @param patientCount the number of patients to simulate; patient IDs are expected
     *                     to be in the range 1 to {@code patientCount} inclusive
     */
    public BloodSaturationDataGenerator(int patientCount) {
        lastSaturationValues = new int[patientCount + 1];

        // Initialize with baseline saturation values for each patient
        for (int i = 1; i <= patientCount; i++) {
            lastSaturationValues[i] = 95 + random.nextInt(6); // Initializes with a value between 95 and 100
        }
    }

    /**
     * Generates a simulated blood saturation reading for the specified patient and outputs it.
     * The new value is computed by applying a small random fluctuation (−1, 0, or +1) to the
     * patient's previous value, then limiting the result to the range [90, 100].
     * The output label is {@code "Saturation"} and the value is formatted as a percentage string.
     *
     * @param patientId      the unique identifier of the patient
     * @param outputStrategy the strategy used to transmit the generated saturation reading
     */
    @Override
    public void generate(int patientId, OutputStrategy outputStrategy) {
        try {
            // Simulate blood saturation values
            int variation = random.nextInt(3) - 1; // -1, 0, or 1 to simulate small fluctuations
            int newSaturationValue = lastSaturationValues[patientId] + variation;

            // Ensure the saturation stays within a realistic and healthy range
            newSaturationValue = Math.min(Math.max(newSaturationValue, 90), 100);
            lastSaturationValues[patientId] = newSaturationValue;
            outputStrategy.output(patientId, System.currentTimeMillis(), "Saturation",
                    Double.toString(newSaturationValue) + "%");
        } catch (Exception e) {
            System.err.println("An error occurred while generating blood saturation data for patient " + patientId);
            e.printStackTrace(); // This will print the stack trace to help identify where the error occurred.
        }
    }
}
