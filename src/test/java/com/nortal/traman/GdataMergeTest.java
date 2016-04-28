package com.nortal.traman;

import static org.junit.Assert.assertEquals;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Arne Lapõnin (Arne.Laponin@nortal.com)
 * @author Tanel Käär (Tanel.Kaar@nortal.com)
 */
public class GdataMergeTest {

  private static final String KEY1 = "test.123.one";
  private static final String KEY2 = "test.123.two";
  private static final String KEY3 = "test.123.three";
  private static final String KEY4 = "test.empty.row";
  private static final String KEY5 = "test.emty.row2";
  private static final String KEY6 = "test.new.from.dictionary";
  private static final String KEY_H = "test.hierarchical.directory";
  
  private static final String VALUE_VALID = "Valid value ÕÄÖÜŠŽõäöüšž";
  private static final String VALUE_DUMMY = "[DUMMY]Dummy value";
  private static final String VALUE_EMPTY = "";
  private static final String VALUE_MULTIPLE_LINES = "Value\\non\\n\\nmultiple\\n\\n\\nlines\\\"";
  private static final String VALUE_WITH_SEVERAL_APOSTROPHES = "Value\"with\"\"apostrophes'''' '";
  private static final String VALUE_H = "Hierarchical value";
  
  private static final String RESOURCE__EN_PROPERTIES = "build/resources/test/test-resources_en.properties";
  private static final String RESOURCE__ET_PROPERTIES = "build/resources/test/test-resources_et.properties";
  private static final String RESOURCE___HIER_EN_PROPERTIES = "build/resources/test/hierarchical/test-resources_en.properties";
  private static final String RESOURCE___HIER_ET_PROPERTIES = "build/resources/test/hierarchical/test-resources_et.properties";
  private static final String GDATA_RESOURCE_KEY = "11yvPVnGQ2bE5M7w1gCZeFHLL2BRt8DswColOKXAix4M";
  
  GdataTranslator gdataResources;

  @Before
  public void setUp() {
    String workSheetName = "util-test";
    String[] langs = {"en", "et"};
    gdataResources = new GdataTranslator(GDATA_RESOURCE_KEY, langs, workSheetName);
    gdataResources.setResourcesDirectory("build/resources/test");
  }

  private void setGdataLocal(Properties resources, String propertyFile) {
    gdataResources.localResources.put(propertyFile, resources);
  }

  private void setGdataDictionary(Properties resources, String propertyFile) {
    gdataResources.dictionaryResources.put(propertyFile, resources);
  }

  @Test
  public void testMerge() {
    Properties local = new Properties();
    local.put(KEY1, VALUE_DUMMY);
    setGdataLocal(local, RESOURCE__EN_PROPERTIES);
    Properties global = new Properties();
    global.put(KEY1, VALUE_VALID);
    setGdataDictionary(global, RESOURCE__EN_PROPERTIES);
    gdataResources.merge();
    Properties result = gdataResources.localResources.get(RESOURCE__EN_PROPERTIES);
    assertEquals(VALUE_VALID, result.getProperty(KEY1));
  }

  @Test
  public void testMergeWithNull() {
    Properties local = new Properties();
    local.put(KEY1, VALUE_DUMMY);
    setGdataLocal(local, RESOURCE__EN_PROPERTIES);
    Properties global = new Properties();
    global.put(KEY1, "");
    setGdataDictionary(global, RESOURCE__EN_PROPERTIES);
    gdataResources.merge();
    Properties result = gdataResources.localResources.get(RESOURCE__EN_PROPERTIES);
    assertEquals(VALUE_DUMMY, result.getProperty(KEY1));
  }

