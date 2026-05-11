package com.cardio_generator.outputs;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link OutputStrategy} that writes patient health data into a text file.
 * Each data label is written into its own file within the base directory.
 * Files are created if they don't exist.
 */
public class FileOutputStrategy implements OutputStrategy {
    //variable name changed to camelCase and updated all future usages
    private String baseDirectory;

    /** Maps each data label to its corresponding output file path. */
    //changed variable name to camelCase instead of using underscore and updated all future usages
    public final ConcurrentHashMap<String, String> fileMap = new ConcurrentHashMap<>();

    /** Constructs a FileInputStrategy that writes output files to the specified directory.
     *
     * @param baseDirectory the path to the directory where the output files will be stored;
     *                      The directory is created if it doesn't already exist.
     */
    public FileOutputStrategy(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    /**
     * Writes the patient health data ti a label specific file within the base directory.
     * If the file for a given label doesn't exist, it will be created. Each entry is recorded
     * on a new line in the format: {@code patientId, timestamp, label, data}
     *
     * @param patientId unique identifier for the patient
     * @param timestamp the time in which the data was generated
     * @param label the type of health data being generated (for example, "Saturation" or "Alert"
     * @param data the data value to be the output, represented as a string
     */
    @Override
    public void output(int patientId, long timestamp, String label, String data) {
        try {
            // Create the directory
            Files.createDirectories(Paths.get(baseDirectory));
        } catch (IOException e) {
            System.err.println("Error creating base directory: " + e.getMessage());
            return;
        }
        //changed variable from FilePath to filePath (camelCase)
        // Set the filePath variable
        String filePath = fileMap.computeIfAbsent(label, k -> Paths.get(baseDirectory, label + ".txt").toString());

        // Write the data to the file
        try (PrintWriter out = new PrintWriter(
                Files.newBufferedWriter(Paths.get(filePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
            out.printf("Patient ID: %d, Timestamp: %d, Label: %s, Data: %s%n", patientId, timestamp, label, data);
        } catch (Exception e) {
            System.err.println("Error writing to file " + filePath + ": " + e.getMessage());
        }
    }
}