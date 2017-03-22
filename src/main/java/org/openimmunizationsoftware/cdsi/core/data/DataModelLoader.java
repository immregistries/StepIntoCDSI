package org.openimmunizationsoftware.cdsi.core.data;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.openimmunizationsoftware.cdsi.core.domain.Age;
import org.openimmunizationsoftware.cdsi.core.domain.AllowableVaccine;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.BirthDateImmunity;
import org.openimmunizationsoftware.cdsi.core.domain.ClinicalHistory;
import org.openimmunizationsoftware.cdsi.core.domain.Concept;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalNeed;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkip;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkipCondition;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkipConditionType;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkipSet;
import org.openimmunizationsoftware.cdsi.core.domain.Contraindication;
import org.openimmunizationsoftware.cdsi.core.domain.DoseType;
import org.openimmunizationsoftware.cdsi.core.domain.Exclusion;
import org.openimmunizationsoftware.cdsi.core.domain.Immunity;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;
import org.openimmunizationsoftware.cdsi.core.domain.IntervalPriority;
import org.openimmunizationsoftware.cdsi.core.domain.LiveVirusConflict;
import org.openimmunizationsoftware.cdsi.core.domain.PreferrableVaccine;
import org.openimmunizationsoftware.cdsi.core.domain.RecurringDose;
import org.openimmunizationsoftware.cdsi.core.domain.RequiredGender;
import org.openimmunizationsoftware.cdsi.core.domain.Schedule;
import org.openimmunizationsoftware.cdsi.core.domain.SeasonalRecommendation;
import org.openimmunizationsoftware.cdsi.core.domain.SelectBestPatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.SubstituteDose;
import org.openimmunizationsoftware.cdsi.core.domain.Vaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroup;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DataModelLoader {
  private static final String[] scheduleNames =
      {"Diphtheria", "HepA", "HepB", "Hib", "HPV", "Influenza", "MCV", "Measles", "Mumps", "PCV",
          "Pertussis", "Polio", "Rotavirus", "Rubella", "Tetanus", "Varicella"};

  public static DataModel createDataModel() throws Exception {
    DataModel dataModel = new DataModel();

    String baseLocation = "";

    {
      InputStream is =
          DataModelLoader.class.getResourceAsStream(baseLocation + "ScheduleSupportingData.xml");

      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(is);

      doc.getDocumentElement().normalize();
      readCvxToAntigenMap(dataModel, doc);
      readContraindications(dataModel, doc);
      readVaccineGroups(dataModel, doc);
      readVaccineGroupToAntigenMap(dataModel, doc);
      readLiveVirusConflicts(dataModel, doc);
    }
    for (String scheduleName : scheduleNames) {
      InputStream is = DataModelLoader.class
          .getResourceAsStream(baseLocation + "AntigenSupportingData- " + scheduleName + ".xml");

      if (is != null) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(is);

        doc.getDocumentElement().normalize();

        Schedule schedule = new Schedule();
        schedule.setScheduleName(scheduleName);
        dataModel.getScheduleList().add(schedule);
        readImmunity(schedule, dataModel, doc);
        readAntigenSeries(schedule, dataModel, doc);
      }
    }

    return dataModel;
  }

  private static void readImmunity(Schedule schedule, DataModel dataModel, Document doc) {
    NodeList parentList = doc.getElementsByTagName("immunity");
    for (int i = 0; i < parentList.getLength(); i++) {
      Immunity immunity = new Immunity();
      schedule.setImmunity(immunity);
      Node parentNode = parentList.item(i);
      NodeList childList = parentNode.getChildNodes();
      for (int j = 0; j < childList.getLength(); j++) {
        Node childNode = childList.item(j);
        if (childNode.getNodeType() == Node.ELEMENT_NODE) {
          if (childNode.getNodeName().equals("clinicalHistory")) {
            ClinicalHistory clinicalHistory = new ClinicalHistory();
            immunity.getClinicalHistoryList().add(clinicalHistory);
            NodeList grandchildList = childNode.getChildNodes();
            for (int k = 0; k < grandchildList.getLength(); k++) {
              Node grandchildNode = grandchildList.item(k);
              if (grandchildNode.getNodeType() == Node.ELEMENT_NODE) {
                if (grandchildNode.getNodeName().equals("guideline")) {
                  clinicalHistory.setImmunityGuideline(DomUtils.getInternalValue(grandchildNode));
                } else if (grandchildNode.getNodeName().equals("mapping")) {
                  Concept concept = readConcept(grandchildNode);
                  if (concept != null) {
                    clinicalHistory.getConceptList().add(concept);
                  }
                }
              }
            }
          } else if (childNode.getNodeName().equals("dateOfBirth")) {
            BirthDateImmunity birthDateImmunity = null;
            NodeList grandchildList = childNode.getChildNodes();
            for (int k = 0; k < grandchildList.getLength(); k++) {
              Node grandchildNode = grandchildList.item(k);
              if (grandchildNode.getNodeType() == Node.ELEMENT_NODE) {
                if (birthDateImmunity == null) {
                  birthDateImmunity = new BirthDateImmunity();
                  immunity.getBirthDateImmunityList().add(birthDateImmunity);
                }
                if (grandchildNode.getNodeName().equals("immunityBirthDate")) {
                  birthDateImmunity
                      .setImmunityBirthDate(parseDate(DomUtils.getInternalValue(grandchildNode)));
                } else if (grandchildNode.getNodeName().equals("birthCountry")) {
                  birthDateImmunity.setCountryOfBirth(DomUtils.getInternalValue(grandchildNode));
                } else if (grandchildNode.getNodeName().equals("exclusion")) {
                  Exclusion exclusion = null;
                  NodeList greatgrandchildList = grandchildNode.getChildNodes();
                  for (int m = 0; m < greatgrandchildList.getLength(); m++) {
                    Node greatgrandchildNode = greatgrandchildList.item(m);
                    if (greatgrandchildNode.getNodeType() == Node.ELEMENT_NODE) {
                      if (exclusion == null) {
                        exclusion = new Exclusion();
                        birthDateImmunity.getExclusionList().add(exclusion);
                      }
                      if (greatgrandchildNode.getNodeName().equals("exclusionCondition")) {
                        exclusion
                            .setExclusionCondition(DomUtils.getInternalValue(greatgrandchildNode));
                      } else if (greatgrandchildNode.getNodeName().equals("mapping")) {
                        Concept concept = readConcept(greatgrandchildNode);
                        if (concept != null) {
                          exclusion.getConceptList().add(concept);
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private static Concept readConcept(Node parentNode) {
    Concept concept = null;
    NodeList childList = parentNode.getChildNodes();
    for (int m = 0; m < childList.getLength(); m++) {
      Node childNode = childList.item(m);
      if (childNode.getNodeType() == Node.ELEMENT_NODE) {
        if (concept == null) {
          concept = new Concept();
        }
        if (childNode.getNodeName().equals("conceptCodeSystem")) {
          concept.setConceptCodeSystem(DomUtils.getInternalValue(childNode));
        } else if (childNode.getNodeName().equals("conceptCode")) {
          concept.setConceptCode(DomUtils.getInternalValue(childNode));
        } else if (childNode.getNodeName().equals("conceptText")) {
          concept.setConceptText(DomUtils.getInternalValue(childNode));
        }
      }
    }
    return concept;
  }

  private static void readAntigenSeries(Schedule schedule, DataModel dataModel, Document doc) {
    NodeList parentList = doc.getElementsByTagName("series");
    Map<String, SeriesDose> seriesDoseMap = new HashMap<String, SeriesDose>();
    for (int i = 0; i < parentList.getLength(); i++) {
      Node parentNode = parentList.item(i);
      AntigenSeries antigenSeries = new AntigenSeries();
      schedule.getAntigenSeriesList().add(antigenSeries);
      dataModel.getAntigenSeriesList().add(antigenSeries);
      NodeList childList = parentNode.getChildNodes();
      Antigen targetDisease = null;
      VaccineGroup vaccineGroup = null;
      for (int j = 0; j < childList.getLength(); j++) {
        Node childNode = childList.item(j);
        if (childNode.getNodeType() == Node.ELEMENT_NODE) {
          if (childNode.getNodeName().equals("seriesName")) {
            String seriesName = DomUtils.getInternalValue(childNode);
            antigenSeries.setSeriesName(seriesName);
          } else if (childNode.getNodeName().equals("targetDisease")) {
            targetDisease = dataModel.getOrCreateAntigen(DomUtils.getInternalValue(childNode));
            antigenSeries.setTargetDisease(targetDisease);
          } else if (childNode.getNodeName().equals("vaccineGroup")) {
            String nameValue = DomUtils.getInternalValue(childNode);
            vaccineGroup = dataModel.getOrCreateVaccineGroup(nameValue);
            antigenSeries.setVaccineGroup(vaccineGroup);
          } else if (childNode.getNodeName().equals("selectBest")) {
            SelectBestPatientSeries selectBestPatientSeries = new SelectBestPatientSeries();
            antigenSeries.setSelectBestPatientSeries(selectBestPatientSeries);
            NodeList grandchildList = parentNode.getChildNodes();
            for (int k = 0; k < grandchildList.getLength(); k++) {
              Node grandchildNode = grandchildList.item(k);
              if (grandchildNode.getNodeType() == Node.ELEMENT_NODE) {
                if (grandchildNode.getNodeName().equals("defaultSeries")) {
                  selectBestPatientSeries
                      .setDefaultSeries(DomUtils.getInternalValueYesNo(grandchildNode));
                } else if (grandchildNode.getNodeName().equals("productPath")) {
                  selectBestPatientSeries
                      .setProductPath(DomUtils.getInternalValueYesNo(grandchildNode));
                } else if (grandchildNode.getNodeName().equals("seriesPreference")) {
                  selectBestPatientSeries
                      .setSeriesPreference(DomUtils.getInternalValue(grandchildNode));
                } else if (grandchildNode.getNodeName().equals("maxAgeToStart")) {
                  selectBestPatientSeries
                      .setMaxAgeToStart(new TimePeriod(DomUtils.getInternalValue(grandchildNode)));
                }
              }
            }
          } else if (childNode.getNodeName().equals("seriesDose")) {
            SeriesDose seriesDose = new SeriesDose();
            seriesDose.setAntigenSeries(antigenSeries);
            readSeriesDose(seriesDose, seriesDoseMap, dataModel, childNode);
            antigenSeries.getSeriesDoseList().add(seriesDose);
          }
        }
      }
      if (targetDisease != null && vaccineGroup != null) {
        if (!vaccineGroup.getAntigenList().contains(targetDisease)) {
          vaccineGroup.getAntigenList().add(targetDisease);
        }
      }
    }
  }

  private static void readSeriesDose(SeriesDose seriesDose, Map<String, SeriesDose> seriesDoseMap,
      DataModel dataModel, Node node) {
    NodeList parentList = node.getChildNodes();
    for (int i = 0; i < parentList.getLength(); i++) {
      Node parentNode = parentList.item(i);
      if (parentNode.getNodeType() == Node.ELEMENT_NODE) {
        if (parentNode.getNodeName().equals("doseNumber")) {
          String doseNumber = DomUtils.getInternalValue(parentNode);
          if (doseNumber != null) {
            if (doseNumber.startsWith("Dose")) {
              doseNumber = doseNumber.substring(4).trim();
            }
            seriesDose.setDoseNumber(doseNumber);
            if (doseNumber.length() > 0) {
              seriesDoseMap.put(doseNumber, seriesDose);
            }
          }
        } else if (parentNode.getNodeName().equals("age")) {
          Age age = new Age();
          seriesDose.getAgeList().add(age);
          NodeList childNodeList = parentNode.getChildNodes();
          for (int j = 0; j < childNodeList.getLength(); j++) {
            Node childNode = childNodeList.item(j);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
              if (childNode.getNodeName().equals("absMinAge")) {
                age.setAbsoluteMinimumAge(new TimePeriod(DomUtils.getInternalValue(childNode)));
              } else if (childNode.getNodeName().equals("minAge")) {
                age.setMinimugeAge(new TimePeriod(DomUtils.getInternalValue(childNode)));
              } else if (childNode.getNodeName().equals("earliestRecAge")) {
                age.setEarliestRecommendedAge(new TimePeriod(DomUtils.getInternalValue(childNode)));
              } else if (childNode.getNodeName().equals("latestRecAge")) {
                age.setLatestRecommendedAge(new TimePeriod(DomUtils.getInternalValue(childNode)));
              } else if (childNode.getNodeName().equals("maxAge")) {
                age.setMaximumAge(new TimePeriod(DomUtils.getInternalValue(childNode)));
              }
            }
          }
        } else if (parentNode.getNodeName().equals("interval")) {
          Interval interval = new Interval();

          interval.setSeriesDose(seriesDose);
          boolean populated = false;
          NodeList childNodeList = parentNode.getChildNodes();

          for (int j = 0; j < childNodeList.getLength(); j++) {
            Node childNode = childNodeList.item(j);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
              if (childNode.getNodeName().equals("fromPrevious")) {
                YesNo fromPrevious = DomUtils.getInternalValueYesNo(childNode);
                if (fromPrevious != null) {
                  populated = true;
                }
                interval.setFromImmediatePreviousDoseAdministered(fromPrevious);
              } else if (childNode.getNodeName().equals("fromTargetDose")) {
                String fromTargetDose = DomUtils.getInternalValue(childNode);
                if (!fromTargetDose.equalsIgnoreCase("n/a")
                    && !fromTargetDose.equalsIgnoreCase("")) {
                  interval.setFromTargetDoseNumberInSeries(fromTargetDose);
                  populated = true;
                }
              } else if (childNode.getNodeName().equals("absMinInt")) {
                TimePeriod timePeriod = new TimePeriod(DomUtils.getInternalValue(childNode));
                interval.setAbsoluteMinimumInterval(timePeriod);
              } else if (childNode.getNodeName().equals("minInt")) {
                TimePeriod timePeriod = new TimePeriod(DomUtils.getInternalValue(childNode));
                interval.setMinimumInterval(timePeriod);
              } else if (childNode.getNodeName().equals("earliestRecInt")) {
                TimePeriod timePeriod = new TimePeriod(DomUtils.getInternalValue(childNode));
                interval.setEarliestRecommendedInterval(timePeriod);
              } else if (childNode.getNodeName().equals("latestRecInt")) {
                TimePeriod timePeriod = new TimePeriod(DomUtils.getInternalValue(childNode));
                interval.setLatestRecommendedInterval(timePeriod);
              } else if (childNode.getNodeName().equals("intervalPriority")) {
                String intervalPriority = DomUtils.getInternalValue(childNode);
                if (intervalPriority != null && intervalPriority.equalsIgnoreCase("override")) {
                  interval.setIntervalPriority(IntervalPriority.OVERRIDE);
                }
              }
            }
          }
          if (populated) {
            seriesDose.getIntervalList().add(interval);
          }
        } else if (parentNode.getNodeName().equals("preferableVaccine")) {
          PreferrableVaccine preferableVaccine = new PreferrableVaccine();
          preferableVaccine.setSeriesDose(seriesDose);
          Vaccine vaccine = preferableVaccine;
          readVaccine(dataModel, parentNode, vaccine);
          NodeList childNodeList = parentNode.getChildNodes();
          for (int j = 0; j < childNodeList.getLength(); j++) {
            Node childNode = childNodeList.item(j);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
              if (childNode.getNodeName().equals("forecastVaccineType")) {
                preferableVaccine.setForecastVaccineType(DomUtils.getInternalValueYesNo(childNode));
              }
            }
          }
          seriesDose.getPreferrableVaccineList().add(preferableVaccine);
        } else if (parentNode.getNodeName().equals("allowableVaccine")) {
          AllowableVaccine allowableVaccine = new AllowableVaccine();
          allowableVaccine.setSeriesDose(seriesDose);
          Vaccine vaccine = allowableVaccine;
          readVaccine(dataModel, parentNode, vaccine);
          seriesDose.getAllowableVaccineList().add(allowableVaccine);
        } else if (parentNode.getNodeName().equals("conditionalSkip")) {
          ConditionalSkip conditionalSkip = new ConditionalSkip();
          boolean populated = false;
          NodeList childNodeList = parentNode.getChildNodes();
          for (int j = 0; j < childNodeList.getLength(); j++) {
            Node childNode = childNodeList.item(j);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
              if (childNode.getNodeName().equals("setLogic")) {
                String setLogic = DomUtils.getInternalValue(childNode);
                conditionalSkip.setSetLogic(setLogic);
              } else if (childNode.getNodeName().equals("set")) {
                ConditionalSkipSet conditionalSkipSet = new ConditionalSkipSet();
                conditionalSkip.getConditionalSkipSetList().add(conditionalSkipSet);
                NodeList grandchildList = childNode.getChildNodes();
                for (int k = 0; k < grandchildList.getLength(); k++) {
                  Node grandchildNode = grandchildList.item(k);
                  if (grandchildNode.getNodeType() == Node.ELEMENT_NODE) {
                    if (grandchildNode.getNodeName().equals("setID")) {
                      populated = true;
                      conditionalSkipSet
                          .setSetId(Integer.parseInt(DomUtils.getInternalValue(grandchildNode)));
                    } else if (grandchildNode.getNodeName().equals("setDescription")) {
                      conditionalSkipSet
                          .setSetDescription(DomUtils.getInternalValue(grandchildNode));
                    } else if (grandchildNode.getNodeName().equals("conditionLogic")) {
                      conditionalSkipSet
                          .setConditionLogic(DomUtils.getInternalValue(grandchildNode));
                    } else if (grandchildNode.getNodeName().equals("condition")) {
                      readCondition(dataModel, conditionalSkipSet, grandchildNode, seriesDose);
                    }
                  }
                }
              }
            }
          }
          if (populated) {
            seriesDose.setConditionalSkip(conditionalSkip);
          }
        } else if (parentNode.getNodeName().equals("recurringDose")) {
          RecurringDose recurringDose = new RecurringDose();
          seriesDose.getRecurringDoseList().add(recurringDose);
          recurringDose.setValue(DomUtils.getInternalValueYesNo(parentNode));
          recurringDose.setSeriesDose(seriesDose);
        } else if (parentNode.getNodeName().equals("conditionalNeed")) {
          ConditionalNeed conditionalNeed = new ConditionalNeed();
          conditionalNeed.setSeriesDose(seriesDose);
          seriesDose.getConditionalNeedList().add(conditionalNeed);
          // TODO
        } else if (parentNode.getNodeName().equals("seasonalRecommednation")) {
          SeasonalRecommendation seasonalRecommendation = new SeasonalRecommendation();
          seasonalRecommendation.setSeriesDose(seriesDose);
          seriesDose.getSeasonalRecommendationList().add(seasonalRecommendation);
          // TODO
        } else if (parentNode.getNodeName().equals("substituteDose")) {
          SubstituteDose substituteDose = new SubstituteDose();
          substituteDose.setSeriesDose(seriesDose);
          boolean populated = false;
          NodeList childNodeList = parentNode.getChildNodes();
          for (int j = 0; j < childNodeList.getLength(); j++) {
            Node childNode = childNodeList.item(j);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
              if (childNode.getNodeName().equals("validCount")) {
                substituteDose.setTotalCountOfValidDoses(DomUtils.getInternalValueInt(childNode));
                populated = true;
              } else if (childNode.getNodeName().equals("beginAge")) {
                substituteDose
                    .setFirstDoseBeginAge(new TimePeriod(DomUtils.getInternalValue(childNode)));
                populated = true;
              } else if (childNode.getNodeName().equals("beginEnd")) {
                substituteDose
                    .setFirstDoseEndAge(new TimePeriod(DomUtils.getInternalValue(childNode)));
                populated = true;
              } else if (childNode.getNodeName().equals("dosesToSubstitute")) {
                substituteDose
                    .setNumberOfTargetDosesToSubstitue(DomUtils.getInternalValueInt(childNode));
                populated = true;
              }
            }
          }
          if (populated) {
            seriesDose.getSubstituteDoseList().add(substituteDose);
          }
        } else if (parentNode.getNodeName().equals("requiredGender")) {
          RequiredGender requiredGender = new RequiredGender();
          requiredGender.setSeriesDose(seriesDose);
          seriesDose.getRequiredGenderList().add(requiredGender);
          requiredGender.setValue(DomUtils.getInternalValue(parentNode));
        }
      }
    }
  }

  private static void readCondition(DataModel dataModel, ConditionalSkipSet conditionalSkipSet,
      Node grandchildNode, SeriesDose seriesDose) {
    ConditionalSkipCondition condition = new ConditionalSkipCondition(seriesDose);
    conditionalSkipSet.getConditionList().add(condition);
    NodeList greatgrandchildNodeList = grandchildNode.getChildNodes();
    for (int m = 0; m < greatgrandchildNodeList.getLength(); m++) {
      Node greatgrandchildNode = greatgrandchildNodeList.item(m);
      if (greatgrandchildNode.getNodeType() == Node.ELEMENT_NODE) {
        if (greatgrandchildNode.getNodeName().equals("conditionID")) {
          condition
              .setConditionId(Integer.parseInt(DomUtils.getInternalValue(greatgrandchildNode)));
        } else if (greatgrandchildNode.getNodeName().equals("conditionType")) {
          ConditionalSkipConditionType conditionType = null;
          String conditionTypeString = DomUtils.getInternalValue(greatgrandchildNode);
          if (!conditionTypeString.equals("")) {
            conditionType = ConditionalSkipConditionType
                .valueOf(conditionTypeString.toUpperCase().replaceAll("\\s", "_"));
            condition.setConditionType(conditionType);
          }
        } else if (greatgrandchildNode.getNodeName().equals("startDate")) {
          condition.setStartDate(parseDate(DomUtils.getInternalValue(greatgrandchildNode)));
        } else if (greatgrandchildNode.getNodeName().equals("endDate")) {
          condition.setEndDate(parseDate(DomUtils.getInternalValue(greatgrandchildNode)));
        } else if (greatgrandchildNode.getNodeName().equals("beginAge")) {
          condition.setBeginAge(new TimePeriod(DomUtils.getInternalValue(greatgrandchildNode)));
        } else if (greatgrandchildNode.getNodeName().equals("endAge")) {
          condition.setEndAge(new TimePeriod(DomUtils.getInternalValue(greatgrandchildNode)));
        } else if (greatgrandchildNode.getNodeName().equals("interval")) {
          condition.setInterval(new TimePeriod(DomUtils.getInternalValue(greatgrandchildNode)));
        } else if (greatgrandchildNode.getNodeName().equals("doseCount")) {
          String doseCountString = DomUtils.getInternalValue(greatgrandchildNode);
          if (doseCountString.length() > 0) {
            condition.setDoseCount(Integer.parseInt(doseCountString));
          }
        } else if (greatgrandchildNode.getNodeName().equals("doseType")) {
          DoseType doseType = null;
          String doseTypeString = DomUtils.getInternalValue(greatgrandchildNode);
          if (!doseTypeString.equals("")) {
            doseType = DoseType.valueOf(doseTypeString.toUpperCase().replaceAll("\\s", "_"));
            condition.setDoseType(doseType);
          }
        } else if (greatgrandchildNode.getNodeName().equals("doseCountLogic")) {
          condition.setDoseCountLogic(DomUtils.getInternalValue(greatgrandchildNode));
        } else if (greatgrandchildNode.getNodeName().equals("vaccineTypes")) {
          String vaccineTypeCvxCodeString = DomUtils.getInternalValue(greatgrandchildNode);
          if (vaccineTypeCvxCodeString.length() > 0) {
            String[] vaccineTypeCvxCodes = vaccineTypeCvxCodeString.split("\\;");
            for (String vaccineTypeCvx : vaccineTypeCvxCodes) {
              VaccineType vaccineType = dataModel.getCvx(vaccineTypeCvx);
              condition.getVaccineTypeSet().add(vaccineType);
            }
          }
        }
      }
    }
  }

  private static Date parseDate(String dateString) {
    Date date = null;
    if (!dateString.equals("")) {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
      try {
        date = sdf.parse(dateString);
      } catch (ParseException pe) {
        sdf = new SimpleDateFormat("MM/dd/yyyy");
        try {
          date = sdf.parse(dateString);
        } catch (ParseException pe2) {
          System.err.println("This date is not parsable: " + dateString);
          pe2.printStackTrace();
        }
      }
    }
    return date;
  }

  private static void readVaccine(DataModel dataModel, Node parentNode, Vaccine vaccine) {
    NodeList childNodeList = parentNode.getChildNodes();
    for (int j = 0; j < childNodeList.getLength(); j++) {
      Node childNode = childNodeList.item(j);
      if (childNode.getNodeType() == Node.ELEMENT_NODE) {
        if (childNode.getNodeName().equals("vaccineType")) {
          // do nothing
        } else if (childNode.getNodeName().equals("cvx")) {
          VaccineType vaccineType = dataModel.getCvx(DomUtils.getInternalValue(childNode));
          vaccine.setVaccineType(vaccineType);
        } else if (childNode.getNodeName().equals("beginAge")) {
          vaccine.setVaccineTypeBeginAge(new TimePeriod(DomUtils.getInternalValue(childNode)));
        } else if (childNode.getNodeName().equals("endAge")) {
          vaccine.setVaccineTypeEndAge(new TimePeriod(DomUtils.getInternalValue(childNode)));
        } else if (childNode.getNodeName().equals("tradeName")) {
          vaccine.setTradeName(DomUtils.getInternalValue(childNode));
        } else if (childNode.getNodeName().equals("mvx")) {
          vaccine.setManufacturer(DomUtils.getInternalValue(childNode));
        } else if (childNode.getNodeName().equals("volume")) {
          vaccine.setVolume(DomUtils.getInternalValue(childNode));
        }
      }
    }
  }

  private static void readContraindications(DataModel dataModel, Document doc) {
    NodeList parentList = doc.getElementsByTagName("contraindications");
    for (int i = 0; i < parentList.getLength(); i++) {
      Node parentNode = parentList.item(i);
      NodeList childList = parentNode.getChildNodes();
      for (int j = 0; j < childList.getLength(); j++) {
        Node childNode = childList.item(j);
        if (childNode.getNodeType() == Node.ELEMENT_NODE
            && childNode.getNodeName().equals("contraindication")) {
          NodeList grandchildList = childNode.getChildNodes();
          Contraindication contraindication = new Contraindication();
          dataModel.getContraindicationList().add(contraindication);
          for (int k = 0; k < grandchildList.getLength(); k++) {
            Node grandchildNode = grandchildList.item(k);
            if (grandchildNode.getNodeType() == Node.ELEMENT_NODE) {
              if (grandchildNode.getNodeName().equals("antigen")) {
                String antigenName = DomUtils.getInternalValue(grandchildNode);
                Antigen antigen = dataModel.getAntigen(antigenName);
                contraindication.setAntigen(antigen);
              } else if (grandchildNode.getNodeName().equals("contraindicationLanguage")) {
                String contraindicationLanguage = DomUtils.getInternalValue(grandchildNode);
                contraindication.setContraindicationLanguage(contraindicationLanguage);
              } if (grandchildNode.getNodeName().equals("concept")) {
                String concept = DomUtils.getInternalValue(grandchildNode);
                contraindication.getConcept().setConceptCodeSystem(concept);
              } else if (grandchildNode.getNodeName().equals("conceptCode")) {
                String conceptCode = DomUtils.getInternalValue(grandchildNode);
                contraindication.getConcept().setConceptCode(conceptCode);
              } else if (grandchildNode.getNodeName().equals("conceptText")) {
                String conceptText = DomUtils.getInternalValue(grandchildNode);
                contraindication.getConcept().setConceptText(conceptText);
              } else if (grandchildNode.getNodeName().equals("cvxList")) {
                String cvxList = DomUtils.getInternalValue(grandchildNode);
                String cvxStrings[] = cvxList.split("\\,");
                for (String cvxString : cvxStrings) {
                  if (cvxString != null) {
                    cvxString = cvxString.trim();
                    if (cvxString.length() > 0) {
                      VaccineType vaccineType = dataModel.getCvx(cvxString);
                      contraindication.getCvxList().add(vaccineType);
                    }
                  }
                }

              }
            }
          }
        }
      }
    }

  }

  private static void readVaccineGroups(DataModel dataModel, Document doc) {
    NodeList parentList = doc.getElementsByTagName("vaccineGroups");
    for (int i = 0; i < parentList.getLength(); i++) {
      Node parentNode = parentList.item(i);
      NodeList childList = parentNode.getChildNodes();
      for (int j = 0; j < childList.getLength(); j++) {
        Node childNode = childList.item(j);
        if (childNode.getNodeType() == Node.ELEMENT_NODE
            && childNode.getNodeName().equals("vaccineGroup")) {
          NodeList grandchildList = childNode.getChildNodes();
          VaccineGroup vaccineGroup = null;
          for (int k = 0; k < grandchildList.getLength(); k++) {
            Node grandchildNode = grandchildList.item(k);
            if (grandchildNode.getNodeType() == Node.ELEMENT_NODE) {
              if (grandchildNode.getNodeName().equals("name")) {
                String nameValue = DomUtils.getInternalValue(grandchildNode);
                vaccineGroup = dataModel.getOrCreateVaccineGroup(nameValue);
              } else if (vaccineGroup != null
                  && grandchildNode.getNodeName().equals("administerFullVaccineGroup")) {
                String s = DomUtils.getInternalValue(grandchildNode);
                if (s.equalsIgnoreCase("Yes")) {
                  vaccineGroup.setAdministerFullVaccineGroup(YesNo.YES);
                } else if (s.equalsIgnoreCase("No")) {
                  vaccineGroup.setAdministerFullVaccineGroup(YesNo.NO);
                } else if (s.equalsIgnoreCase("n/a")) {
                  vaccineGroup.setAdministerFullVaccineGroup(YesNo.NOT_APPLICABLE);
                }
              }
            }
          }
        }
      }
    }
  }

  private static void readVaccineGroupToAntigenMap(DataModel dataModel, Document doc) {
    NodeList parentList = doc.getElementsByTagName("vaccineGroupToAntigenMap");
    for (int i = 0; i < parentList.getLength(); i++) {
      Node parentNode = parentList.item(i);
      NodeList childList = parentNode.getChildNodes();
      for (int j = 0; j < childList.getLength(); j++) {
        Node childNode = childList.item(j);
        if (childNode.getNodeType() == Node.ELEMENT_NODE
            && childNode.getNodeName().equals("vaccineGroupMap")) {
          NodeList grandchildList = childNode.getChildNodes();
          VaccineGroup vaccineGroup = null;
          for (int k = 0; k < grandchildList.getLength(); k++) {
            Node grandchildNode = grandchildList.item(k);
            if (grandchildNode.getNodeType() == Node.ELEMENT_NODE) {
              if (grandchildNode.getNodeName().equals("name")) {
                String nameValue = DomUtils.getInternalValue(grandchildNode);
                vaccineGroup = dataModel.getOrCreateVaccineGroup(nameValue);
              } else if (vaccineGroup != null && grandchildNode.getNodeName().equals("antigen")) {
                String antigenName = DomUtils.getInternalValue(grandchildNode);
                Antigen antigen = dataModel.getOrCreateAntigen(antigenName);
                antigen.setVaccineGroup(vaccineGroup);
                vaccineGroup.getAntigenList().add(antigen);
              }
            }
          }
        }
      }
    }
  }

  private static void readCvxToAntigenMap(DataModel dataModel, Document doc) {
    NodeList parentList = doc.getElementsByTagName("cvxToAntigenMap");
    for (int i = 0; i < parentList.getLength(); i++) {
      Node parentNode = parentList.item(i);
      NodeList childList = parentNode.getChildNodes();
      for (int j = 0; j < childList.getLength(); j++) {
        Node childNode = childList.item(j);
        if (childNode.getNodeType() == Node.ELEMENT_NODE
            && childNode.getNodeName().equals("cvxMap")) {
          VaccineType cvx = new VaccineType();
          readCvx(dataModel, cvx, childNode);
          dataModel.getCvxMap().put(cvx.getCvxCode(), cvx);
        }
      }
    }
  }

  private static void readCvx(DataModel dataModel, VaccineType cvx, Node parentNode) {
    NodeList childList = parentNode.getChildNodes();
    for (int j = 0; j < childList.getLength(); j++) {
      Node childNode = childList.item(j);
      if (childNode.getNodeType() == Node.ELEMENT_NODE) {
        if (childNode.getNodeName().equals("cvx")) {
          cvx.setCvxCode(DomUtils.getInternalValue(childNode));
        } else if (childNode.getNodeName().equals("shortDescription")) {
          cvx.setShortDescription(DomUtils.getInternalValue(childNode));
        } else if (childNode.getNodeName().equals("association")) {
          NodeList grandchildList = childNode.getChildNodes();
          for (int k = 0; k < grandchildList.getLength(); k++) {
            Node grandchildNode = grandchildList.item(k);
            if (grandchildNode.getNodeName().equals("antigen")) {
              String antigenName = DomUtils.getInternalValue(grandchildNode);
              Antigen antigen = dataModel.getOrCreateAntigen(antigenName);
              cvx.getAntigenList().add(antigen);
              antigen.getCvxList().add(cvx);
            }
          }
        }
      }
    }
  }

  private static void readLiveVirusConflicts(DataModel dataModel, Document doc) {
    NodeList parentList = doc.getElementsByTagName("liveVirusConflicts");
    for (int i = 0; i < parentList.getLength(); i++) {
      Node parentNode = parentList.item(i);

      NodeList childList = parentNode.getChildNodes();
      for (int j = 0; j < childList.getLength(); j++) {
        Node childNode = childList.item(j);
        if (childNode.getNodeType() == Node.ELEMENT_NODE
            && childNode.getNodeName().equals("liveVirusConflict")) {
          LiveVirusConflict liveVirusConflict = new LiveVirusConflict();
          dataModel.getLiveVirusConflictList().add(liveVirusConflict);
          readLiveVirusConfict(dataModel, liveVirusConflict, childNode);
        }
      }
    }
  }

  private static void readLiveVirusConfict(DataModel dataModel, LiveVirusConflict liveVirusConflict,
      Node parentNode) {
    NodeList childList = parentNode.getChildNodes();
    for (int j = 0; j < childList.getLength(); j++) {
      Node childNode = childList.item(j);
      if (childNode.getNodeType() == Node.ELEMENT_NODE) {
        if (childNode.getNodeName().equals("previous")) {
          VaccineType cvx = readVaccine(dataModel, childNode);
          liveVirusConflict.setPreviousVaccineType(cvx);
        } else if (childNode.getNodeName().equals("current")) {
          VaccineType cvx = readVaccine(dataModel, childNode);
          liveVirusConflict.setCurrentVaccineType(cvx);
        } else if (childNode.getNodeName().equals("conflictBeginInterval")) {
          liveVirusConflict
              .setConflictBeginInterval(new TimePeriod(DomUtils.getInternalValue(childNode)));
        } else if (childNode.getNodeName().equals("minConflictEndInterval")) {
          liveVirusConflict
              .setMinimalConflictEndInterval(new TimePeriod(DomUtils.getInternalValue(childNode)));
        } else if (childNode.getNodeName().equals("conflictEndInterval")) {
          liveVirusConflict
              .setConflictEndInterval(new TimePeriod(DomUtils.getInternalValue(childNode)));
        }
      }
    }

  }

  private static VaccineType readVaccine(DataModel dataModel, Node childNode) {
    NodeList grandchildList = childNode.getChildNodes();
    String vaccineType = "";
    String cvxCode = "";
    for (int k = 0; k < grandchildList.getLength(); k++) {
      Node grandchildNode = grandchildList.item(k);
      if (grandchildNode.getNodeType() == Node.ELEMENT_NODE) {
        if (grandchildNode.getNodeName().equals("cvx")) {
          cvxCode = DomUtils.getInternalValue(grandchildNode);
        } else if (grandchildNode.getNodeName().equals("vaccineType")) {
          vaccineType = DomUtils.getInternalValue(grandchildNode);
        }
      }
    }
    VaccineType cvx = dataModel.getCvx(cvxCode);
    if (cvx == null) {
      cvx = new VaccineType();
      cvx.setCvxCode(cvxCode);
      cvx.setShortDescription(vaccineType);
      dataModel.getCvxMap().put(cvxCode, cvx);
    }
    return cvx;
  }
}
