package com.alerts;

import com.data_management.DataStorage;
import com.data_management.Patient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AlertGeneratorTest {

    private DataStorage dataStorage;
    private AlertGenerator alertGenerator;
    private static final int PATIENT_ID = 1;

    @BeforeEach
    public void setUp() {
        dataStorage = new DataStorage();
        alertGenerator = new AlertGenerator(dataStorage);
    }

    //Blood pressure alerts
    @Test
    void testSystolicIncreasingTrendTriggersNoException() {
        Patient p = buildPatient();
        // Three readings each >10 mmHg higher than the last
        p.addRecord(100.0, "SystolicPressure", 1000L);
        p.addRecord(115.0, "SystolicPressure", 2000L);
        p.addRecord(130.0, "SystolicPressure", 3000L);

        assertDoesNotThrow(() -> alertGenerator.evaluateData(p));
    }

    @Test
    void testSystolicDecreasingTrendTriggersNoException() {
        Patient p = buildPatient();
        p.addRecord(160.0, "SystolicPressure", 1000L);
        p.addRecord(145.0, "SystolicPressure", 2000L);
        p.addRecord(130.0, "SystolicPressure", 3000L);

        assertDoesNotThrow(() -> alertGenerator.evaluateData(p));
    }

    @Test
    void testNoBPTrendWithSmallChanges() {
        Patient p = buildPatient();
        // Changes of only 5 mmHg — should NOT trigger a trend alert
        p.addRecord(120.0, "SystolicPressure", 1000L);
        p.addRecord(125.0, "SystolicPressure", 2000L);
        p.addRecord(130.0, "SystolicPressure", 3000L);

        assertDoesNotThrow(() -> alertGenerator.evaluateData(p));
    }

    @Test
    void testSystolicAbove180TriggersCriticalAlert() {
        Patient p = buildPatient();
        p.addRecord(185.0, "SystolicPressure", 1000L);

        assertDoesNotThrow(() -> alertGenerator.evaluateData(p));
    }

    @Test
    void testSystolicBelow90TriggersCriticalAlert() {
        Patient p = buildPatient();
        p.addRecord(85.0, "SystolicPressure", 1000L);

        assertDoesNotThrow(() -> alertGenerator.evaluateData(p));
    }

    @Test
    void testDiastolicAbove120TriggersCriticalAlert() {
        Patient p = buildPatient();
        p.addRecord(125.0, "DiastolicPressure", 1000L);

        assertDoesNotThrow(() -> alertGenerator.evaluateData(p));
    }

    @Test
    void testDiastolicBelow60TriggersCriticalAlert() {
        Patient p = buildPatient();
        p.addRecord(55.0, "DiastolicPressure", 1000L);

        assertDoesNotThrow(() -> alertGenerator.evaluateData(p));
    }

    //Saturation alerts
    @Test
    void testLowSaturationBelow92TriggersAlert() {
        Patient p = buildPatient();
        p.addRecord(90.0, "Saturation", 1000L);

        assertDoesNotThrow(() -> alertGenerator.evaluateData(p));
    }

    @Test
    void testSaturationAt92DoesNotTriggerLowAlert() {
        Patient p = buildPatient();
        p.addRecord(92.0, "Saturation", 1000L);

        assertDoesNotThrow(() -> alertGenerator.evaluateData(p));
    }

    @Test
    void testRapidSaturationDropTriggersAlert() {
        Patient p = buildPatient();
        long now = System.currentTimeMillis();
        p.addRecord(98.0, "Saturation", now - 5 * 60 * 1000L); // 5 min ago
        p.addRecord(92.0, "Saturation", now);                   // drop of 6%

        assertDoesNotThrow(() -> alertGenerator.evaluateData(p));
    }

    @Test
    void testSaturationDropOutside10MinWindowDoesNotTrigger() {
        Patient p = buildPatient();
        long now = System.currentTimeMillis();
        p.addRecord(98.0, "Saturation", now - 15 * 60 * 1000L); // 15 min ago
        p.addRecord(92.0, "Saturation", now);                    // outside window

        assertDoesNotThrow(() -> alertGenerator.evaluateData(p));
    }

    //Hypotensive Hypoxemia alerts
    @Test
    void testHypotensiveHypoxemiaBothConditionsMet() {
        Patient p = buildPatient();
        p.addRecord(85.0, "SystolicPressure", 1000L);  // below 90
        p.addRecord(90.0, "Saturation",       2000L);  // below 92

        assertDoesNotThrow(() -> alertGenerator.evaluateData(p));
    }

    @Test
    void testHypotensiveHypoxemiaOnlyLowBPNoAlert() {
        Patient p = buildPatient();
        p.addRecord(85.0, "SystolicPressure", 1000L);  // below 90
        p.addRecord(95.0, "Saturation",       2000L);  // fine

        assertDoesNotThrow(() -> alertGenerator.evaluateData(p));
    }

    @Test
    void testHypotensiveHypoxemiaOnlyLowSatNoAlert() {
        Patient p = buildPatient();
        p.addRecord(110.0, "SystolicPressure", 1000L); // fine
        p.addRecord(90.0,  "Saturation",       2000L); // below 92

        assertDoesNotThrow(() -> alertGenerator.evaluateData(p));
    }

    //ECG alerts
    @Test
    void testECGNormalReadingsNoAlert() {
        Patient p = buildPatient();
        // 25 stable readings — no peak
        for (int i = 0; i < 25; i++) {
            p.addRecord(1.0, "ECG", 1000L + i * 100L);
        }

        assertDoesNotThrow(() -> alertGenerator.evaluateData(p));
    }

    @Test
    void testECGAbnormalPeakTriggersAlert() {
        Patient p = buildPatient();
        // Fill the window with baseline readings
        for (int i = 0; i < 20; i++) {
            p.addRecord(1.0, "ECG", 1000L + i * 100L);
        }
        // One reading that is more than twice the average
        p.addRecord(10.0, "ECG", 1000L + 20 * 100L);

        assertDoesNotThrow(() -> alertGenerator.evaluateData(p));
    }

    @Test
    void testECGFewerThanWindowSizeRecordsSkipped() {
        Patient p = buildPatient();
        // Only 5 readings — not enough to fill the window
        for (int i = 0; i < 5; i++) {
            p.addRecord(1.0, "ECG", 1000L + i * 100L);
        }

        assertDoesNotThrow(() -> alertGenerator.evaluateData(p));
    }

    //Manual triggers
    @Test
    void testManualAlertTriggeredValue1() {
        Patient p = buildPatient();
        p.addRecord(1.0, "Alert", 1000L);

        assertDoesNotThrow(() -> alertGenerator.evaluateData(p));
    }

    @Test
    void testManualAlertUntriggeredValue0() {
        Patient p = buildPatient();
        p.addRecord(0.0, "Alert", 1000L);

        assertDoesNotThrow(() -> alertGenerator.evaluateData(p));
    }

    //Edge case
    @Test
    void testEmptyPatientRecordsNoException() {
        Patient p = buildPatient();
        assertDoesNotThrow(() -> alertGenerator.evaluateData(p));
    }

    //Helper

    private Patient buildPatient() {
        return new Patient(PATIENT_ID);
    }
}
