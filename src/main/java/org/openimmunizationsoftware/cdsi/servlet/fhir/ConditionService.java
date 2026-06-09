package org.openimmunizationsoftware.cdsi.servlet.fhir;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;

public class ConditionService {

    private static ConditionService instance;
    private final Map<String, ConditionInfo> conditions = new LinkedHashMap<>();

    public static class ConditionInfo {
        public final String code;
        public final String title;
        public final String definition;

        public ConditionInfo(String code, String title, String definition) {
            this.code = code;
            this.title = title;
            this.definition = definition;
        }
    }

    public static synchronized ConditionService getInstance() {
        if (instance == null) {
            instance = new ConditionService();
            instance.loadConditions();
        }
        return instance;
    }

    private void loadConditions() {
        ClassLoader cl = getClass().getClassLoader();

        String[] antigenFiles = {
            "AntigenSupportingData- COVID-19-508.xml",
            "AntigenSupportingData- Diphtheria-508.xml",
            "AntigenSupportingData- HepA-508.xml",
            "AntigenSupportingData- HepB-508.xml",
            "AntigenSupportingData- HPV-508.xml",
            "AntigenSupportingData- Influenza-508.xml",
            "AntigenSupportingData- JE-508.xml",
            "AntigenSupportingData- Measles-508.xml",
            "AntigenSupportingData- Meningococcal-508.xml",
            "AntigenSupportingData- Mumps-508.xml",
            "AntigenSupportingData- Polio-508.xml",
            "AntigenSupportingData- Rabies-508.xml",
            "AntigenSupportingData- Rotavirus-508.xml",
            "AntigenSupportingData- Rubella-508.xml",
            "AntigenSupportingData- Tetanus-508.xml",
            "AntigenSupportingData- Typhoid-508.xml",
            "AntigenSupportingData- Varicella-508.xml",
            "AntigenSupportingData- Zoster-508.xml"
        };

        for (String file : antigenFiles) {
            InputStream is = cl.getResourceAsStream(file);
            if (is != null) {
                loadAntigenFile(is);
            }
        }

        InputStream scheduleIs = cl.getResourceAsStream("ScheduleSupportingData.xml");
        if (scheduleIs != null) {
            loadScheduleFile(scheduleIs);
        }

        System.err.println("ConditionService loaded " + conditions.size() + " conditions");
    }

    private void loadAntigenFile(InputStream is) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            is.close();

            NodeList contraindicationNodes = doc.getElementsByTagName("contraindication");
            for (int i = 0; i < contraindicationNodes.getLength(); i++) {
                Element contr = (Element) contraindicationNodes.item(i);
                String code = getElementText(contr, "observationCode");
                String title = getElementText(contr, "observationTitle");
                String contraText = getElementText(contr, "contraindicationText");

                if (code != null && !code.isEmpty() && !conditions.containsKey(code)) {
                    conditions.put(code, new ConditionInfo(code, title != null ? title : "", contraText != null ? contraText : ""));
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading antigen file: " + e.getMessage());
        }
    }

    private void loadScheduleFile(InputStream is) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            is.close();

            NodeList observationNodes = doc.getElementsByTagName("observation");
            for (int i = 0; i < observationNodes.getLength(); i++) {
                Element obs = (Element) observationNodes.item(i);
                String code = getElementText(obs, "observationCode");
                String title = getElementText(obs, "observationTitle");

                if (code != null && !code.isEmpty() && !conditions.containsKey(code)) {
                    conditions.put(code, new ConditionInfo(code, title != null ? title : "", ""));
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading schedule file: " + e.getMessage());
        }
    }

    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }

    public List<ConditionInfo> getAllConditions() {
        return new ArrayList<>(conditions.values());
    }

    public ConditionInfo getCondition(String code) {
        return conditions.get(code);
    }

    public int getCount() {
        return conditions.size();
    }
}
