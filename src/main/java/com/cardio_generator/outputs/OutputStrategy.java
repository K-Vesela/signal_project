package com.cardio_generator.outputs;

/**
 * Interface to define output strategies in this health data simulation system.
 * Implementations determine how and where generated health data is delivered,
 * like to a console, WebSocket or TCP.
 */
public interface OutputStrategy {

    /**
     *
     * @param patientId unique identifier for the patient
     * @param timestamp the time in which the data was generated
     * @param label the type of health data being generated (for example, "Saturation" or "Alert"
     * @param data the data value to be the output, represented as a string
     */
    void output(int patientId, long timestamp, String label, String data);
}
