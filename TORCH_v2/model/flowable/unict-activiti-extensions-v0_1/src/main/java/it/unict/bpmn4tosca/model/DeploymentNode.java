package it.unict.bpmn4tosca.model;

import java.util.List;
import java.util.Map;

public class DeploymentNode extends Node {
	
	private List<Map<String, Object>> containers;
	private Map<String, String> envs;
	
	public DeploymentNode() {}

	public DeploymentNode(String name,
						  String type,
						  String category,
						  String template,
						  Map<String, List<String>> requirements,
						  Map<String, Object> nodeProperties,
						  Map<String, Map<String, Object>> capProperties,
						  Map<String, Map<String, Object>> reqProperties,
						  Map<String, Map<String, Object>> relationships,
						  Map<String, Object> nodeAttributes,
						  Map<String, Map<String, Object>> capAttributes,
						  List<Map<String, Object>> containers,
						  Map<String, String> envs ) {
		
//		super(name, type, null, requirements, properties);
		super(name, type, category, template, requirements, nodeProperties, capProperties, reqProperties, relationships, nodeAttributes, capAttributes);
		this.setContainers(containers);
		this.envs = envs;
	}

	public List<Map<String, Object>> getContainers() {
		return containers;
	}

	public void setContainers(List<Map<String, Object>> containers) {
		this.containers = containers;
	}

	public Map<String, String> getEnvs() {
		return envs;
	}

	public void setEnvs(Map<String, String> envs) {
		this.envs = envs;
	}

	public void addEnv(String name, String value){
	 	this.envs.put(name, value);
	}

	public void updateEnvs(Map<String,String> envs){
		if (this.envs == null)
		   this.envs = envs;
	    else{
			for (Map.Entry<String, String> env : envs.entrySet()){
				this.envs.put(env.getKey(), env.getValue());
			}
		}
        
	}

}
