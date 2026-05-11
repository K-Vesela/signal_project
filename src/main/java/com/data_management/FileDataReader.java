package com.data_management;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Reads simulator-generated patient data files and stores
 * the parsed information inside DataStorage.
 */
public class FileDataReader implements DataReader {

    private String directoryPath;

    /**
     * Creates a reader for the specified directory.
     *
     * @param directoryPath path to simulator output directory
     */
    public FileDataReader(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    /**
     * Reads all files inside the directory and stores their
     * data inside the provided DataStorage object.
     * <p>
     * Expected line format:
     * <p>
     * patientId,measurementValue,recordType,timestamp
     * <p>
     * Example:
     * 1,98.6,Temperature,1700000000000
     *
     * @param dataStorage the storage object where parsed data is saved
     * @throws IOException if reading files fails
     */
    @Override
    public void readData(DataStorage dataStorage) throws IOException {

        File directory = new File(directoryPath);

        // Validate directory
        if (!directory.exists() || !directory.isDirectory()) {
            throw new IOException("Invalid directory: " + directoryPath);
        }

        File[] files = directory.listFiles();

        if (files == null) {
            return;
        }

        // Read every file in the directory
        for (File file : files) {

            // Skip subdirectories
            if (!file.isFile()) {
                continue;
            }

            readFile(file, dataStorage);
        }
    }

    /**
     * Reads and parses one file.
     *
     * @param file the file to read
     * @param dataStorage destination storage
     * @throws IOException if file reading fails
     */
    private void readFile(File file, DataStorage dataStorage)
            throws IOException {

        try (BufferedReader reader =
                     new BufferedReader(new FileReader(file))) {

            String line;

            while ((line = reader.readLine()) != null) {

                // Ignore blank lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                parseAndStore(line, dataStorage);
            }
        }
    }

    /**
     * Parses one line of simulator output and stores it.
     * <p>
     * Expected CSV format:
     * patientId,measurementValue,recordType,timestamp
     *
     * @param line raw input line
     * @param dataStorage storage destination
     */
    private void parseAndStore(String line, DataStorage dataStorage) {

        String[] parts = line.split(",");

        // Validate format
        if (parts.length != 4) {
            System.err.println("Invalid record format: " + line);
            return;
        }

        try {

            int patientId =
                    Integer.parseInt(parts[0].trim());

            double measurementValue =
                    Double.parseDouble(parts[1].trim());

            String recordType =
                    parts[2].trim();

            long timestamp =
                    Long.parseLong(parts[3].trim());

            dataStorage.addPatientData(
                    patientId,
                    measurementValue,
                    recordType,
                    timestamp
            );

        } catch (NumberFormatException e) {

            System.err.println(
                    "Invalid numeric value in line: " + line
            );
        }
    }
}