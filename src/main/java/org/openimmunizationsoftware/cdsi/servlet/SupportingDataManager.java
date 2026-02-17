package org.openimmunizationsoftware.cdsi.servlet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;

public class SupportingDataManager {

    public static final String CONTEXT_PARAM_DEFAULT_SUPPORTING_DATA_SET = "defaultSupportingDataSet";

    private static final String CLASS_PATH_ROOT = "/WEB-INF/classes/";
    private static final String CLASS_PATH_SUPPORTING_DATA = "/WEB-INF/classes/supporting-data/";
    private static final String WEBAPP_PATH_SUPPORTING_DATA = "/supporting-data/";

    public static List<String> listSupportingDataSetIds(ServletContext servletContext) {
        List<String> zipNameList = new ArrayList<String>();

        addServletContextZipNames(servletContext, CLASS_PATH_ROOT, zipNameList, false);
        addServletContextZipNames(servletContext, CLASS_PATH_SUPPORTING_DATA, zipNameList, true);
        addServletContextZipNames(servletContext, WEBAPP_PATH_SUPPORTING_DATA, zipNameList, true);

        File supportingDataDir = new File("supporting-data");
        if (supportingDataDir.exists() && supportingDataDir.isDirectory()) {
            File[] files = supportingDataDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().toLowerCase().endsWith(".zip")) {
                        zipNameList.add(file.getName());
                    }
                }
            }
        }

        List<String> setIdList = new ArrayList<String>();
        for (String zipName : zipNameList) {
            String setId = normalizeSetId(zipName);
            if (!setIdList.contains(setId)) {
                setIdList.add(setId);
            }
        }

        Collections.sort(setIdList, String.CASE_INSENSITIVE_ORDER);
        return setIdList;
    }

    public static String resolveDefaultSupportingDataSet(ServletContext servletContext) {
        List<String> setIdList = listSupportingDataSetIds(servletContext);
        if (setIdList.isEmpty()) {
            return null;
        }

        String configuredDefault = servletContext.getInitParameter(CONTEXT_PARAM_DEFAULT_SUPPORTING_DATA_SET);
        if (configuredDefault != null && !configuredDefault.trim().equals("")) {
            String configuredSetId = normalizeSetId(configuredDefault.trim());
            for (String setId : setIdList) {
                if (setId.equalsIgnoreCase(configuredSetId)) {
                    return setId;
                }
            }
        }

        return setIdList.get(0);
    }

    public static String normalizeSetId(String value) {
        if (value == null) {
            return null;
        }
        String s = value.trim();
        if (s.toLowerCase().endsWith(".zip")) {
            s = s.substring(0, s.length() - 4);
        }
        return s;
    }

    @SuppressWarnings("unchecked")
    private static void addServletContextZipNames(ServletContext servletContext, String folderPath,
            List<String> zipNameList, boolean recursive) {

        Set<String> pathSet = servletContext.getResourcePaths(folderPath);
        if (pathSet == null) {
            return;
        }

        for (String path : pathSet) {
            if (path.endsWith("/")) {
                if (recursive) {
                    addServletContextZipNames(servletContext, path, zipNameList, true);
                }
                continue;
            }

            if (path.toLowerCase().endsWith(".zip")) {
                int pos = path.lastIndexOf('/');
                zipNameList.add(pos < 0 ? path : path.substring(pos + 1));
            }
        }
    }
}
