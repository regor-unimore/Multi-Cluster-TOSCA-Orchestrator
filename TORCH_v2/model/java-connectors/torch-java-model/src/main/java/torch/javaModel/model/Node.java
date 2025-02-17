package torch.javaModel.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class Node implements Serializable {
	
	private String name;
	private String type;
	private String category;
	private String template;
	private Map<String, List<String>> requirements;
	private Map<String, Object> nodeProperties;
	private Map<String, Map<String, Object>> capProperties;
	private Map<String, Map<String, Object>> reqProperties;
	private Map<String, Relationship> relationships;
	private Map<String, Object> nodeAttributes;
	private Map<String, Map<String, Object>> capAttributes;
	
	public enum Category {
		VM,
		WS,
		SC,
		DBMS,
		DB,
		WA,
		CLUSTER,
		DU,
		UNRECOGNIZED;
		
		public static Category forValue(String value) {
			if (value != null) {
				for (Category c : Category.values()) {
					if (c.name().equalsIgnoreCase(value))
						return c;
				}
			}
			return Category.UNRECOGNIZED;
		}
		
		public String value() {
	        return name().toLowerCase();
		}
		
	}

	public Node() {}

	public Node(String name, String type, String category, String template,
			Map<String, List<String>> requirements,  
			Map<String, Object> nodeProperties,
			Map<String, Map<String, Object>> capProperties,
			Map<String, Map<String, Object>> reqProperties,
			Map<String, Relationship> relationships,
			Map<String, Object> nodeAttributes,
			Map<String, Map<String, Object>> capAttributes) {
		this.name = name;
		this.type = type;
		this.category = category;
		this.template = template;
		this.requirements = requirements;
		this.nodeProperties = nodeProperties;
		this.capProperties = capProperties;
		this.reqProperties = reqProperties;
		this.relationships = relationships;
		this.nodeAttributes = nodeAttributes;
		this.capAttributes = capAttributes;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public String getCategory() {
		return category;
	}
	
	public void setCategory(String category) {
		this.category = category;
	}

	public String getTemplate() {
		return template;
	}
	
	public void setTemplate(String template) {
		this.template = template;
	}

	public Map<String, List<String>> getRequirements() {
		return requirements;
	}

	public void setRequirements(Map<String, List<String>> requirements) {
		this.requirements = requirements;
	}

	public void setNodeProperties(Map<String, Object> nodeProperties) {
		this.nodeProperties = nodeProperties;
	}

	public Map<String, Object> getNodeProperties() {
		return nodeProperties;
	}

	public void setCapProperties(Map<String, Map<String, Object>> capProperties) {
		this.capProperties = capProperties;
	}

	public Map<String, Map<String, Object>> getCapProperties() {
		return capProperties;
	}

	public void setReqProperties(Map<String, Map<String, Object>> reqProperties) {
		this.reqProperties = reqProperties;
	}

	public Map<String, Map<String, Object>> getReqProperties() {
		return reqProperties;
	}

	public void updateReqProperties(String key, Map<String, Object> value){
		if(this.reqProperties == null){
			this.reqProperties = new HashMap<String, Map<String, Object>>();
			this.reqProperties.put(key, value);
		 }
		 else if(this.reqProperties.containsKey(key)){
			 this.reqProperties.get(key).putAll(value);
		 }
		 else{
			 this.reqProperties.put(key, value);
		 }
	}

	public void updateReqProperties(Map<String, Map<String, Object>> map){
		if(this.reqProperties == null){
			this.reqProperties = map;
		 }
		else {
			for(String key : map.keySet()){
				updateReqProperties(key, map.get(key));
			}
		}
	}

	public void setRelationships(Map<String, Relationship> relationships) {
		this.relationships = relationships;
	}

	public Map<String, Relationship> getRelationships() {
		return relationships;
	}

	public void setNodeAttributes(Map<String, Object> nodeAttributes) {
		this.nodeAttributes = nodeAttributes;
	}

	public Map<String, Object> getNodeAttributes() {
		return nodeAttributes;
	}

    public void updateNodeAttributes(String key, Object value){
		if(this.nodeAttributes == null){
			this.nodeAttributes = new HashMap<String, Object>();
		}
		this.nodeAttributes.put(key, value);
	}

	public void updateNodeAttributes(Map<String, Object> map){
		 
		if(this.nodeAttributes == null){
			this.nodeAttributes = map;
		} else {
		this.nodeAttributes.putAll(map);
		}
		
	}

	public void setCapAttributes(Map<String, Map<String, Object>> capAttributes) {
		this.capAttributes = capAttributes;
	}

	public Map<String, Map<String, Object>> getCapAttributes() {
		return capAttributes;
	}

	public void updateCapAttributes(String key, Map<String, Object> value){
		if(this.capAttributes == null){
           this.capAttributes = new HashMap<String, Map<String, Object>>();
           this.capAttributes.put(key, value);
		}
		else if(this.capAttributes.containsKey(key)){
			this.capAttributes.get(key).putAll(value);
		}
		else{
			this.capAttributes.put(key, value);
		}
	}

	public void updateCapAttributes(Map<String, Map<String, Object>> map){
		if(this.capAttributes == null){
			this.capAttributes = map;
		 }
		else {
			for(String key : map.keySet()){
				updateCapAttributes(key, map.get(key));
			}
		}
		
	}

	public String printRequirements() {
		String text = "{";
		for (Map.Entry<String, List<String>> requirement : requirements.entrySet()) {
			text += requirement.getKey() + ": [";
			for (String item : requirement.getValue())
				text += item + ", ";
			if (!requirement.getValue().isEmpty())
				text = text.substring(0, text.length()-2);
			text += "], ";
		}
		text = text.substring(0, text.length()-2) + "}";

		return text;
	}

	public String printNodeProperties() {
		String text = "{";
		for (Map.Entry<String, Object> property : nodeProperties.entrySet())
			text += property.getKey() + ": " + property.getValue() + ", ";
		text = text.substring(0, text.length()-2) + "}";

		return text;
	}

	public String printCapProperties() {
		String text = "{";
		for (Map.Entry<String, Map<String, Object>> cap : capProperties.entrySet()){
           for(Map.Entry<String, Object> property : cap.getValue().entrySet())
		       text += cap.getKey() + "::" + property.getKey() + ": " + property.getValue() + ", ";
		}
		text = text.substring(0, text.length()-2) + "}";

		return text;
	}

	public String printReqProperties() {
		String text = "{";
		for (Map.Entry<String, Map<String, Object>> req : reqProperties.entrySet()){
           for(Map.Entry<String, Object> property : req.getValue().entrySet())
		       text += req.getKey() + "::" + property.getKey() + ": " + property.getValue() + ", ";
		}
		text = text.substring(0, text.length()-2) + "}";

		return text;
	}

	public String printProperties() {
		String text = "{";
		text += "requirementsProperties : " + printReqProperties();
		text += ", nodeProperties : " + printNodeProperties();
		text += ", capabilitiesProperties : " + printCapProperties();
		text = text.substring(0, text.length()-2) + "}";

		return text;
	}

    public String printRelationships(){
		String text = "{";
		for (Map.Entry<String, Relationship> rel : relationships.entrySet()){
           for(Map.Entry<String, Object> property : rel.getValue().getProperties().entrySet())
		       text += rel.getKey() + "::" + property.getKey() + ": " + property.getValue() + ", ";
		}
		text = text.substring(0, text.length()-2) + "}";

		return text;
	}

	@Override
	public String toString() {
		String text = "Name: " + this.name + "\tType: " + this.type
				+ "\tCategory: " + this.category + "\tRequirements: "
				+ printRequirements() + "\tProperties: " + printProperties()
				+ "\tRelationships: " + printRelationships();

		return text;
	}

}
