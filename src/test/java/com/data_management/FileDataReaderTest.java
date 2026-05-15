package com.data_management;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileDataReaderTest {
    @TempDir
    Path tempDir;

    private DataStorage dataStorage;

    @BeforeEach
    void setUp() {
        dataStorage = new DataStorage();
    }

    @Test
    void testReadsValidFileAndStoresRecords() throws IOException {
        writeLines("patient1.txt",
                "1,98.6,Temperature,1700000000000",
                "1,120.0,SystolicPressure,1700000001000"
        );

        new FileDataReader(tempDir.toString()).readData(dataStorage);

        List<PatientRecord> records =
                dataStorage.getRecords(1, 0L, Long.MAX_VALUE);
        assertEquals(2, records.size());
    }

    @Test
    void testReadsMultipleFilesInDirectory() throws IOException {
        writeLines("fileA.txt", "2,75.0,HeartRate,1700000000000");
        writeLines("fileB.txt", "3,99.0,Temperature,1700000000000");

        new FileDataReader(tempDir.toString()).readData(dataStorage);

        assertFalse(dataStorage.getRecords(2, 0L, Long.MAX_VALUE).isEmpty());
        assertFalse(dataStorage.getRecords(3, 0L, Long.MAX_VALUE).isEmpty());
    }

    //Edge cases

    @Test
    void testEmptyDirectoryDoesNotThrow() {
        assertDoesNotThrow(() ->
                new FileDataReader(tempDir.toString()).readData(dataStorage)
        );
    }

    @Test
    void testBlankLinesAreSkipped() throws IOException {
        writeLines("blanks.txt",
                "",
                "   ",
                "4,80.0,HeartRate,1700000000000"
        );

        new FileDataReader(tempDir.toString()).readData(dataStorage);

        List<PatientRecord> records =
                dataStorage.getRecords(4,0L, Long.MAX_VALUE);
        assertEquals(1, records.size());
    }

    @Test
    void testMalformedLineIsSkipped() throws IOException {
        writeLines("bad.txt",
                "not,a,valid,line,at,all",
                "5,72.0,HeartRate,1700000000000"
        );

        assertDoesNotThrow(() ->
                new FileDataReader(tempDir.toString()).readData(dataStorage)
        );

        //The valid line should still be stored
        assertFalse(dataStorage.getRecords(5, 0L, Long.MAX_VALUE).isEmpty());    }

    @Test
    void testNonNumericFieldsAreSkipped() throws IOException {
        writeLines("numeric.txt",
                "abc,72.0,HeartRate,1700000000000",  // bad patientId
                "6,xyz,HeartRate,1700000000000",      // bad value
                "6,72.0,HeartRate,notATimestamp"      // bad timestamp
        );

        assertDoesNotThrow(() ->
                new FileDataReader(tempDir.toString()).readData(dataStorage)
        );
    }

    @Test
    void testInvalidDirectoryThrowsIOException() {
        FileDataReader reader = new FileDataReader("/this/does/not/exist");
        assertThrows(IOException.class, () -> reader.readData(dataStorage));
    }

    // --- Helper ---

    private void writeLines(String filename, String... lines) throws IOException {
        Path file = tempDir.resolve(filename);
        Files.write(file, List.of(lines));
    }
}
