package org.openimmunizationsoftware.cdsi.servlet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.ServletContext;

public class SupportingDataManager {

    public static final String CONTEXT_PARAM_DEFAULT_SUPPORTING_DATA_SET = "defaultSupportingDataSet";
    public static final String DEFAULT_KNOWLEDGE_BASE_ID = "USA-CDC-CDSI";

    private static final String CLASS_PATH_ROOT = "/WEB-INF/classes/";
    private static final String CLASS_PATH_SUPPORTING_DATA = "/WEB-INF/classes/supporting-data/";
    private static final String WEBAPP_PATH_SUPPORTING_DATA = "/supporting-data/";
    private static final Pattern LEGACY_CDC_ZIP_PATTERN = Pattern
            .compile("^supporting-data-(\\d+(?:\\.\\d+)*)(?:-508)?\\.zip$", Pattern.CASE_INSENSITIVE);
    private static final Pattern KNOWLEDGE_BASE_ZIP_PATTERN = Pattern
            .compile("^(.+)-(\\d+(?:\\.\\d+)*)\\.zip$", Pattern.CASE_INSENSITIVE);

    public static List<String> listSupportingDataSetIds(ServletContext servletContext) {
        List<String> zipNameList = listSupportingDataZipNames(servletContext);

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

    public static List<String> listKnowledgeBaseIds(ServletContext servletContext) {
        List<SupportingDataDescriptor> descriptorList = listSupportingDataDescriptors(servletContext);
        List<String> knowledgeBaseIdList = new ArrayList<String>();
        for (SupportingDataDescriptor descriptor : descriptorList) {
            if (descriptor.knowledgeBaseId != null && !knowledgeBaseIdList.contains(descriptor.knowledgeBaseId)) {
                knowledgeBaseIdList.add(descriptor.knowledgeBaseId);
            }
        }
        Collections.sort(knowledgeBaseIdList, String.CASE_INSENSITIVE_ORDER);
        return knowledgeBaseIdList;
    }

    public static List<SupportingDataDescriptor> listSupportingDataDescriptors(ServletContext servletContext) {
        List<String> zipNameList = listSupportingDataZipNames(servletContext);
        Map<String, SupportingDataDescriptor> setIdToDescriptorMap = new LinkedHashMap<String, SupportingDataDescriptor>();

        for (String zipName : zipNameList) {
            SupportingDataDescriptor descriptor = parseSupportingDataDescriptor(zipName);
            if (descriptor != null && !setIdToDescriptorMap.containsKey(descriptor.setId)) {
                setIdToDescriptorMap.put(descriptor.setId, descriptor);
            }
        }

        List<SupportingDataDescriptor> descriptorList = new ArrayList<SupportingDataDescriptor>(
                setIdToDescriptorMap.values());
        Collections.sort(descriptorList);
        return descriptorList;
    }

    public static String resolveSupportingDataSetForKnowledgeBase(ServletContext servletContext,
            String knowledgeBaseId, String knowledgeBaseVersion) {
        List<SupportingDataDescriptor> descriptorList = listSupportingDataDescriptors(servletContext);
        if (descriptorList.isEmpty()) {
            return null;
        }

        String targetKnowledgeBaseId = knowledgeBaseId == null || knowledgeBaseId.trim().equals("")
                ? DEFAULT_KNOWLEDGE_BASE_ID
                : knowledgeBaseId.trim();

        List<SupportingDataDescriptor> filteredList = new ArrayList<SupportingDataDescriptor>();
        for (SupportingDataDescriptor descriptor : descriptorList) {
            if (descriptor.knowledgeBaseId != null
                    && descriptor.knowledgeBaseId.equalsIgnoreCase(targetKnowledgeBaseId)) {
                filteredList.add(descriptor);
            }
        }

        if (filteredList.isEmpty()) {
            return null;
        }

        String requestedVersion = knowledgeBaseVersion == null ? "" : knowledgeBaseVersion.trim();
        if (!requestedVersion.equals("")) {
            for (SupportingDataDescriptor descriptor : filteredList) {
                if (requestedVersion.equals(descriptor.version)) {
                    return descriptor.setId;
                }
            }
        }

        String latestVersion = null;
        for (SupportingDataDescriptor descriptor : filteredList) {
            if (descriptor.version == null || descriptor.version.equals("")) {
                continue;
            }
            if (latestVersion == null || VersionComparator.compareVersions(descriptor.version, latestVersion) > 0) {
                latestVersion = descriptor.version;
            }
        }

        if (latestVersion != null) {
            for (SupportingDataDescriptor descriptor : filteredList) {
                if (latestVersion.equals(descriptor.version)) {
                    return descriptor.setId;
                }
            }
        }

        return filteredList.get(0).setId;
    }

    public static SupportingDataDescriptor findSupportingDataDescriptor(ServletContext servletContext, String setId) {
        if (setId == null || setId.trim().equals("")) {
            return null;
        }

        String normalizedSetId = normalizeSetId(setId);
        List<SupportingDataDescriptor> descriptorList = listSupportingDataDescriptors(servletContext);
        for (SupportingDataDescriptor descriptor : descriptorList) {
            if (descriptor.setId.equalsIgnoreCase(normalizedSetId)) {
                return descriptor;
            }
        }
        return null;
    }

    private static List<String> listSupportingDataZipNames(ServletContext servletContext) {
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

        return zipNameList;
    }

    private static SupportingDataDescriptor parseSupportingDataDescriptor(String zipName) {
        if (zipName == null || zipName.trim().equals("")) {
            return null;
        }

        String normalizedZipName = zipName.trim();
        String setId = normalizeSetId(normalizedZipName);

        Matcher legacyMatcher = LEGACY_CDC_ZIP_PATTERN.matcher(normalizedZipName);
        if (legacyMatcher.matches()) {
            String version = legacyMatcher.group(1);
            return new SupportingDataDescriptor(setId, normalizedZipName, DEFAULT_KNOWLEDGE_BASE_ID, version);
        }

        Matcher kbMatcher = KNOWLEDGE_BASE_ZIP_PATTERN.matcher(normalizedZipName);
        if (kbMatcher.matches()) {
            String knowledgeBaseId = kbMatcher.group(1).trim();
            String version = kbMatcher.group(2).trim();
            if (!knowledgeBaseId.equals("") && !version.equals("")) {
                return new SupportingDataDescriptor(setId, normalizedZipName, knowledgeBaseId, version);
            }
        }

        return null;
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

    public static class SupportingDataDescriptor implements Comparable<SupportingDataDescriptor> {
        public final String setId;
        public final String zipName;
        public final String knowledgeBaseId;
        public final String version;

        private SupportingDataDescriptor(String setId, String zipName, String knowledgeBaseId, String version) {
            this.setId = setId;
            this.zipName = zipName;
            this.knowledgeBaseId = knowledgeBaseId;
            this.version = version;
        }

        @Override
        public int compareTo(SupportingDataDescriptor other) {
            int kbCompare = compareIgnoreCase(knowledgeBaseId, other.knowledgeBaseId);
            if (kbCompare != 0) {
                return kbCompare;
            }

            int versionCompare = VersionComparator.compareVersions(version, other.version);
            if (versionCompare != 0) {
                return -versionCompare;
            }

            return compareIgnoreCase(setId, other.setId);
        }

        private int compareIgnoreCase(String a, String b) {
            String sa = a == null ? "" : a;
            String sb = b == null ? "" : b;
            return sa.compareToIgnoreCase(sb);
        }
    }
}
