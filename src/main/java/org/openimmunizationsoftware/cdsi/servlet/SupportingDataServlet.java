package org.openimmunizationsoftware.cdsi.servlet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SupportingDataServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String PARAM_ACTION = "action";
    private static final String PARAM_SET = "set";
    private static final String PARAM_ENTRY = "entry";

    private static final String ACTION_DOWNLOAD = "download";

    private static final String CLASS_PATH_SUPPORTING_DATA = "/WEB-INF/classes/supporting-data/";
    private static final String CLASS_PATH_ROOT = "/WEB-INF/classes/";
    private static final String WEBAPP_PATH_SUPPORTING_DATA = "/supporting-data/";

    private static final Pattern ANTIGEN_XML_PATTERN = Pattern
            .compile("^AntigenSupportingData-\\s*(.+?)(?:-\\s*508)?\\.xml$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ANTIGEN_XLSX_PATTERN = Pattern
            .compile("^AntigenSupportingData-\\s*(.+?)(?:-\\s*508)?\\.xlsx$", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEGACY_CDC_ZIP_PATTERN = Pattern
            .compile("^supporting-data-(\\d+(?:\\.\\d+)*)(?:-508)?\\.zip$", Pattern.CASE_INSENSITIVE);
    private static final Pattern KNOWLEDGE_BASE_ZIP_PATTERN = Pattern
            .compile("^(.+)-(\\d+(?:\\.\\d+)*)\\.zip$", Pattern.CASE_INSENSITIVE);
    private static final Pattern VERSION_FOLDER_PATTERN = Pattern.compile("^Version\\b.*", Pattern.CASE_INSENSITIVE);

    private static final String DEFAULT_KNOWLEDGE_BASE = "USA-CDC-CDSI";
    private static final String DEFAULT_HUMAN_READABLE_NAME = "HHS/ACIP";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        List<SupportingDataSet> dataSetList = discoverSupportingDataSets(getServletContext());

        String action = req.getParameter(PARAM_ACTION);
        if (ACTION_DOWNLOAD.equals(action)) {
            handleDownload(req, resp, dataSetList);
            return;
        }

        renderPage(req, resp, dataSetList);
    }

    private void handleDownload(HttpServletRequest req, HttpServletResponse resp,
            List<SupportingDataSet> dataSetList) throws IOException {

        String setId = req.getParameter(PARAM_SET);
        String entryPath = req.getParameter(PARAM_ENTRY);

        if (setId == null || entryPath == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing set or entry parameter");
            return;
        }

        SupportingDataSet set = findById(dataSetList, setId);
        if (set == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Supporting data set not found");
            return;
        }

        if (!set.downloadableEntryPathSet.contains(entryPath)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Requested file is not available for download");
            return;
        }

        try (InputStream zipStream = set.zipSource.openStream(getServletContext());
                ZipInputStream zis = new ZipInputStream(new BufferedInputStream(zipStream))) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }
                if (!entry.getName().equals(entryPath)) {
                    zis.closeEntry();
                    continue;
                }

                String fileName = simpleName(entry.getName());
                String contentType = resolveContentType(fileName);

                resp.setContentType(contentType);
                resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = zis.read(buffer)) > 0) {
                    resp.getOutputStream().write(buffer, 0, bytesRead);
                }
                return;
            }
        } catch (Exception e) {
            throw new IOException("Unable to stream file from supporting data set", e);
        }

        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Requested file not found in zip");
    }

    private void renderPage(HttpServletRequest req, HttpServletResponse resp,
            List<SupportingDataSet> dataSetList) throws IOException {

        resp.setContentType("text/html");
        PrintWriter out = new PrintWriter(resp.getOutputStream());

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("  <meta charset=\"UTF-8\">");
        out.println("  <title>CDSi Supporting Data</title>");
        out.println("  <style>");
        out.println("    body { font-family: Arial, sans-serif; margin: 20px; }");
        out.println("    h1, h2, h3 { color: #333; }");
        out.println(
                "    img { float: right; margin: 20px 0 20px 20px; border: 1px solid #ddd; padding: 8px; background-color: #f9f9f9; }");
        out.println("    table { border-collapse: collapse; margin: 20px 0; width: 100%; }");
        out.println("    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; vertical-align: top; }");
        out.println("    th { background-color: #f2f2f2; font-weight: bold; }");
        out.println(
                "    .summary { margin: 20px 0; padding: 15px; background-color: #f5f5f5; border: 1px solid #ddd; }");
        out.println(
                "    .set-block { margin-top: 30px; border: 1px solid #ddd; padding: 12px; background-color: #fcfcfc; }");
        out.println("    .muted { color: #666; font-size: 12px; }");
        out.println("  </style>");
        out.println("</head>");
        out.println("<body>");

        out.println("  <h1>CDSi Supporting Data</h1>");
        out.println("  <img src=\"pm.png\" alt=\"Process Model Diagram\" width=\"900\" />");

        out.println("  <div class=\"summary\">");
        out.println(
                "    <p>This is a read-only view of available supporting data sets (CDC format) bundled with the app or available in the supporting-data folder.</p>");
        out.println("    <p><strong>Set count:</strong> " + dataSetList.size() + "</p>");
        out.println("  </div>");

        if (dataSetList.isEmpty()) {
            out.println("  <p>No supporting data zip sets were found.</p>");
            out.println("</body></html>");
            out.close();
            return;
        }

        out.println("  <h2>Versions Available</h2>");
        out.println("  <table>");
        out.println("    <tr>");
        out.println("      <th>Schedule Name</th>");
        out.println("      <th>Knowledge Base</th>");
        out.println("      <th>Version</th>");
        out.println("      <th>Release Notes</th>");
        out.println("      <th>Zip Name</th>");
        out.println("    </tr>");

        for (SupportingDataSet set : dataSetList) {
            out.println("    <tr>");
            out.println("      <td>" + escapeHtml(set.scheduleName) + "</td>");
            out.println("      <td>" + escapeHtml(set.knowledgeBaseId) + "</td>");
            out.println("      <td>" + escapeHtml(set.version) + "</td>");
            out.println("      <td>" + buildDownloadLink("View", set.id, set.releaseNotesEntryPath) + "</td>");
            out.println("      <td>" + escapeHtml(set.zipFileName) + "</td>");
            out.println("    </tr>");
        }
        out.println("  </table>");

        for (SupportingDataSet set : dataSetList) {
            out.println("  <div class=\"set-block\">");
            out.println("    <h3>" + escapeHtml(set.scheduleName) + "</h3>");
            out.println(
                    "    <p><span class=\"muted\">Knowledge Base:</span> " + escapeHtml(set.knowledgeBaseId) + "</p>");
            out.println("    <p><span class=\"muted\">Version:</span> " + escapeHtml(set.version) + "</p>");
            out.println("    <p><span class=\"muted\">Zip:</span> " + escapeHtml(set.zipFileName) + "</p>");
            out.println("    <p><span class=\"muted\">Release Notes:</span> "
                    + buildDownloadLink(set.releaseNotesEntryPath == null ? "Not found" : "Download", set.id,
                            set.releaseNotesEntryPath)
                    + "</p>");

            out.println("    <table>");
            out.println("      <tr>");
            out.println("        <th>Antigen</th>");
            out.println("        <th>XML</th>");
            out.println("        <th>Excel</th>");
            out.println("      </tr>");

            List<String> antigenNameList = new ArrayList<String>(set.antigenMap.keySet());
            Collections.sort(antigenNameList, String.CASE_INSENSITIVE_ORDER);
            for (String antigenName : antigenNameList) {
                AntigenArtifacts artifacts = set.antigenMap.get(antigenName);
                out.println("      <tr>");
                out.println("        <td>" + escapeHtml(antigenName) + "</td>");
                out.println(
                        "        <td>" + buildDownloadLink("Download XML", set.id, artifacts.xmlEntryPath) + "</td>");
                out.println("        <td>" + buildDownloadLink("Download Excel", set.id, artifacts.xlsxEntryPath)
                        + "</td>");
                out.println("      </tr>");
            }

            out.println("    </table>");
            out.println("  </div>");
        }

        out.println("</body>");
        out.println("</html>");
        out.close();
    }

    private List<SupportingDataSet> discoverSupportingDataSets(ServletContext servletContext) {
        List<ZipSource> zipSources = discoverZipSources(servletContext);
        List<SupportingDataSet> setList = new ArrayList<SupportingDataSet>();

        for (ZipSource source : zipSources) {
            try {
                SupportingDataSet set = parseSetFromZip(source, servletContext);
                if (set != null) {
                    setList.add(set);
                }
            } catch (Exception e) {
                // Skip unreadable zips to keep this servlet read-only and resilient.
            }
        }

        Collections.sort(setList, new Comparator<SupportingDataSet>() {
            @Override
            public int compare(SupportingDataSet o1, SupportingDataSet o2) {
                return o1.zipFileName.compareToIgnoreCase(o2.zipFileName);
            }
        });

        return setList;
    }

    private List<ZipSource> discoverZipSources(ServletContext servletContext) {
        List<ZipSource> sourceList = new ArrayList<ZipSource>();

        addServletContextZipSources(servletContext, CLASS_PATH_ROOT, sourceList, false);
        addServletContextZipSources(servletContext, CLASS_PATH_SUPPORTING_DATA, sourceList, true);
        addServletContextZipSources(servletContext, WEBAPP_PATH_SUPPORTING_DATA, sourceList, true);

        File supportingDataDir = new File("supporting-data");
        if (supportingDataDir.exists() && supportingDataDir.isDirectory()) {
            File[] files = supportingDataDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().toLowerCase().endsWith(".zip")) {
                        sourceList.add(ZipSource.fromFile(file));
                    }
                }
            }
        }

        return sourceList;
    }

    @SuppressWarnings("unchecked")
    private void addServletContextZipSources(ServletContext servletContext, String folderPath,
            List<ZipSource> sourceList, boolean recursive) {

        Set<String> pathSet = servletContext.getResourcePaths(folderPath);
        if (pathSet == null) {
            return;
        }

        for (String path : pathSet) {
            if (path.endsWith("/")) {
                if (recursive) {
                    addServletContextZipSources(servletContext, path, sourceList, true);
                }
                continue;
            }

            if (path.toLowerCase().endsWith(".zip")) {
                sourceList.add(ZipSource.fromServletResource(path));
            }
        }
    }

    private SupportingDataSet parseSetFromZip(ZipSource source, ServletContext servletContext) throws Exception {
        SupportingDataSet set = new SupportingDataSet();
        set.zipFileName = source.fileName();
        set.id = normalizeSetId(set.zipFileName);
        parseKnowledgeBaseAndVersion(set);
        set.zipSource = source;
        Set<String> rootFolderNameSet = new HashSet<String>();

        try (InputStream is = source.openStream(servletContext);
                ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is))) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryPath = normalizeZipPath(entry.getName());
                String rootFolderName = findRootFolderName(entryPath);
                if (rootFolderName == null) {
                    throw new IOException("Supporting data zip must contain a single root folder: " + set.zipFileName);
                }
                rootFolderNameSet.add(rootFolderName);
                if (rootFolderNameSet.size() > 1) {
                    throw new IOException("Supporting data zip contains more than one root folder: " + set.zipFileName);
                }

                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }

                String fileName = simpleName(entryPath);

                if (isReleaseNotesFile(fileName) && set.releaseNotesEntryPath == null) {
                    set.releaseNotesEntryPath = entryPath;
                    set.downloadableEntryPathSet.add(entryPath);
                }

                Matcher xmlMatcher = ANTIGEN_XML_PATTERN.matcher(fileName);
                if (xmlMatcher.matches()) {
                    String antigenName = xmlMatcher.group(1).trim();
                    if (!antigenName.equals("")) {
                        AntigenArtifacts artifacts = set.getOrCreateAntigenArtifacts(antigenName);
                        artifacts.xmlEntryPath = entryPath;
                        set.downloadableEntryPathSet.add(entryPath);
                    }
                }

                Matcher xlsxMatcher = ANTIGEN_XLSX_PATTERN.matcher(fileName);
                if (xlsxMatcher.matches()) {
                    String antigenName = xlsxMatcher.group(1).trim();
                    if (!antigenName.equals("")) {
                        AntigenArtifacts artifacts = set.getOrCreateAntigenArtifacts(antigenName);
                        artifacts.xlsxEntryPath = entryPath;
                        set.downloadableEntryPathSet.add(entryPath);
                    }
                }

                zis.closeEntry();
            }
        }

        if (rootFolderNameSet.size() != 1) {
            throw new IOException("Supporting data zip must contain exactly one root folder: " + set.zipFileName);
        }

        String rootFolderName = rootFolderNameSet.iterator().next();
        String humanReadableName = VERSION_FOLDER_PATTERN.matcher(rootFolderName).matches()
                ? DEFAULT_HUMAN_READABLE_NAME
                : rootFolderName;
        set.scheduleName = humanReadableName + " " + set.version;

        if (set.antigenMap.isEmpty() && set.releaseNotesEntryPath == null) {
            return null;
        }

        return set;
    }

    private boolean isReleaseNotesFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.contains("release notes") && (lower.endsWith(".doc") || lower.endsWith(".docx")
                || lower.endsWith(".pdf") || lower.endsWith(".txt"));
    }

    private SupportingDataSet findById(List<SupportingDataSet> list, String id) {
        for (SupportingDataSet set : list) {
            if (set.id.equals(id)) {
                return set;
            }
        }
        return null;
    }

    private String buildDownloadLink(String label, String setId, String entryPath) {
        if (entryPath == null) {
            return "<span class=\"muted\">N/A</span>";
        }
        String href = "supportingData?action=download&set=" + urlEncode(setId)
                + "&entry=" + urlEncode(entryPath);
        return "<a href=\"" + href + "\">" + escapeHtml(label) + "</a>";
    }

    private String normalizeSetId(String zipFileName) {
        if (zipFileName.toLowerCase().endsWith(".zip")) {
            return zipFileName.substring(0, zipFileName.length() - 4);
        }
        return zipFileName;
    }

    private void parseKnowledgeBaseAndVersion(SupportingDataSet set) throws IOException {
        Matcher legacyMatcher = LEGACY_CDC_ZIP_PATTERN.matcher(set.zipFileName);
        if (legacyMatcher.matches()) {
            set.knowledgeBaseId = DEFAULT_KNOWLEDGE_BASE;
            set.version = legacyMatcher.group(1).trim();
            return;
        }

        Matcher kbMatcher = KNOWLEDGE_BASE_ZIP_PATTERN.matcher(set.zipFileName);
        if (!kbMatcher.matches()) {
            throw new IOException("Zip name is not in expected format [knowledge base id]-[version].zip: "
                    + set.zipFileName);
        }

        String knowledgeBaseId = kbMatcher.group(1).trim();
        String version = kbMatcher.group(2).trim();

        if (knowledgeBaseId.equals("") || version.equals("")) {
            throw new IOException("Unable to parse knowledge base/version from zip name: " + set.zipFileName);
        }

        set.knowledgeBaseId = knowledgeBaseId;
        set.version = version;
    }

    private String normalizeZipPath(String path) {
        if (path == null) {
            return "";
        }

        String normalized = path.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private String findRootFolderName(String normalizedPath) {
        if (normalizedPath == null || normalizedPath.equals("")) {
            return null;
        }

        int slashPos = normalizedPath.indexOf('/');
        if (slashPos <= 0) {
            return null;
        }

        return normalizedPath.substring(0, slashPos).trim();
    }

    private String resolveContentType(String fileName) {
        String byServlet = getServletContext().getMimeType(fileName);
        if (byServlet != null) {
            return byServlet;
        }

        String lower = fileName.toLowerCase();
        if (lower.endsWith(".xml")) {
            return "application/xml";
        }
        if (lower.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        if (lower.endsWith(".doc")) {
            return "application/msword";
        }
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".zip")) {
            return "application/zip";
        }
        return "application/octet-stream";
    }

    private String simpleName(String path) {
        int pos = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return pos < 0 ? path : path.substring(pos + 1);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static class SupportingDataSet {
        private String id;
        private String zipFileName;
        private String knowledgeBaseId;
        private String version;
        private String scheduleName;
        private ZipSource zipSource;
        private String releaseNotesEntryPath;
        private Map<String, AntigenArtifacts> antigenMap = new HashMap<String, AntigenArtifacts>();
        private Set<String> downloadableEntryPathSet = new HashSet<String>();

        private AntigenArtifacts getOrCreateAntigenArtifacts(String antigenName) {
            AntigenArtifacts artifacts = antigenMap.get(antigenName);
            if (artifacts == null) {
                artifacts = new AntigenArtifacts();
                antigenMap.put(antigenName, artifacts);
            }
            return artifacts;
        }
    }

    private static class AntigenArtifacts {
        private String xmlEntryPath;
        private String xlsxEntryPath;
    }

    private static class ZipSource {
        private final String servletResourcePath;
        private final File file;

        private ZipSource(String servletResourcePath, File file) {
            this.servletResourcePath = servletResourcePath;
            this.file = file;
        }

        private static ZipSource fromServletResource(String servletResourcePath) {
            return new ZipSource(servletResourcePath, null);
        }

        private static ZipSource fromFile(File file) {
            return new ZipSource(null, file);
        }

        private InputStream openStream(ServletContext servletContext) throws IOException {
            if (file != null) {
                return new FileInputStream(file);
            }
            InputStream is = servletContext.getResourceAsStream(servletResourcePath);
            if (is == null) {
                throw new IOException("Unable to open servlet resource " + servletResourcePath);
            }
            return is;
        }

        private String fileName() {
            if (file != null) {
                return file.getName();
            }
            int pos = servletResourcePath.lastIndexOf('/');
            return pos < 0 ? servletResourcePath : servletResourcePath.substring(pos + 1);
        }
    }
}
