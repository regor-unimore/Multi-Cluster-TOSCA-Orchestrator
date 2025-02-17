package torch.javaModel.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

//"location":{"cap_type":"torch.capabilities.Location.Container","rel_type":"tosca.relationships.HostedOn","capability":"host","target":"cluster1"}
public class Relationship implements Serializable{
    private String rel_type;
    private String target;
    private String cap_type;
    private String capability;
    private Map<String,Object> properties = new HashMap<>();
    
    public Relationship() {}

    public Relationship(String rel_type, String target, String cap_type, String capability,
            Map<String, Object> properties) {
        this.rel_type = rel_type;
        this.target = target;
        this.cap_type = cap_type;
        this.capability = capability;
        this.properties = properties;
    }

    public String getRel_type() {
        return rel_type;
    }

    public void setRel_type(String rel_type) {
        this.rel_type = rel_type;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getCap_type() {
        return cap_type;
    }

    public void setCap_type(String cap_type) {
        this.cap_type = cap_type;
    }

    public String getCapability() {
        return capability;
    }

    public void setCapability(String capability) {
        this.capability = capability;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    

    
}
