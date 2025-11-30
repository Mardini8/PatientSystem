package org.journalsystem.dto.fhir;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FhirBundle {
    public String resourceType;
    public String type;
    public int total;
    public List<BundleEntry> entry;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BundleEntry {
        public FhirResource resource;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FhirResource {
        public String resourceType;
        public String id;
        public List<HumanName> name;
        public List<Identifier> identifier;
        public String birthDate;

        // For Condition
        public CodeableConcept code;
        public Reference subject;
        public String recordedDate;

        // For Encounter
        public Period period;
        public List<Participant> participant;

        // For Practitioner
        public List<Qualification> qualification;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HumanName {
        public List<String> given;
        public String family;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Identifier {
        public String value;
        public String system;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CodeableConcept {
        public String text;
        public List<Coding> coding;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Coding {
        public String code;
        public String display;
        public String system;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Reference {
        public String reference;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Period {
        public String start;
        public String end;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Participant {
        public Reference individual;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Qualification {
        public CodeableConcept code;
    }
}