package com.nortal.traman;

import com.google.gdata.client.spreadsheet.FeedURLFactory;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.BaseFeed;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.ServiceException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author Arne Lapõnin (Arne.Laponin@nortal.com)
 * @author Tanel Käär (Tanel.Kaar@nortal.com)
 */
public class GdataTranslator {

  private static final String COLUMN__LOCATION = "location";
  private static final String COLUMN__RESOURCE_KEY = "resourcekey";

  private static final Logger LOGGER = Logger.getLogger(GdataTranslator.class.getName());

  private String spreadsheetKey;
  private String workSheetName;
  protected Map<String, Properties> localResources = new HashMap<String, Properties>();
  protected Map<String, Properties> dictionaryResources = new HashMap<String, Properties>();
  private List<String> firstTimeWrite = new ArrayList<String>();
  private String resourcesDirectory = "resources";
  private String[] langs;

  public GdataTranslator(String spreadsheetKey, String[] langs, String worksheetName) {
    this.spreadsheetKey = spreadsheetKey;
    this.langs = langs;
    this.workSheetName = worksheetName;
  }

  public void translate() {
    this.readPropertyFiles();
    this.readDictionaryResources();
    this.merge();
    this.writeResultToFiles();
  }

  /**
   * Local ResourceBundles from folder 'resources' are read. Bundles are put into a map, with a key, that consists of
   * bundle path and bundle name.
   */
  protected void readPropertyFiles() {
    File resourcesFileDir = new File(resourcesDirectory);
    if (!resourcesFileDir.exists() || !resourcesFileDir.isDirectory()) {
      throw new RuntimeException("Given directory '" + this.resourcesDirectory + "' doesn't exist!");
    }
    List<File> resourceFiles = findExistingFiles(resourcesFileDir);

    for (File resource : resourceFiles) {
      String filePath = (resource.getPath()).replace('\\', '/');
      Properties fileResources = loadResources(resource);
      String language = getLanguage(resource);
      LOGGER.info("Mapped resources for language '" + language + "'");
      localResources.put(filePath, fileResources);
    }
  }

  /**
   * Find all resource files recursivelly 
   */
  private List<File> findExistingFiles(File resourcesFileDir) {
    final List<File> resources = new ArrayList<File>();

    File[] dirListing = resourcesFileDir.listFiles(new FileFilter() {

      @Override
      public boolean accept(File pathname) {
        if(pathname == null) {
          return false;
        }
        // travel directories recursivelly
        if(pathname.isDirectory()) {
          resources.addAll(findExistingFiles(pathname));
        }
        return pathname.isFile() &&
               pathname.getName().endsWith(".properties");
      }
    });
    resources.addAll(Arrays.asList(dirListing));
    return resources;
  }

  private String getLanguage(File file) {
    String fileName = file.getName();
    return getLanguage(fileName);
  }

  private static String getLanguage(String fileName) {
    int indexOfUnderscore = fileName.lastIndexOf('_') + 1;
    return fileName.substring(indexOfUnderscore, indexOfUnderscore + 2);
  }

