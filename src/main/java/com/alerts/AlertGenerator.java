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
    private static final int ECG_WINDOW_SIZE = 20;
    private static final double ECG_PEAK_FACTOR = 2.0; //multiplier used to determine abnormal peaks
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
     * @param patient the patient whose data is to be evaluated
     */
    public void evaluateData(Patient patient) {
        long now = System.currentTimeMillis();
        List<PatientRecord> records = patient.getRecords(0, now);

        checkBloodPressure(patient, records);
        checkBloodSaturation(patient, records);
        checkHypotensiveHypoxemia(patient, records);
        checkECG(patient, records);
        checkTriggeredAlert(patient, records);
    }

    /**
     * Checks for blood pressure events (trending upwards/downwards and values outside normal range).
     *
     * @param patient the person whose BP gets checked
     * @param records the patient's full list of health records
     */
    private void checkBloodPressure(Patient patient, List<PatientRecord> records) {
        List<PatientRecord> systolic = filterByType(records, "SystolicPressure");
        List<PatientRecord> diastolic = filterByType(records, "DiastolicPressure");

        checkBPTrend(patient, systolic, "SystolicPressure");
        checkBPTrend(patient, diastolic, "DiastolicPressure");
        checkBPCritical(patient, systolic, diastolic);
    }

    /**
     * Screens for BP trends (upwards or downwards). Creates an alert
     * if catches a trend that's trending faster than it should.
     *
     * @param patient person whose BP gets evaluated
     * @param records the patient's full list of health records
     * @param label an identifier for the record type
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
     * @param systolic filtered list of systolic BP records
     * @param diastolic filtered list of diastolic BP records
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
     * @param patient person whose blood oxygen saturation gets evaluated
     * @param records the patient's full list of health records
     */
    private void checkBloodSaturation(Patient patient, List<PatientRecord> records) {
        List<PatientRecord> saturationRecords = filterByType(records, "Saturation");

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
     * @param records patient's full list of health records
     */
    private void checkHypotensiveHypoxemia(Patient patient, List<PatientRecord> records) {
        List<PatientRecord> systolic = filterByType(records, "SystolicPressure");
        List<PatientRecord> saturation = filterByType(records, "Saturation");

        for (PatientRecord bpRecord : systolic) {
            if (bpRecord.getMeasurementValue() >= 90) continue;

            for (PatientRecord saturationRecord : saturation) {
                if (saturationRecord.getMeasurementValue() < 92.0) {
                    triggerAlert( new Alert(String.valueOf(patient.getPatientId()),
                            "Hypotensive Hypoxemia Alert",
                            Math.max(bpRecord.getTimestamp(), saturationRecord.getTimestamp())
                    ));
                    return;
                }
            }
        }
    }

    /**
     * Analyzes patient's ECG readings using a sliding window average to detect abnormal peaks.
     * A peak is considered abnormal when it exceeds {@value ECG_PEAK_FACTOR} times the average
     * of the preceding {@value ECG_WINDOW_SIZE} readings. Evaluation only happens when
     * there are enough records.
     *
     * @param patient a person whose ECG readings get evaluated
     * @param records patient's full list of health record
     */

    private void checkECG(Patient patient, List<PatientRecord> records) {
        List<PatientRecord> ecgRecords = filterByType(records, "ECG");

        if (ecgRecords.size() < ECG_WINDOW_SIZE) return;

        for (int i = ECG_WINDOW_SIZE; i < ecgRecords.size(); i++) {
            double sum = 0;
            for (int j = i - ECG_WINDOW_SIZE; j < i; j++) {
                sum += ecgRecords.get(j).getMeasurementValue();
            }
            double windowAvg = sum / ECG_WINDOW_SIZE;

            double currentValue = ecgRecords.get(i).getMeasurementValue();

            if (windowAvg > 0 && currentValue > ECG_PEAK_FACTOR * windowAvg) {
                triggerAlert(new Alert(String.valueOf(patient.getPatientId()),
                        "Abnormal ECG Peak: " + currentValue + " (avg = " +
                        String.format("%.2f", windowAvg) + ")", ecgRecords.get(i).getTimestamp()
                ));
            }
        }
    }

    /**
     * Checks for manual alerts raised by hospital staff or the patient themselves.
     * A record with measurement {@code 1.0} indicates the button has been pressed,
     * while {@code 0.0} indicates it's been released.
     *
     * @param patient patient for whom the alarm has been triggered
     * @param records the patient's full list of health records
     */
    private void checkTriggeredAlert(Patient patient, List<PatientRecord> records) {
        List<PatientRecord> triggered = filterByType(records, "Alert");

        for  (PatientRecord record : triggered) {
            if (record.getMeasurementValue() == 1.0) {
                triggerAlert(new Alert(String.valueOf(patient.getPatientId()),
                        "Manual Alert Triggered", record.getTimestamp()
                ));
            }
        }
    }

    /**
     * Returns a filtered sublist containing only the records whose type matches
     * the specified string.
     *
     * @param records full list of records to filter
     * @param type record type to match the string (e.g., {@code "ECG"},
     *             {@code "Saturation"}
     * @return a new list containing only records of the given type
     */
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
        System.out.println("ALERT: Patient " + alert.getPatientId() + " | "
                + alert.getCondition() + " | " + alert.getTimestamp());
    }
}
