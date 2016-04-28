package com.nortal.traman;

import static com.nortal.traman.GdataTranslator.getResultFilename;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Arne Lapõnin (Arne.Laponin@nortal.com)
 * @author Tanel Käär (Tanel.Kaar@nortal.com)
 */
public class GdataResourcesTest {

    // spreadsheet for testing purposes: https://docs.google.com/spreadsheets/d/11yvPVnGQ2bE5M7w1gCZeFHLL2BRt8DswColOKXAix4M/edit?usp=sharing
    private static final String GDATA_RESOURCE_KEY = "11yvPVnGQ2bE5M7w1gCZeFHLL2BRt8DswColOKXAix4M";
    GdataTranslator gdataResources;

    @Before
    public void setUp() {
      String workSheetName = "util-test";
      String[] langs = {"en", "et", "fi", "lt", "ru"};
      gdataResources = new GdataTranslator(GDATA_RESOURCE_KEY, langs, workSheetName);
    }

    @Test
    public void testLinuxFileName() {
        String linuxFileName = getResultFilename("src/main/test/resources/some-resources_lt.properties");
        assertEquals(linuxFileName, "some-resources_lt.properties");
    }

    @Test
    public void testWindowsFileName() {
        String windowsFileName = getResultFilename("src\\main\\test\\resources\\some-resources_lt.properties");
        assertEquals(windowsFileName, "some-resources_lt.properties");
    }

    @Test
    public void testLinuxFilePath() {
        String linuxFileName = getResultFilename("src/main/test/resources/some-resources_lt.properties");
        String linuxFilePath = gdataResources.getResultPath(linuxFileName);
        assertEquals(linuxFilePath, "resources/some-resources_lt.properties");
    }

    @Test
    public void testWindowsFilePath() {
        String windowsFileName = getResultFilename("src\\main\\test\\resources\\some-resources_lt.properties");
        String windowsFilePath = gdataResources.getResultPath(windowsFileName);
        assertEquals(windowsFilePath, "resources/some-resources_lt.properties");
    }
    
    @Test
    public void fetchGlobalDirectory() {
      gdataResources.readDictionaryResources();
    }
}
