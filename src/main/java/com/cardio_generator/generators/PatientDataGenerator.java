package com.cardio_generator.generators;

import com.cardio_generator.outputs.OutputStrategy;

/**
 * Interface for generating simulated patient data in a health monitoring system.
 * Implementations are responsible for producing specific types of health data
 * (for example, blood pressure) and forwarding it to the desired output strategy.
 */
public interface PatientDataGenerator {

    /**
     *
     * @param patientId unique identifier for the patient whose data is being generated
     * @param outputStrategy strategy used to output the generated data
     */
    void generate(int patientId, OutputStrategy outputStrategy);
}