  @Test
  public void testFindingDuplicate() {
    Properties local = new Properties();
    local.put(KEY1, VALUE_DUMMY);// overwritten by valid value
    local.put(KEY2, VALUE_DUMMY); // not modified - does not exist in dictionary
    local.put(KEY3, VALUE_DUMMY); // not modified by empty value
    local.put(KEY5, VALUE_DUMMY); // not modified by null value
    setGdataLocal(local, RESOURCE__EN_PROPERTIES);
    // set same values to et
    setGdataLocal(local, RESOURCE__ET_PROPERTIES);
    
    Properties global = new Properties();
    global.put(KEY1, VALUE_VALID); // overwritten
    global.put(KEY3, VALUE_EMPTY); // not overwritten
    global.put(KEY4, VALUE_VALID); // added
    global.put(KEY5, ""); // not overwritten
    setGdataDictionary(global, RESOURCE__EN_PROPERTIES);
    
    // ET has all values empty
    global = new Properties();
    global.put(KEY1, ""); // not overwritten
    global.put(KEY2, ""); // not overwritten
    global.put(KEY3, ""); // not overwritten
    global.put(KEY4, ""); // not overwritten
    global.put(KEY5, ""); // not overwritten
    setGdataDictionary(global, RESOURCE__ET_PROPERTIES);
    
    gdataResources.merge();
    
    Properties result = gdataResources.localResources.get(RESOURCE__EN_PROPERTIES);
    assertEquals(5, result.keySet().size());
    assertEquals(VALUE_VALID, result.getProperty(KEY1));
    assertEquals(VALUE_DUMMY, result.getProperty(KEY2));
    assertEquals(VALUE_DUMMY, result.getProperty(KEY3));
    assertEquals(VALUE_VALID, result.getProperty(KEY4));
    assertEquals(VALUE_DUMMY, result.getProperty(KEY5));
    Assert.assertNull(result.getProperty(KEY6));
    
    result = gdataResources.localResources.get(RESOURCE__ET_PROPERTIES);
    assertEquals(5, result.keySet().size());
    assertEquals(VALUE_VALID, result.getProperty(KEY1));
    assertEquals(VALUE_DUMMY, result.getProperty(KEY2));
    assertEquals(VALUE_DUMMY, result.getProperty(KEY3));
    assertEquals(VALUE_VALID, result.getProperty(KEY4));
    assertEquals(VALUE_DUMMY, result.getProperty(KEY5));
    Assert.assertNull(result.getProperty(KEY6));
  }
  
  @Test
  public void gdataTranslate() throws IOException {
    Properties local = new Properties();
    local.put(KEY1, VALUE_DUMMY);// overwritten by valid value
    local.put(KEY2, VALUE_DUMMY); // overwritten by valid value
    local.put(KEY3, VALUE_DUMMY); // not modified by empty value
    local.put(KEY5, VALUE_DUMMY); // not modified by null value
    writePropertyFile(local, RESOURCE__EN_PROPERTIES);
    
    // set same values to et
    writePropertyFile(local, RESOURCE__ET_PROPERTIES);

    gdataResources.translate();
    
    // reload local resources
    gdataResources.readPropertyFiles();
    
    Properties result = gdataResources.localResources.get(RESOURCE__EN_PROPERTIES);
    assertEquals(5, result.keySet().size());
    assertEquals(VALUE_VALID, result.getProperty(KEY1));
    assertEquals(VALUE_MULTIPLE_LINES, result.getProperty(KEY2));
    assertEquals(VALUE_DUMMY, result.getProperty(KEY3));
    assertEquals(VALUE_DUMMY, result.getProperty(KEY5));
    assertEquals(VALUE_WITH_SEVERAL_APOSTROPHES, result.getProperty(KEY6));
    
    result = gdataResources.localResources.get(RESOURCE__ET_PROPERTIES);
    assertEquals(5, result.keySet().size());
    assertEquals(VALUE_VALID, result.getProperty(KEY1));
    assertEquals(VALUE_MULTIPLE_LINES, result.getProperty(KEY2));
    assertEquals(VALUE_DUMMY, result.getProperty(KEY3));
    assertEquals(VALUE_DUMMY, result.getProperty(KEY5));
    assertEquals(VALUE_WITH_SEVERAL_APOSTROPHES, result.getProperty(KEY6));
    Assert.assertNull(result.getProperty(KEY4));
    assertEquals(VALUE_DUMMY, result.getProperty(KEY5));

    // new hierarchical resource created
    result = gdataResources.localResources.get(RESOURCE___HIER_EN_PROPERTIES);
    assertEquals(1, result.keySet().size());
    assertEquals(VALUE_H, result.getProperty(KEY_H));

    result = gdataResources.localResources.get(RESOURCE___HIER_ET_PROPERTIES);
    assertEquals(1, result.keySet().size());
    assertEquals(VALUE_H, result.getProperty(KEY_H));
}
  
  private void writePropertyFile(Properties resources, String propertyFile) throws IOException {
    FileWriter fileWriter = new FileWriter(propertyFile);
    resources.store(fileWriter, "saved as property files");
    fileWriter.close();
  }
}
 