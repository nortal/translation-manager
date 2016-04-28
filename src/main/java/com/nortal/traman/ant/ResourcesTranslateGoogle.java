package com.nortal.traman.ant;

import com.nortal.traman.GdataTranslator;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * @author Arne Lapõnin (Arne.Laponin@nortal.com)
 * @author Tanel Käär (Tanel.Kaar@nortal.com)
 */
public class ResourcesTranslateGoogle extends Task {

    protected String[] langs; // all possible languages for properties' files
    protected GdataTranslator gdataTranslator;
    private String spreadsheetKey;
    private String workSheetName;
    private String resourcesDirName;

    public void setSpreadsheetKey(String spreadsheetKey) {
        this.spreadsheetKey = spreadsheetKey;
    }

    @Override
    public void execute() throws BuildException {
        gdataTranslator = new GdataTranslator(spreadsheetKey, langs, workSheetName);
        if(resourcesDirName != null) {
          gdataTranslator.setResourcesDirectory(resourcesDirName);
        }
        gdataTranslator.translate();
    }

    public void setWorkSheetName(String workSheetName) {
      this.workSheetName = workSheetName;
    }

    public void setResourcesDirName(String resourcesDirName) {
      this.resourcesDirName = resourcesDirName;
    }

    public void setLangs(String langs) {
      this.langs = langs.split(",");
    }

}