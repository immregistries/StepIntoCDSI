package org.openimmunizationsoftware.cdsi.core.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.LiveVirusConflict;
import org.openimmunizationsoftware.cdsi.core.domain.Observation;
import org.openimmunizationsoftware.cdsi.core.domain.Schedule;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroup;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;

/**
 * Static Supporting Data, loaded once per selected Supporting Data set by
 * {@link DataModelLoader} and shared across every {@link DataModel} built
 * from it in a JVM's lifetime (Phase 22 of the reference-module plan).
 *
 * Not deep-immutable: dozens of existing spec-conformance unit tests build
 * fixtures by mutating these collections directly through a {@link DataModel}
 * reference (e.g. {@code dataModel.getCvxMap().put(...)}), so wrapping them
 * as unmodifiable here would break every one of those tests. In production
 * code, nothing outside {@link DataModelLoader} mutates any field on this
 * class - see the Phase 22 plan for the audit that confirmed this. Stronger
 * immutability enforcement is deferred to a later hardening pass.
 */
public class SupportingDataModel {

  private Map<String, VaccineType> cvxMap = new HashMap<String, VaccineType>();
  private Map<String, Antigen> antigenMap = new HashMap<String, Antigen>();
  private List<Antigen> antigenList = null;
  private Map<String, VaccineGroup> vaccineGroupMap = new HashMap<String, VaccineGroup>();
  private List<VaccineGroup> vaccineGroupList;
  private List<Schedule> scheduleList = new ArrayList<Schedule>();
  private List<AntigenSeries> antigenSeriesList = new ArrayList<AntigenSeries>();
  private List<LiveVirusConflict> liveVirusConflictList = new ArrayList<LiveVirusConflict>();
  private Map<String, Observation> observationMap = new HashMap<String, Observation>();

  public Map<String, VaccineType> getCvxMap() {
    return cvxMap;
  }

  public void setCvxMap(Map<String, VaccineType> cvxMap) {
    this.cvxMap = cvxMap;
  }

  public VaccineType getCvx(String cvxCode) {
    return cvxMap.get(cvxCode);
  }

  public Map<String, Antigen> getAntigenMap() {
    return antigenMap;
  }

  public Antigen getAntigen(String antigenName) {
    return antigenMap.get(antigenName);
  }

  public Antigen getOrCreateAntigen(String antigenName) {
    Antigen antigen = getAntigen(antigenName);
    if (antigen == null) {
      antigen = new Antigen();
      antigen.setName(antigenName);
      getAntigenMap().put(antigenName, antigen);
    }
    return antigen;
  }

  public List<Antigen> getAntigenList() {
    if (antigenList == null) {
      antigenList = new ArrayList<Antigen>(antigenMap.values());
    }
    return antigenList;
  }

  public Map<String, VaccineGroup> getVaccineGroupMap() {
    return vaccineGroupMap;
  }

  public VaccineGroup getVaccineGroup(String vaccineGroupName) {
    return vaccineGroupMap.get(vaccineGroupName);
  }

  public VaccineGroup getOrCreateVaccineGroup(String vaccineGroupName) {
    VaccineGroup vaccineGroup = getVaccineGroup(vaccineGroupName);
    if (vaccineGroup == null) {
      vaccineGroup = new VaccineGroup();
      vaccineGroup.setName(vaccineGroupName);
      vaccineGroupMap.put(vaccineGroupName, vaccineGroup);
    }
    return vaccineGroup;
  }

  public List<VaccineGroup> getVaccineGroupList() {
    if (vaccineGroupList == null) {
      vaccineGroupList = new ArrayList<VaccineGroup>(vaccineGroupMap.values());
    }
    return vaccineGroupList;
  }

  public void setVaccineGroupList(List<VaccineGroup> vaccineGroupList) {
    this.vaccineGroupList = vaccineGroupList;
  }

  public List<Schedule> getScheduleList() {
    return scheduleList;
  }

  public List<AntigenSeries> getAntigenSeriesList() {
    return antigenSeriesList;
  }

  public List<LiveVirusConflict> getLiveVirusConflictList() {
    return liveVirusConflictList;
  }

  public Map<String, Observation> getObservationMap() {
    return observationMap;
  }

}
