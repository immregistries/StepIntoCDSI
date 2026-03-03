package org.openimmunizationsoftware.cdsi.servlet;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralized management of example test cases for the Sandbox servlet.
 * This class organizes vaccine examples by category and provides a single
 * source of truth for use in both the Sandbox servlet and Home page.
 * 
 * To add new examples, simply call add() in initializeExamples().
 */
public class SandboxServletExamples {
    private static List<ExampleCategory> examples = null;

    /**
     * Get the list of all example categories and links.
     * Lazily initializes on first call.
     */
    public static List<ExampleCategory> getExamples() {
        if (examples == null) {
            examples = new ArrayList<>();
            initializeExamples();
        }
        return examples;
    }

    /**
     * Initialize all example categories and links.
     * Simply add new entries here using add(categoryName, exampleName, url).
     */
    private static void initializeExamples() {
        // DTaP examples
        add("DTaP", "2024-0016 Patient receives an optional 5th dose",
                "?evalDate=20260216&resultFormat=text&patientDob=20200716&patientSex=F&vaccineDate1=20200916&vaccineCvx1=20&vaccineDate2=20210116&vaccineCvx2=20&vaccineDate3=20210819&vaccineCvx3=20&vaccineDate4=20240724&vaccineCvx4=20&vaccineDate5=20260216&vaccineCvx5=20");
        add("DTaP", "2013-0038 Invalid age for dose #3 in midst of others",
                "?evalDate=20260216&resultFormat=text&patientDob=20250215&patientSex=F&vaccineDate1=20250329&vaccineCvx1=107&vaccineDate2=20250423&vaccineCvx2=107&vaccineDate3=20250518&vaccineCvx3=107&vaccineDate4=20260216&vaccineCvx4=107");
        add("DTaP", "2020-0002 Decennial dose as Tdap",
                "?evalDate=20260216&resultFormat=text&patientDob=20040216&patientSex=F&vaccineDate1=20040416&vaccineCvx1=107&vaccineDate2=20040616&vaccineCvx2=107&vaccineDate3=20040816&vaccineCvx3=107&vaccineDate4=20050516&vaccineCvx4=107&vaccineDate5=20080216&vaccineCvx5=107&vaccineDate6=20160216&vaccineCvx6=115&vaccineDate7=20260216&vaccineCvx7=115");

        add("MMR", "2013-0565 Correctly administered single antigen M, M and R followed by MMR=series complete",
                "?evalDate=20260216&resultFormat=text&patientDob=20230207&patientSex=F&vaccineDate1=20240413&vaccineCvx1=07&vaccineMvx1=&vaccineDate2=20240613&vaccineCvx2=06&vaccineMvx2=&vaccineDate3=20250213&vaccineCvx3=05&vaccineMvx3=&vaccineDate4=20260213&vaccineCvx4=03&vaccineMvx4=");
        add("MMR", "2013-0572 Dose 2 at age 13 mo ",
                "?evalDate=20260216&resultFormat=text&patientDob=20250116&patientSex=F&vaccineDate1=20260116&vaccineCvx1=03&vaccineMvx1=&vaccineDate2=20260216&vaccineCvx2=03&vaccineMvx2=");

        add("HepB", "2013-0241 # 3 Pediarix at 24 weeks-4 days. Valid for dose 3",
                "?evalDate=20260216&resultFormat=text&patientDob=20250905&patientSex=F&vaccineDate1=20251017&vaccineCvx1=08&vaccineMvx1=&vaccineDate2=20251217&vaccineCvx2=08&vaccineMvx2=&vaccineDate3=20260216&vaccineCvx3=110&vaccineMvx3=");
        add("HepB", "2013-0202 Dose 2 to dose 3 interval 8 wks-5 days. 4th dose needed.",
                "?evalDate=20260216&resultFormat=text&patientDob=20250626&patientSex=F&vaccineDate1=20250926&vaccineCvx1=08&vaccineMvx1=&vaccineDate2=20251226&vaccineCvx2=08&vaccineMvx2=&vaccineDate3=20260215&vaccineCvx3=08&vaccineMvx3=");
        add("HepB", "2013-0209 Dose # 1 at age 0 days. ",
                "?evalDate=20260216&resultFormat=text&patientDob=20260216&patientSex=F&vaccineDate1=20260216&vaccineCvx1=45&vaccineMvx1=");
        add("HepB", "2013-0257 dose 1 to 2 Pediarix, interval 28-4 days",
                "?evalDate=20260216&resultFormat=text&patientDob=20251123&patientSex=F&vaccineDate1=20260123&vaccineCvx1=110&vaccineMvx1=&vaccineDate2=20260216&vaccineCvx2=110&vaccineMvx2=");
    }

    /**
     * Add an example link to a category (creates category if it doesn't exist).
     * This is the simple method to use for adding test cases.
     * 
     * @param categoryName The vaccine group name (e.g., "DTaP", "MMR")
     * @param exampleName  The test case ID and description (e.g., "2024-0016
     *                     Patient receives an optional 5th dose")
     * @param url          The sandbox URL starting with ? (e.g.,
     *                     "?evalDate=20260216&patientDob=...")
     */
    private static void add(String categoryName, String exampleName, String url) {
        // Find existing category or create new one
        ExampleCategory category = null;
        for (ExampleCategory cat : examples) {
            if (cat.title.equals(categoryName)) {
                category = cat;
                break;
            }
        }
        if (category == null) {
            category = new ExampleCategory(categoryName);
            examples.add(category);
        }

        category.addLink(exampleName, url);
    }

    /**
     * Example link data structure for sidebar quick links.
     * Each link has a name and a URL starting with ? (to be prepended with
     * /step/sandbox).
     */
    public static class ExampleLink {
        public String name;
        public String url; // Starts with ?

        public ExampleLink(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }

    /**
     * Example category for organizing quick links by vaccine type.
     * Contains a title and a list of example links.
     */
    public static class ExampleCategory {
        public String title;
        public List<ExampleLink> links;

        public ExampleCategory(String title) {
            this.title = title;
            this.links = new ArrayList<>();
        }

        public void addLink(String name, String url) {
            this.links.add(new ExampleLink(name, url));
        }
    }
}
