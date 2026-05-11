package com.alerts;

import com.data_management.DataStorage;
import com.data_management.Patient;
import com.data_management.PatientRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code AlertGenerator} class is responsible for monitoring patient data
 * and generating alerts when certain predefined conditions are met. This class
 * relies on a {@link DataStorage} instance to access patient data and evaluate
 * it against specific health criteria.
 */
public class AlertGenerator {
    private DataStorage dataStorage;

    /**
     * Constructs an {@code AlertGenerator} with a specified {@code DataStorage}.
     * The {@code DataStorage} is used to retrieve patient data that this class
     * will monitor and evaluate.
     *
     * @param dataStorage the data storage system that provides access to patient
     *                    data
     */
    public AlertGenerator(DataStorage dataStorage) {
        this.dataStorage = dataStorage;
    }

    /**
     * Pulls all the records for the patient and runs every alert check.
     *
     * @param patient the patient data to evaluate for alert conditions
     */
    public void evaluateData(Patient patient) {
        long now = System.currentTimeMillis();
        List<PatientRecord> records = patient.getRecords(0, now);

        checkBloodPressure(patient, records);
        checkBloodSaturation(patient, records);
        checkHypotensitiveHypoxemia(patient, records);
        checkECG(patient, records);
        checkTriggeredAlert(patient, records);
    }

    /**
     * Checks for blood pressure events (trending upwards/downwards and values outside normal range).
     *
     * @param patient the person whose BP gets checked
     * @param records value of a person's BP
     */
    private void checkBloodPressure(Patient patient, List<PatientRecord> records) {
        List<PatientRecord> systolic = filterByType(records, "SystolicPressure");
        List<PatientRecord> diastolic = filterByType(records, "DiastolicPressure");

        checkBPTrend(patient, systolic, "Systolic");
        checkBPTrend(patient, diastolic, "DiastolicPressure");
        checkBPCritical(patient, systolic, diastolic);
    }

    /**
     * Screens for BP trends (upwards or downwards). Creates an alert
     * if catches a trend that's trending faster than it should.
     *
     * @param patient person whose BP gets evaluated
     * @param records value of blood pressure
     * @param label systolic or diastolic blood pressure
     */
    private void checkBPTrend(Patient patient, List<PatientRecord> records, String label) {
        for (int i = 2; i < records.size(); i++) {
            double a = records.get(i - 2).getMeasurementValue();
            double b = records.get(i - 1).getMeasurementValue();
            double c = records.get(i).getMeasurementValue();

            boolean increasing = (b - a > 10) && (c - b > 10);
            boolean decreasing = (a - b > 10) && (b - c > 10);

            if (increasing) {
                triggerAlert(new Alert(String.valueOf(patient.getPatientId()),
                        label + "BP Increasing Trend",
                        records.get(i).getTimestamp()
                ));
            } else if (decreasing) {
                triggerAlert(new Alert(String.valueOf(patient.getPatientId()),
                        label + "BP Decreasing Trend",
                        records.get(i).getTimestamp()
                ));
            }
        }
    }

    /**
     * Evaluates for dangerous values, whether too low or too high, using both systolic
     * and diastolic values. Creates a new alert if it detects an extreme value.
     *
     * @param patient person whose BP gets evaluated
     * @param systolic systolic part of the BP value
     * @param diastolic diastolic part of the BP value
     */
    private void checkBPCritical(Patient patient, List<PatientRecord> systolic, List<PatientRecord> diastolic) {
        for (PatientRecord record : systolic) {
            double value = record.getMeasurementValue();
            if (value > 180) {
                triggerAlert(new Alert(String.valueOf(patient.getPatientId()),
                         "Critical Systolic BP High: " + value, record.getTimestamp()
                ));
            } else if (value < 90) {
                triggerAlert(new Alert(String.valueOf(patient.getPatientId()),
                        "Critical Systolic BP Low: " + value, record.getTimestamp()
                ));
            }
        }

        for (PatientRecord record : diastolic) {
            double value = record.getMeasurementValue();
            if (value > 120) {
                triggerAlert(new Alert(String.valueOf(patient.getPatientId()),
                        "Critical Diastolic BP High: " + value, record.getTimestamp()
                ));
            } else if (value < 60) {
                triggerAlert(new Alert(String.valueOf(patient.getPatientId()),
                        "Critical Diastolic BP Low: " + value, record.getTimestamp()
                ));
            }
        }
    }

    /**
     * Evaluates the blood saturation of a given patient, creating a new alert
     * if it detects a low blood saturation or if it drops by more than 5.0%
     * in the last 10-minute window.
     *
     * @param patient person whose blood saturation gets evaluated
     * @param records
     */
    private void checkBloodSaturation(Patient patient, List<PatientRecord> records) {
        List<PatientRecord> saturationRecords = filterbyType(records, "Saturation");

        for (int i = 0; i < records.size(); i++) {
            PatientRecord current = saturationRecords.get(i);
            double value = current.getMeasurementValue();

            if (value < 92.0) {
                triggerAlert(new Alert(String.valueOf(patient.getPatientId()),
                        "Low Blood Saturation: " + value + "%",
                        current.getTimestamp()
                ));
            }

            //Comparing against all earlier readings in the 10-minute window for a rapid drop
            for (int j = i - 1; j >= 0; j--) {
                PatientRecord earlier = saturationRecords.get(j);
                long windowMs = 10 * 60 * 1000L; //10 minutes

                if (current.getTimestamp() - earlier.getTimestamp() > windowMs) {
                    break;
                }

                double drop = earlier.getMeasurementValue() - value;
                if (drop >= 5.0) {
                    triggerAlert(new Alert(String.valueOf(patient.getPatientId()),
                            "Rapid Blood saturation Drop: " + drop + "%",
                            current.getTimestamp()
                    ));
                    break;
                }
            }
        }
    }

    /**
     * If it detects an abnormally low blood pressure, checks whether there has also been
     * low blood saturation. If there has, it creates an alert for hypotensive hypoxemia.
     *
     * @param patient person whose records are getting evaluated
     * @param records values getting evaluated
     */
    private void checkHypotensiveHypoxemia(Patient patient, List<PatientRecord> records) {
        List<PatientRecord> systolic = filterByType(records, "SystolicPressure");
        List<PatientRecord> saturation = filterByType(records, "Saturation");

        for (PatientRecord bpRecord : systolic) {
            if (bpRecord.getMeasurementValue() >= 90) continue;

            for (PatientRecord saturationRecord : saturation) {
                if (saturationRecord.getMeasurementValue() < 92.0) {
                    triggerAlert( new Alert(String.valueOf(patient.getPatientId()),
                            "Hypotensive Hypoexmia Alert",
                            Math.max(bpRecord.getTimestamp(), saturationRecord.getTimestamp())
                    ));
                    return;
                }
            }
        }
    }

    private void checkECG(Patient patient, List<PatientRecord> records) {
        //TODO implement checkECG method
    }

    private List<PatientRecord> filterByType(List<PatientRecord> records, String type) {
        List<PatientRecord> result = new ArrayList<>();
        for (PatientRecord record : records) {
            if (type.equals(record.getRecordType())) {
                result.add(record);
            }
        }
        return result;
    }

    /**
     * Triggers an alert for the monitoring system. This method can be extended to
     * notify medical staff, log the alert, or perform other actions. The method
     * currently assumes that the alert information is fully formed when passed as
     * an argument.
     *
     * @param alert the alert object containing details about the alert condition
     */
    private void triggerAlert(Alert alert) {
        // Implementation might involve logging the alert or notifying staff
    }
}
