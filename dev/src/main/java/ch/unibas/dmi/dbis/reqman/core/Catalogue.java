package ch.unibas.dmi.dbis.reqman.core;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;

/**
 * TODO: write JavaDoc
 *
 * @author loris.sauter
 */
public class Catalogue {

    private String lecture;
    private String name;
    private String description;
    private String semester;

    private List<Milestone> milestones = new Vector<Milestone>();
    private List<Requirement> requirements = new Vector<Requirement>();

    @JsonIgnore
    private Map<Integer, List<Requirement>> reqsPerMinMS = new TreeMap<>();

    public Catalogue() {

    }

    public Catalogue(String lecture, String name, String description, String semester) {
        this.lecture = lecture;
        this.name = name;
        this.description = description;
        this.semester = semester;
    }

    public String getLecture() {
        return lecture;
    }

    public void setLecture(String lecture) {
        this.lecture = lecture;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public void clearMilestones() {
        milestones = new ArrayList<>();
    }

    public void clearRequirements() {
        requirements = new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Catalogue catalogue = (Catalogue) o;

        if (getLecture() != null ? !getLecture().equals(catalogue.getLecture()) : catalogue.getLecture() != null) {
            return false;
        }
        if (getName() != null ? !getName().equals(catalogue.getName()) : catalogue.getName() != null) {
            return false;
        }
        if (getDescription() != null ? !getDescription().equals(catalogue.getDescription()) : catalogue.getDescription() != null) {
            return false;
        }
        if (getSemester() != null ? !getSemester().equals(catalogue.getSemester()) : catalogue.getSemester() != null) {
            return false;
        }
        if (milestones != null ? !milestones.equals(catalogue.milestones) : catalogue.milestones != null) {
            return false;
        }
        return requirements != null ? requirements.equals(catalogue.requirements) : catalogue.requirements == null;
    }

    @Override
    public int hashCode() {
        int result = getLecture() != null ? getLecture().hashCode() : 0;
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
        result = 31 * result + (getSemester() != null ? getSemester().hashCode() : 0);
        result = 31 * result + (milestones != null ? milestones.hashCode() : 0);
        result = 31 * result + (requirements != null ? requirements.hashCode() : 0);
        return result;
    }

    public boolean addMilestone(Milestone milestone) {
        // TODO: uncomment as soon as all done
        // milestone.setOrdinal(getLastOrdinal()+1);
        return milestones.add(milestone);
    }

    public boolean removeMilestone(Milestone milestone) {
        return milestones.remove(milestone);
    }

    public List<Milestone> getMilestones() {
        return new ArrayList<>(milestones);
    }

    public void setMilestones(List<Milestone> milestones) {
        this.milestones = milestones;
    }

    public boolean addRequirement(Requirement requirement) {

        if (reqsPerMinMS.get(requirement.getMinMilestoneOrdinal()) != null) {
            reqsPerMinMS.get(requirement.getMinMilestoneOrdinal()).add(requirement);
        } else {
            List<Requirement> list = new ArrayList<>(Arrays.asList(requirement));
            reqsPerMinMS.put(requirement.getMinMilestoneOrdinal(), list);
        }
        return requirements.add(requirement);
    }

    public boolean removeRequirement(Requirement requirement) {
        if (reqsPerMinMS.get(requirement.getMinMilestoneOrdinal()) != null) {
            reqsPerMinMS.get(requirement.getMinMilestoneOrdinal()).remove(requirement);
        }
        return requirements.remove(requirement);

    }

    public List<Requirement> getRequirements() {
        return new ArrayList<>(requirements);
    }

    @JsonIgnore
    public List<Requirement> requirementList(){
        return requirements;
    }

    public void setRequirements(List<Requirement> requirements) {
        this.requirements = requirements;
        this.reqsPerMinMS = new TreeMap<>();
        requirements.forEach(this::addRequirementToMSMap);
    }

    public void addAllRequirements(Requirement... requirements) {
        List<Requirement> list = new ArrayList<>(Arrays.asList(requirements));

        this.requirements.addAll(list);

        list.forEach(this::addRequirementToMSMap);

    }

    public void addAllMilestones(Milestone... milestones) {
        this.milestones.addAll(Arrays.asList(milestones));
    }

    public Milestone getMilestoneByOrdinal(int ordinal) {
        Milestone result = null;
        for (Milestone ms : milestones) {
            if (ms.getOrdinal() == ordinal) {
                result = ms;
            }
        }
        return result;
    }

    public List<Requirement> getRequirementsByMilestone(int ordinal) {
        ArrayList<Requirement> reqs = new ArrayList<>();
        for(Requirement r : requirements){
            if(r.getMinMilestoneOrdinal() <= ordinal && ordinal <= r.getMaxMilestoneOrdinal() ){
                reqs.add(r);
            }
        }
        return reqs;
    }

    public List<Requirement> getRequirementsWithMinMS(int ordinal){
        if (reqsPerMinMS.containsKey(ordinal)) {
            return new ArrayList<Requirement>(reqsPerMinMS.get(ordinal));
        } else {
            return null;
        }
    }

    @JsonIgnore
    public double getSum(int msOrdinal) {
        List<Requirement> reqs = reqsPerMinMS.get(msOrdinal);
        if (reqs == null) {
            return 0;
        } else {
            List<Double> points = new ArrayList<>();
            reqs.forEach(req -> points.add(!req.isMandatory() || req.isMalus() ? 0 : req.getMaxPoints()));
            return points.stream().mapToDouble(Double::doubleValue).sum();
        }
    }

    @JsonIgnore
    public double getSum() {
        List<Double> points = new ArrayList<>();
        reqsPerMinMS.keySet().forEach(ordinal -> {
            points.add(getSum(ordinal));
        });
        return points.stream().mapToDouble(Double::doubleValue).sum();
    }

    @JsonIgnore
    public Requirement getRequirementByName(String name) {
        for (Requirement r : requirements) {
            if (r.getName().equals(name)) {
                return r;
            }
        }
        return null;
    }

    @JsonIgnore
    public boolean containsRequirement(String name) {
        for (Requirement r : requirements) {
            if (r.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    public Requirement getRequirementForProgress(Progress progress) {
        return getRequirementByName(progress.getRequirementName());
    }

    @JsonIgnore
    public Milestone getMilestoneForProgress(Progress progress) {
        return getMilestoneByOrdinal(progress.getMilestoneOrdinal());
    }

    private void addRequirementToMSMap(Requirement requirement) {
        int ordinal = requirement.getMinMilestoneOrdinal();
        if (reqsPerMinMS.get(ordinal) != null) {
            reqsPerMinMS.get(ordinal).add(requirement);

        } else {
            reqsPerMinMS.put(ordinal, new ArrayList<>(Arrays.asList(requirement)));
        }
    }

    public int getLastOrdinal() {
        ArrayList<Milestone> list = new ArrayList<>(getMilestones());
        list.sort(Comparator.comparingInt(Milestone::getOrdinal));
        return list.get(list.size()-1).getOrdinal();
    }
}
