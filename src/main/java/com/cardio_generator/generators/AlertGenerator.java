package com.cardio_generator.generators;

import java.util.Random;

import com.cardio_generator.outputs.OutputStrategy;

/**
 * A {@link PatientDataGenerator} that simulates alert events for patients.
 * Each patient can be in one of two states: active alert or no alert. On each call
 * to {@link #generate}, an active alert has a 90% chance of resolving, while an
 * inactive patient has a probability of triggering a new alert with rate {@code lambda = 0.1}.
 */
public class AlertGenerator implements PatientDataGenerator {

    public static final Random randomGenerator = new Random();
    //changed AlertStates to alertStates (camelCase) and all future uses
    private boolean[] alertStates; // false = resolved, true = pressed

    /**
     * Constructs an {@code AlertGenerator} for the given number of patients.
     * All patients are initialized with no active alert.
     *
     * @param patientCount the number of patients to simulate
     */
    public AlertGenerator(int patientCount) {
        alertStates = new boolean[patientCount + 1];
    }

    /**
     * Evaluates and updates the alert state for the specified patient, then outputs the result.
     *
     * @param patientId      the unique identifier of the patient
     * @param outputStrategy the strategy used to transmit the alert event;
     *                       the output label is {@code "Alert"}
     */
    @Override
    public void generate(int patientId, OutputStrategy outputStrategy) {
        try {
            if (alertStates[patientId]) {
                if (randomGenerator.nextDouble() < 0.9) { // 90% chance to resolve
                    alertStates[patientId] = false;
                    // Output the alert
                    outputStrategy.output(patientId, System.currentTimeMillis(), "Alert", "resolved");
                }
            } else {
                //changed Lambda to lamba as it is a local variable (camelCase)
                double lambda = 0.1; // Average rate (alerts per period), adjust based on desired frequency
                double p = -Math.expm1(-lambda); // Probability of at least one alert in the period
                boolean alertTriggered = randomGenerator.nextDouble() < p;

                if (alertTriggered) {
                    alertStates[patientId] = true;
                    // Output the alert
                    outputStrategy.output(patientId, System.currentTimeMillis(), "Alert", "triggered");
                }
            }
        } catch (Exception e) {
            System.err.println("An error occurred while generating alert data for patient " + patientId);
            e.printStackTrace();
        }
    }
}
