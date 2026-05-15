package com.data_management;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

class DataStorageTest {

    private DataStorage dataStorage;

    @BeforeEach
    public void setUp() {
        dataStorage = new DataStorage();
    }

    @Test
    void testAddAndGetRecords() {
        // TODO Perhaps you can implement a mock data reader to mock the test data?
        DataReader reader = new DataReader() {
            @Override
            public void readData(DataStorage storage) throws IOException {

            }
        };

        dataStorage.addPatientData(1, 100.0, "WhiteBloodCells", 1714376789050L);
        dataStorage.addPatientData(1, 200.0, "WhiteBloodCells", 1714376789051L);

        List<PatientRecord> records = dataStorage.getRecords(1, 1714376789050L, 1714376789051L);
        assertEquals(2, records.size()); // Check if two records are retrieved
        assertEquals(100.0, records.get(0).getMeasurementValue()); // Validate first record
    }

    @Test
    void testSingleRecordStoredAndRetrieved() {
        dataStorage.addPatientData(1, 120.0, "SystolicPressure", 1000L);

        List<PatientRecord> records = dataStorage.getRecords(1,0L,Long.MAX_VALUE);
        assertEquals(1, records.size());
        assertEquals(120.0, records.get(0).getMeasurementValue());
        assertEquals("SystolicPressure", records.get(0).getRecordType());
        assertEquals(1000L, records.get(0).getTimestamp());
    }

    @Test
    void testMultipleRecordsSamePatient() {
        dataStorage.addPatientData(2, 98.0, "Saturation", 1000L);
        dataStorage.addPatientData(2, 97.0, "Saturation", 2000L);
        dataStorage.addPatientData(2, 120.0, "SystolicPressure", 3000L);
        List<PatientRecord> records = dataStorage.getRecords(2, 0L, Long.MAX_VALUE);
        assertEquals(3, records.size());
    }

    @Test
    void testRecordsFromDifferentPatientAreIsolated() {
        dataStorage.addPatientData(3, 1.0, "ECG", 1000L);
        dataStorage.addPatientData(4, 2.0, "ECG", 1000L);

        assertEquals(1, dataStorage.getRecords(3, 0L, Long.MAX_VALUE).size());
        assertEquals(1, dataStorage.getRecords(4, 0L, Long.MAX_VALUE).size());
    }

    @Test
    void testGetRecordsUnknownPatientReturnsEmpty() {
        List<PatientRecord> records = dataStorage.getRecords(9999, 0L, Long.MAX_VALUE);
        assertNotNull(records);
        assertTrue(records.isEmpty());
    }

    @Test
    void testGetRecordsFilteringBeforeRange() {
        dataStorage.addPatientData(5, 1.0, "ECG", 500L);
        dataStorage.addPatientData(5, 2.0, "ECG", 1500L);

        List<PatientRecord> records = dataStorage.getRecords(5, 1000L, 3000L);
        assertEquals(1, records.size());
        assertEquals(2.0, records.get(0).getMeasurementValue());
    }

    @Test
    void testGetRecordsFilteringAfterRange() {
        dataStorage.addPatientData(6, 1.0, "ECG", 1500L);
        dataStorage.addPatientData(6, 2.0, "ECG", 4000L);

        List<PatientRecord> records = dataStorage.getRecords(6, 1000L, 3000L);
        assertEquals(1, records.size());
        assertEquals(1.0, records.get(0).getMeasurementValue());
    }

    @Test
    void testGetRecordsInclusiveBoundaries() {
        dataStorage.addPatientData(7, 1.0, "ECG", 500L);
        dataStorage.addPatientData(7, 2.0, "ECG", 1500L);

        List<PatientRecord> records = dataStorage.getRecords(7, 500L, 1500L);
        assertEquals(2, records.size());
    }

    @Test
    void testGetRecordsEmptyWhenNoneInRange() {
        dataStorage.addPatientData(8, 1.0, "ECG", 500L);

        List<PatientRecord> records = dataStorage.getRecords(8, 1000L, 3000L);
        assertTrue(records.isEmpty());
    }

    @Test
    void testGetAllPatientsReturnsAllAdded() {
        dataStorage.addPatientData(20, 1.0, "ECG", 1000L);
        dataStorage.addPatientData(21, 1.0, "ECG", 1000L);

        List<Patient> all = dataStorage.getAllPatients();
        assertEquals(2, all.size());
    }

    @Test
    void testGetAllPatientsEmptyOnNewStorage() {
        List<Patient> all = dataStorage.getAllPatients();
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }
}