  /**
   * Method constructs Properties from a local file that contains resources.
   * 
   * @param resourceFile local file that contains resources
   * @return ResourceBundle constructed from local file
   */
  private static Properties loadResources(File resourceFile) {
    LOGGER.info("Loading resources for file '" + resourceFile.getName() + "'");
    Properties properties = new Properties();
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(resourceFile), "UTF-8"));
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("#")) {
          continue;
        }

        int index = line.indexOf("=");
        if (index == -1 || index == 0) {
          continue;
        }

        String key = line.substring(0, index).trim();
        String value = line.substring(index + 1).trim();
        properties.put(key, value);
      }
    } catch (FileNotFoundException e) {
      throw new RuntimeException("File reading is not possible '" + resourceFile.getName() + "'", e);
    } catch (IOException e) {
      throw new RuntimeException("File reading is not possible '" + resourceFile.getName() + "'", e);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          throw new RuntimeException("Error closing file input stream", e);
        }
      }
    }
    LOGGER.info(properties.keySet().size() + " properties are loaded from '" + resourceFile.getName() + "'");
    return properties;
  }

  protected void readDictionaryResources() {
    URL url = createSpreadsheetUrl();
    ListFeed listFeed = getListFeedFromUrl(url);
    LOGGER.info("Loaded " + listFeed.getEntries().size() + " rows from global dictionary.");
    for (String lang : langs) {
      loadResourcesFromListFeed(listFeed, lang);
      LOGGER.info("Mapped dictionary resources for language '" + lang + "'");
    }
  }

  private URL createSpreadsheetUrl() {
    LOGGER.info("Loading Google Spreadsheet dictionary resources from url.");
    URL url;
    try {
      url = FeedURLFactory.getDefault().getWorksheetFeedUrl(spreadsheetKey, "public", "full");
    } catch (MalformedURLException e) {
      throw new RuntimeException("Reading is not possible from url", e);
    }
    return url;
  }

  private ListFeed getListFeedFromUrl(URL resourceUrl) {
    SpreadsheetService service = new SpreadsheetService("Translator");
    service.setConnectTimeout(10000);
    WorksheetFeed feed = getFeed(service, resourceUrl, WorksheetFeed.class);
    LOGGER.info("Loading resources from worksheet '" + workSheetName + "'.");
    WorksheetEntry worksheetEntry = getWorkSheetEntry(feed, workSheetName);
    if (worksheetEntry == null) {
      throw new RuntimeException("Worksheet with name '" + workSheetName + "' does not exist.");
    }

    URL listFeedUrl = worksheetEntry.getListFeedUrl();
    return getFeed(service, listFeedUrl, ListFeed.class);
  }

  private static <T extends BaseFeed<?, ?>> T getFeed(SpreadsheetService service, URL url, Class<T> feedType) {
    T feed;
    try {
      feed = service.getFeed(url, feedType);
    } catch (IOException e) {
      throw new RuntimeException("ERROR: Can't get anything from " + feedType.getSimpleName(), e);
    } catch (ServiceException e) {
      throw new RuntimeException("ERROR: Can't get anything from " + feedType.getSimpleName(), e);
    }
    return feed;
  }

  private static WorksheetEntry getWorkSheetEntry(WorksheetFeed feed, String worksheetTitle) {
    List<WorksheetEntry> worksheetList = feed.getEntries();
    WorksheetEntry worksheetEntry = null;
    for (WorksheetEntry worksheet : worksheetList) {
      if (worksheet.getTitle().getPlainText().equals(worksheetTitle)) {
        worksheetEntry = worksheet;
      }
    }
    return worksheetEntry;
  }

  /**
   * Method takes a row from Google Spreadsheet listFeed and parse into key-value and inserted into ResourceBundle. Key
   * consists of Bundle name and location. Value is a Bundle.
   * 
   * @param listFeed Taken from Google Spreadsheet
   * @param lang Language prefix
   */
  private void loadResourcesFromListFeed(ListFeed listFeed, String lang) {
    // Map<String, Properties> addedResources = new HashMap<String, Properties>();

    for (ListEntry row : listFeed.getEntries()) {
      String locationBasedKey = getLocationKey(lang, row);

      // read only properties which have key value
      if (locationBasedKey == null || locationBasedKey.isEmpty()) {
        continue;
      }

      Properties properties;
      if (dictionaryResources.containsKey(locationBasedKey)) {
        properties = dictionaryResources.get(locationBasedKey);
      } else {
        properties = new Properties();
        dictionaryResources.put(locationBasedKey, properties);
      }
      addProperties(lang, row, properties);
    }
  }

  /**
   * Method constructs key, which will later be used in a map with ResourceBundle, from Spreadsheet location column data
   * and language prefix. Language placeholder is removed and language prefix is inserted
   * 
   * @param lang Language for which key is constructed
   * @param row Google Spreadsheet row from which data is taken
   * @return Key constructed from Google Spreadsheet location column and language prefix
   */
  private static String getLocationKey(String lang, ListEntry row) {
    String resourceKey = row.getCustomElements().getValue(COLUMN__RESOURCE_KEY);
    // ignore "empty" lines
    if (resourceKey == null) {
      return null;
    }

    String resourceLocation = row.getCustomElements().getValue(COLUMN__LOCATION);
    if (resourceLocation == null) {
      LOGGER.severe("Resource key " + resourceKey + " has no location. Location has to be added.");
      return null;
    }
    String[] locationValues;
    if (resourceLocation.contains("[LANG]")) {
      locationValues = resourceLocation.split(Pattern.quote("[LANG]"));
    } else {
      throw new RuntimeException("Spreadsheet location has to contain [LANG]: " + resourceLocation);
    }
    if (locationValues.length != 2) {
      throw new RuntimeException("Spreadsheet location can contain only one language placeholder.");
    }
    return locationValues[0] + lang + locationValues[1];
  }

  /**
   * Method parses row data into key-value pair
   * 
   * @param lang Language prefix
   * @param row Google Spreadsheet row, from which data is taken
   * @param properties Data structure to hold property, which will later be inserted into ResourceBundle
   */
  private static void addProperties(String lang, ListEntry row, Properties properties) {
    String value = row.getCustomElements().getValue(lang);
    String key = row.getTitle().getPlainText();
    if (value != null && value.length() > 0) {
      properties.put(key, value);
    }
  }

  protected void merge() {
    // Files not present in local project
    createMissingLocalFiles();

    for (String bundleKey : localResources.keySet()) {
      // Changed values
      overwriteValues(bundleKey);
      // Added values
      addValues(bundleKey);
    }
  }

  private void addValues(String bundleKey) {
    Properties resources = localResources.get(bundleKey);
    Properties dictionary = dictionaryResources.get(bundleKey);
    
    if(dictionary == null) {
      return;
    }

    for (Entry<Object, Object> dictionaryEntry : dictionary.entrySet()) {
      if (!resources.containsKey(dictionaryEntry.getKey())) {
        resources.put(dictionaryEntry.getKey(), dictionaryEntry.getValue());
      }
    }
  }

  private void createMissingLocalFiles() {
    for (String bundleKey : dictionaryResources.keySet()) {
      if (!localResources.containsKey(bundleKey)) {
        writeResultToFile(dictionaryResources, bundleKey);
        firstTimeWrite.add(getResultFilename(bundleKey));
        LOGGER.info("Adding file: " + getResultFilename(bundleKey));
      }
    }
  }

  protected void overwriteValues(String bundleKey) {
    Properties local = localResources.get(bundleKey);
    Properties dictionary = dictionaryResources.get(bundleKey);
    if (dictionary == null) {
      return;
    }
    for (String key : local.stringPropertyNames()) {
      String value = dictionary.getProperty(key);
      if (value != null && value.length() > 0) {
        local.put(key, value);
      }
    }
  }

  protected void writeResultToFiles() {
    for (String bundleKey : localResources.keySet()) {
      String filename = getResultFilename(bundleKey);
      if (!firstTimeWrite.contains(filename)) {
        writeResultToFile(localResources, bundleKey);
        LOGGER.info("Overwriting file: " + filename);
      }

    }
  }

  private void writeResultToFile(Map<String, Properties> resource, String bundleKey) {
//    String filename = getResultPath(bundleKey);
    File file = getOutputFile(bundleKey);

    OutputStreamWriter fileWriter = null;
    try {
      fileWriter = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");

      List<String> keys = new ArrayList<String>(resource.get(bundleKey).stringPropertyNames());
      Collections.sort(keys);
      for (String key : keys) {
        String value = resource.get(bundleKey).getProperty(key);
        if (value == null) {
          value = "";
        }
        fileWriter.write(key + "=" + value + "\n");
      }
      fileWriter.write("\n");
      fileWriter.flush();
    } catch (IOException e) {
      throw new RuntimeException("Cannot write to '" + file.getAbsolutePath() + "'");
    } finally {
      try {
        if (fileWriter != null) {
          fileWriter.close();
        }
      } catch (IOException e) {
        throw new RuntimeException("Cannot write to '" + file.getAbsolutePath() + "'");
      }
    }
  }

  /**
   * Check format, create missing subdirectories
   */
  private File getOutputFile(String fileLocation) {
    if(fileLocation == null) {
      throw new IllegalArgumentException("File location is null!");
    }
    if(!fileLocation.startsWith(resourcesDirectory)) {
      throw new IllegalArgumentException("Invalid resource location: '" + fileLocation + "'! Location must begin with '" + resourcesDirectory + "'.");
    }
    File file = new File(fileLocation);
    
    String path = file.getPath();
    String name = file.getName();
    String dir = path.substring(0, path.length() - name.length());
    
    // create missing subdirectories
    new File(dir).mkdirs();
    
    return file;
  }

  protected static String getResultFilename(String key) {
    if (key.contains("\\")) {
      return key.substring(key.lastIndexOf("\\") + 1);
    }
    return key.substring(key.lastIndexOf("/") + 1);
  }

  protected String getResultPath(String filename) {
    return (resourcesDirectory + File.separator + getResultFilename(filename)).replace('\\', '/');
  }

  public void setResourcesDirectory(String resourcesDirectory) {
    this.resourcesDirectory = resourcesDirectory;
  }
}
