package com.data_management;

import com.data_management.Patient;
import com.data_management.PatientRecord;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class PatientTest {
    @Test
    void testGetRecordsReturnsAllWithinRange() {
        Patient p = new Patient(1);
        p.addRecord(1.0, "ECG", 1000L);
        p.addRecord(2.0, "ECG", 2000L);
        p.addRecord(3.0, "ECG", 3000L);

        List<PatientRecord> result = p.getRecords(1000L, 3000L);
        assertEquals(3, result.size());
    }

    @Test
    void testGetRecordsFiltersOutsideRange() {
        Patient p = new Patient(1);
        p.addRecord(1.0, "ECG", 500L);   // before range
        p.addRecord(2.0, "ECG", 1500L);  // inside range
        p.addRecord(3.0, "ECG", 3500L);  // after range

        List<PatientRecord> result = p.getRecords(1000L, 3000L);
        assertEquals(1, result.size());
        assertEquals(2.0, result.get(0).getMeasurementValue());
    }

    @Test
    void testGetRecordsEmptyWhenNoMatch() {
        Patient p = new Patient(1);
        p.addRecord(1.0, "ECG", 500L);

        List<PatientRecord> result = p.getRecords(1000L, 3000L);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetRecordsOnEmptyPatient() {
        Patient p = new Patient(1);
        List<PatientRecord> result = p.getRecords(0L, Long.MAX_VALUE);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetPatientIdReturnsCorrectValue() {
        Patient p = new Patient(42);
        assertEquals(42, p.getPatientId());
    }
}
