
import json
from toscaparser import functions
from toscaparser.torch_handlers.requirements_handler import RequirementsHandler
from toscaparser.torch_handlers.capabilities_handler import CapabilitiesHandler
from toscaparser.torch_handlers.properties_handler import PropertiesHandler
from toscaparser.utils.merger import limited_deep_merge
#from toscaparser import template_printer

PACKAGE_MAPPING = { 
   "tosca.nodes.WebApplication"        : "wa", 
   "tosca.nodes.DBMS"                  : "dbms", 
   "tosca.nodes.WebServer"             : "ws", 
   "tosca.nodes.Database"              : "db",
   "tosca.nodes.SoftwareComponent"     : "sc",
   "tosca.nodes.LoadBalancer"          : "lb"
}


RESOURCE_MAPPING = { 
   #"tosca.nodes.Compute"       			: "vm",
   #"tosca.nodes.ObjectStorage"				: "obj_store", 
   #"tosca.nodes.BlockStorage"				: "block_store",
   "torch.nodes.Location.Cluster"			: "cluster",
   "torch.nodes.Location.Federation"    		: "federation",
   "torch.nodes.LocationService.VolumeProvider"		: "storage",
   "torch.nodes.LocationResource.Volume"		: "volume"
}

DU_MAPPING = {
   "torch.nodes.ApplicationService"       : "app"
}


# python tosca_parser.py --template-file=toscaparser/tests/data/tosca_container_wordpress.yaml

class ConfigFileUtility(object):

   def __init__(self, *args, **kwargs):
      super().__init__(*args, **kwargs)

   '''
   Resulting structure:
      [inputs] = {"input_name" : { obj_name : { (node_related | req | cap, prop_name) }  }}
      [objects] = torch objects
   '''
   
   #########################################################################
   ####                            GENERATORS                           ####
   #########################################################################
   
   def generate_json(self, tosca, processed_tosca, name):
      #check input @TODO 
      torch_objs = []
      dus =[]
      #locations = []
      inputs = {}
      result = {}
      prop_handler = PropertiesHandler(processed_tosca)
      #extract Deployment Unit Object and Cluster Object from the processed Tosca Template
      for n,v in processed_tosca["nodes"].items():
         if self.satisfyDUCondition(v):
             linked_attr , du = self.createDUobject(v, processed_tosca, inputs)
             du["template"] = name
             torch_objs.append({"node" : du, "used_attributes" : linked_attr})
         elif self.satisfyClusterCondition(v):
             linked_attr, cluster = self.createClusterObject(v, processed_tosca, inputs)
             cluster["template"] = name
             torch_objs.append({"node" : cluster, "used_attributes" : linked_attr}) 
         elif self.satisfyFederationCondition(v):
             linked_attr, federation = self.createFederationObject(v, processed_tosca, inputs)
             federation["template"] = name
             torch_objs.append({"node" : federation, "used_attributes" : linked_attr})
         elif self.isLocationService(v):
             linked_attr, service = self.createLocationServiceObject(v, processed_tosca, inputs)
             service["template"] = name
             torch_objs.append({"node" : service, "used_attributes" : linked_attr})
         elif self.isLocationResource(v):
             linked_attr, resource = self.createLocationResourceObject(v, processed_tosca, inputs)
             resource["template"] = name
             torch_objs.append({"node" : resource, "used_attributes" : linked_attr}) 
      #update Torch Object with the properties of the Relationships
      for obj in torch_objs:
         o = obj["node"]
         if "relationships" in o:
            for r,v in o["relationships"].items():
               #skip virtual relationships (i.e., for federations)
               if "rel" not in v or v["rel"] not in processed_tosca["relationships"]:
                 continue
               rel = processed_tosca["relationships"][v["rel"]]
               #add rel properties to the object properties 
               for p, pv in rel["properties"].items():
                  value, linked_value = prop_handler.getProperty(pv)
                  # if value is None and no input is associated with the properties, it can be ignored
                  if value is None and linked_value is None:
                     continue
                  if not r in o["reqProperties"]:
                     o["reqProperties"][r] = {p : value}
                  else:
                     o["reqProperties"][r][p] = value
                  if linked_value and linked_value[0] == "input":
                     linked_inputs = linked_value[1]
                     if linked_input in processed_tosca["inputs"].keys():
                        if not linked_input in inputs:
                           inputs[linked_input] = {  o["name"] : { "reqProperties": [(r, p)] } }
                        elif o["name"] not in inputs[linked_input]:
                           inputs[linked_input][o["name"]]= { "reqProperties":  [(r, p)] }
                        elif "reqProperties" not in inputs[linked_input][o["name"]]:
                           inputs[linked_input][o["name"]]["reqProperties"] = [(r, p)]
                        else:
                           inputs[linked_input][o["name"]]["reqProperties"].append( (r, p))
                  if linked_value and linked_value[0] == "attribute":
                     #TODO
                     print("[ERROR] - relationships get_attribute() values are not supported yet")
                 
               #add target capability to relationships to use in the Connectors
               if "capability" in rel["target"]:
                  o["relationships"][r]["capability"] = rel["target"]["capability"]
                  o["relationships"][r]["target"] = rel["target"]["node"]
                  o["relationships"][r].pop("rel")
               else:
                  o["relationships"][r]["capability"] = rel["target"]["implicit_capability"]
                  o["relationships"][r]["target"] = rel["target"]["node"]
                  o["relationships"][r].pop("rel")
            #update DU object category which is based on info contained in HostedOn relationship
            if o["type"] == "du":
               o["category"] = self.getCategoryForDU(o, processed_tosca)
            elif o["type"] == "resource":
               if o["category"] == "service":
                  o["category"], o["nodeProperties"]["platform"] = self.getCategoryForService(o, processed_tosca)
               elif o["category"] == "resource":
                  o["category"], o["nodeProperties"]["provider"] = self.getCategoryForResource(o, processed_tosca, torch_objs)
       
      
      result["objects"] = torch_objs
      result["inputs"] = inputs
      # with open("output.json", "w") as write_file:
      #    json.dump([v for v in {**graph_dict["node_templates"], **graph_dict["deployment_units"]}.values()], write_file)
      #print(json.dumps([v for v in {**torch_obj["node_templates"], **torch_obj["deployment_units"]}.values()]), end="")
      print(json.dumps(result)) 

   '''
   DU structure:
     [name]
     [type] = "du"
     [category] = "service"  # not used for now
     -------------------------
     [properties]
        [node_related]  -> generic info for creating the node
        [requirements_related] -> generic info for supporting the functionality of the node (depends on the relationship characteristics)
        [capabilities_related] -> generic info for making the node accessible and usable by others
     [envs] -> inputs of interface Standard, passend as environmental variable to the container at startup
     [requirements] : {"create" : [], "configure":[]}
     [containers] : []
     [relationships]: mappa ciascun requisito soddisfatto al nome della capacità che lo soddisfa e del nodo
   '''   
   def createDUobject(self, node, processed_tosca, inputs):
      du = {}   
      req_handler = RequirementsHandler(processed_tosca)
      cap_handler = CapabilitiesHandler()
      prop_handler = PropertiesHandler(processed_tosca)
      #NAME
      du["name"] = node["name"]
      #TYPE
      du["type"] = "du"
      #CATEGORY
      du["category"] = ""
      
      #NODE INFO
      du = self.createNodeObject(node, processed_tosca, inputs, du)
      
      #USED_ATTRIBUTES
      linked_attrs = {}
      
      #ENVS
      #   -> from the "inputs" field in the Standard > create interface
      #   -> from the capabilities of the node, with name: CAP_<cap_name>_<prop_name> (computed by Connectors!)
      #   -> from the attributes of the caoabilities required by the node, with name  REQ_<req_name>_<attr_name> (computed by Connectors!)
      #   -> from the relationships used by the node, with name REL_<req_name>_<rel_prop_name> (computed by Connectors!)
      du["envs"] = {}
      #if "interfaces" in node and "inputs" in node["interfaces"]["Standard"]["create"]:
      #   for i,v in node["interfaces"]["Standard"]["create"]["inputs"].items():
      #      value , linked_value = prop_handler.getProperty(v)
      #      if value:
      #         du["envs"][i] = str(value)
      #      else:
      #         du["envs"][i] = ""
      #      if linked_value and linked_value[0] == "input":
      #         linked_input = linked_value[1]
      #         if linked_input in processed_tosca["inputs"].keys():
      #            if not linked_input in inputs:
      #               inputs[linked_input] = {  du["name"] : {"envs": [ i ] } }
      #            elif du["name"] not in inputs[linked_input]:
      #               inputs[linked_input][du["name"]]= { "envs":  [ i ] }
      #            elif "envs" not in inputs[linked_input][du["name"]]:
      #               inputs[linked_input][du["name"]]["envs"] = [ i ]
      #            else:
      #               inputs[linked_input][du["name"]]["envs"].append( i )
      #      if linked_value and linked_value[0] == "attribute":
      #         linked_attribute = linked_value[1]
      #         if len(linked_attribute) == 3:
      #            linked_type = "capability"
      #         else:
      #            linked_type = "node"
      #         entry = { linked_attribute[0] : {linked_type : { linked_attribute[1] : { linked_attribute[2] : { du["name"] : { "envs":  [ i ] } }}}} }
      #         linked_attrs = limited_deep_merge(linked_attrs, entry, 6)
               
      #CONTAINER (ARTIFACTS + INTERFACES + setting of ENVS in du)
      linked_attrs, du["containers"] = self.generateContainersObject(node, processed_tosca, inputs, du["envs"])
      return (linked_attrs , du)
   
   def createNodeObject(self, node, processed_tosca, inputs, json_obj):
      req_handler = RequirementsHandler(processed_tosca)
      cap_handler = CapabilitiesHandler()
      prop_handler = PropertiesHandler(processed_tosca)
            #NODE-RELATED PROPERTIES:
      '''
      Node-related Properties: 
          are properties definded in the "properties" field of the node_templates or 
          on the node type from which this node template derives up to the root node 
          type (tosca.nodes.Root). 
          They are used by TORCH only for orchestrating the deployment of the node,
          therefore there is no need to convert them into environment variables
      '''
      json_obj["reqProperties"] = {}
      json_obj["nodeProperties"] = {}
      json_obj["capProperties"] = {}
      for p,v in node["properties"].items():
         value, linked_value = prop_handler.getProperty(v)
         # if value is None and no input is associated with the properties, it can be ignored
         if value is None and linked_value is None:
            continue
         json_obj["nodeProperties"][v["name"]] = value
         if linked_value and linked_value[0] == "input":
            linked_input = linked_value[1]
            if linked_input in processed_tosca["inputs"].keys():
               if not linked_input in inputs:
                  inputs[linked_input] = {  json_obj["name"] : { "nodeProperties": [prop["name"]] } }
               elif json_obj["name"] not in inputs[linked_input]:
                  inputs[linked_input][json_obj["name"]]= { "nodeProperties":  [prop["name"]] }
               elif "nodeProperties" not in inputs[linked_input][json_obj["name"]]:
                  inputs[linked_input][json_obj["name"]]["nodeProperties"] = [prop["name"]]
               else:
                  inputs[linked_input][json_obj["name"]]["nodeProperties"].append(prop["name"])      
      # REQUIREMENTS
      json_obj["relationships"] = {}
      json_obj["requirements"] = {"create" : [], "configure":[]}
      for r,v in node["requirements"].items():
         if v["selected"] and "node" in v["selected"] and v["selected"]["node"] in processed_tosca["nodes"].keys():
            if isinstance(v["selected"]["relationship"], dict):
               json_obj["relationships"][r] = {"rel" : "rel-" + node["name"] + "-" + r }
            else:
               json_obj["relationships"][r] = {"rel" : v["selected"]["relationship"]}
            json_obj["relationships"][r]["cap_type"] = v["capability"]
            if "type" not in processed_tosca["relationships"][json_obj["relationships"][r]["rel"]]:
              json_obj["relationships"][r]["rel_type"] = v["relationship"]
            else:
              json_obj["relationships"][r]["rel_type"] = processed_tosca["relationships"][json_obj["relationships"][r]["rel"]]["type"]
            if v["selected"]["node"] != node["name"]:
               json_obj = self.setRequiredStatus(processed_tosca["nodes"][v["selected"]["node"]], json_obj) 
         else:
            if int(v["occurrences"][0]) > 0:
               #ERROR! @TODO
               print("[ERROR]: requirement " + node["name"] + "." + v["name"] + "not satisfyied! TARGET node is" + str(v["selected"] ))
      
      #CAPABILITIES-RELATED PROPERTIES
      '''
      Capabilities-related properties are the properties defined in the "properties" field 
      of the capabilities of a node template or capability type from which the capability 
      derives up to the root type (tosca.capabilities.Root).
      Capability properties are used by Torch to configure the node properly at deployment time.
      Capabilities are also associated with a specific set of attributes (based on the type of the
      capability) that are displayed in the TORCH dashboard and are also used for configuration of
      nodes that need those capabilities and are mapped to environment variables as well.
      Attributes are known only at runtime (after deployment is finished) so they can't really 
      be parsed here. Attributes are extracted by the Connectors and returned to the BPMN Engine,
      which in turn can be inspected by the TORCH Dashboard and other TORCH Connectors
      '''
      for c, v in node["capabilities"].items():
         json_obj["capProperties"][c] = {"type" : v["type"]}
         for p, prop in v["properties"].items():
            value, linked_value = prop_handler.getProperty(prop)
            # if value is None and no input is associated with the properties, it can be ignored
            if value is None and linked_value is None:
               continue
            json_obj["capProperties"][c][p] = value
            if linked_value and linked_value[0] == "input":
               linked_input = linked_value[1]
               if linked_input in processed_tosca["inputs"].keys():
                  if not linked_input in inputs:
                     inputs[linked_input] = {  json_obj["name"] : {"capProperties": [ (c, prop["name"]) ] } }
                  elif json_obj["name"] not in inputs[linked_input]:
                     inputs[linked_input][json_obj["name"]]= { "capProperties":  [ (c, prop["name"]) ] }
                  elif "capProperties" not in inputs[linked_input][json_obj["name"]]:
                     inputs[linked_input][json_obj["name"]]["capProperties"] = [ (c, prop["name"]) ]
                  else:
                     inputs[linked_input][json_obj["name"]]["capProperties"].append( (c, prop["name"]) )
      
      return json_obj 
   
   def createClusterObject(self, node, processed_tosca, inputs):
      prop_handler = PropertiesHandler(processed_tosca)
      cluster = {}
      cluster["name"] = node["name"]
      cluster["type"] = "resource"
      cluster["category"] = "cluster"
      cluster = self.createNodeObject(node, processed_tosca, inputs, cluster)
      linked_attrs = {}
      return (linked_attrs, cluster)
   
   def createLocationServiceObject(self, node, processed_tosca, inputs):
      prop_handler = PropertiesHandler(processed_tosca)
      service = {}
      service["name"] = node["name"]
      service["type"] = "resource"
      service["category"] = "service"
      service = self.createNodeObject(node, processed_tosca, inputs, service)  
      linked_attrs = {}
      return (linked_attrs, service)
   
   def createLocationResourceObject(self, node, processed_tosca, inputs):
      prop_handler = PropertiesHandler(processed_tosca)
      res = {}
      res["name"] = node["name"]
      res["type"] = "resource"
      res["category"] = "resource"
      res = self.createNodeObject(node, processed_tosca, inputs, res)         
      linked_attrs = {}
      # add specific info for certain type of resourceObject
      res = self.refineResourceObject(node, processed_tosca, inputs, res)
      return (linked_attrs, res)
     
   # @TODO -> extends to support get properties and attributes
   def createFederationObject(self, node, processed_tosca, inputs):
      prop_handler = PropertiesHandler(processed_tosca)
      fed = {}
      fed["name"] = node["name"]
      fed["type"] = "resource"
      fed["category"] = "federation"
      fed = self.createNodeObject(node, processed_tosca, inputs, fed)
      
      #@TODO: since we currently don't support one-to-many rel we have to create virtual relationships between Federation
      #   and Clusters despite not existing in the template! We do this by adding manually the information required by torch
      # 0) create virtual relationship torch.relationships.Location.HasMember @TODO
      fed["relationships"]["members"] = { 
             "cap_type":"torch.capabilities.Location.Container",
             "rel_type":"torch.relationships.Location.HasMember",
             "targets": {}
      }
      for key, info in fed["nodeProperties"]["clusters"].items():
         if key not in processed_tosca["nodes"].keys():
           print("[ERROR]: Federation " + fed["name"] + " has invalid member - " + key + " Location.Cluster is not defined")
         else: 
           #1) create virtual "dependency" 
           fed["requirements"]["create"].append(key + ".create")
           #2) update virtual relationship torch.relationships.Location.HasMember @TODO
           fed["relationships"]["members"]["targets"][key] = "host" 
           #2-B) fill the clusters properties with the infos of the Cluster (= the virtual relationship properties)
           cluster = processed_tosca["nodes"][key]
           for p,v in cluster["capabilities"]["host"]["properties"].items():
              if not info or p not in info.keys():
                 #fed["nodeProperties"]["clusters"][key][p] 
                 value, linked_value = prop_handler.getProperty(v)
                 # if value is None and no input is associated with the properties, it can be ignored
                 if value is None and linked_value is None:
                     continue
                 fed["nodeProperties"]["clusters"][key][p] = value
                 if linked_value and linked_value[0] == "input":
                    linked_input = linked_value[1]
                    if linked_input in processed_tosca["inputs"].keys():
                       if not linked_input in inputs:
                          inputs[linked_input] = {  fed["name"] : {"nodeProperties": [ "clusters." + key + "." + p] } }
                       elif fed["name"] not in inputs[linked_input]:
                          inputs[linked_input][fed["name"]]= { "nodeProperties":  [ "clusters." + key + "." + p ] }
                       elif "capProperties" not in inputs[linked_input][fed["name"]]:
                          inputs[linked_input][fed["name"]]["capProperties"] = [ "clusters." + key + "." + p ]
                       else:
                          inputs[linked_input][fed["name"]]["capProperties"].append( "clusters." + key + "." + p )
              
      #TODO
      linked_attrs = {}
      return (linked_attrs, fed)
     
   def generateContainersObject(self, node, processed_tosca, inputs, envs):
      #it's plural because we may decide we need to deploy container sidecar to support the node lifecycle!
      #  i.e; for metrics collection or to add special adapter for compatibility purpose
      containers = []
      linked_attrs = {}
      
      # @TODO
      linked_attrs, app_container = self.generateMainContainer(node, processed_tosca, inputs, linked_attrs, envs)
 
      containers.append(app_container)
      return (linked_attrs, containers)    
   
   def generateMainContainer(self, node, processed_tosca, inputs, linked_attrs, envs):
      container = {}
      container["configuration_script"] = "no.configuration.script"  #@CHECK?
      container["name"] = node["name"]    #is this useful???
      docker_image = {}
      docker_image["file"] = node["interfaces"]["Standard"]["create"]["implementation"]["file"]
      docker_image["repository"] = node["interfaces"]["Standard"]["create"]["implementation"]["repository"]["value"]
      if not docker_image["repository"]: #if its empty
         docker_image["repository"] = "DOCKERHUB"  #@TODO use url    
      #if docker_image["repository"] in processed_tosca["repositories"].keys():
      #   docker_image["repository"] = processed_tosca["repositories"][docker_image["repository"]]["value"]
      container["image"] = docker_image
      
      # input of the docker image are treated as ENVS
      if "inputs" in node["interfaces"]["Standard"]["create"]:
         en, linked_attrs = self.addEnvs(node["interfaces"]["Standard"]["create"]["inputs"], node["name"], processed_tosca, inputs, linked_attrs, "create")
         envs.update(en)
      
      
      # optional docker COMMAND is setted on the start operation
      if "start" in  node["interfaces"]["Standard"]:
         linked_attrs, cmd = self.generateDockerCommand(node, processed_tosca, inputs, linked_attrs, envs)
         if cmd:
            container["command"] = cmd["command"]
            container["args"] = cmd["args"]
            
      
      return (linked_attrs, container)
   
   def generateDockerCommand(self, node, processed_tosca, inputs, linked_attrs, envs):
      cmd = {}
      artifact = node["interfaces"]["Standard"]["start"]["implementation"]
      if_inputs = node["interfaces"]["Standard"]["start"]["inputs"]
      
      if artifact["type"] == "torch.artifacts.Implementation.SingleLineCommand.Bash":
         # @TODO if deploy_path is setted add `cd <deploy_path;>` at the start of artifact[file] 
         cmd["command"] = "/bin/bash"
         user_command = artifact["file"]
         if "deploy_path" in artifact and artifact["deploy_path"]:
            user_command = "cd " + artifact["deploy_path"] + " ; " + user_command
         cmd["args"] = ["-c", user_command]
         # Inputs need to be setted as ENVs
         en, linked_attrs = self.addEnvs(if_inputs, node["name"], processed_tosca, inputs, linked_attrs, "start")
         envs.update(en)
         # also we add them as positional arguments after the previous arguments
         for i in if_inputs:
           cmd["args"].append("$" + i)
      
      return (linked_attrs, cmd)
      
      # @TODO: what if the user set the value of a property that should be translated in the repository to a get_input() function?
      # Torch must know how to check and retrieve the repository info and translate this field to that value....
   def refineResourceObject(self, node, processed_tosca, inputs, res):
      if("torch.nodes.LocationResource.Volume" in node["hierarchy"]):
            if ("pre-populate" in res["nodeProperties"] and "source_repo" in res["nodeProperties"]["pre-populate"]):
               # add repository info to source_repo
               repo = res["nodeProperties"]["pre-populate"]["source_repo"]
               if repo in processed_tosca["repositories"]:
                  res["nodeProperties"]["pre-populate"]["source_repo"] = processed_tosca["repositories"][repo]
                  return res
      return res
            
   
   #########################################################################
   ####                       UTILITY FUNCTIONS                         ####
   #########################################################################
       
   def satisfyClusterCondition(self, node):
      if "torch.nodes.Location.Cluster" in node["hierarchy"]:
         return True
      return False
   
   def satisfyFederationCondition(self, node):
      if "torch.nodes.Location.Federation" in node["hierarchy"]:
         return True
      return False
     
   def satisfyDUCondition(self, node):
      for parent in node["hierarchy"]:
         if parent in DU_MAPPING:
            return True
      return False
   
   def isApplicationService(self, node):
      if "torch.nodes.ApplicationService" in node["hierarchy"]:
         return True
      return False
   
   def isLocation(self, node):
      if "torch.nodes.Location" in node["hierarchy"]:
         return True
      return False
   
   def isLocationService(self, node):
      if "torch.nodes.LocationService" in node["hierarchy"]:
         return True
      return False
   
   def isLocationResource(self, node):
      if "torch.nodes.LocationResource" in node["hierarchy"]:
         return True
      return False  
   
   def setRequiredStatus(self, requiredNode, json_obj):
      if self.isApplicationService(requiredNode):
         json_obj["requirements"]["create"].append(requiredNode["name"] + ".start")
      elif self.isLocation(requiredNode):
         json_obj["requirements"]["create"].append(requiredNode["name"] + ".create")
      else:
         json_obj["requirements"]["create"].append(requiredNode["name"]+ ".create")
      return json_obj
   
   def getCategoryForDU(self, du, processed_tosca):
      #CATEGORY = Provider(depends on the node satisfying the HostedOn relationship) + Category
      node = processed_tosca["nodes"][du["name"]]
      for parent in node["hierarchy"]:
         if parent in DU_MAPPING:
            #category based on mappings
            category = DU_MAPPING[parent]
            break

      try:
         host = du["relationships"]["location"]["target"]
         provider = processed_tosca["nodes"][host]["properties"]["platform"]["value"]
      except:
         return category
      
      result = provider + "-" + category
      return result
   
   def getCategoryForService(self, service, processed_tosca):
      # category: type of LocationService, mapping is defined in RESOURCE_MAPPING var
      # platform: technology that implements this LocationService. if "default" the platform has the same value as the platoform of the location in which is hosted the LocationServcie
      # provider: tool/connectors that can install the platform in the Location.
      node = processed_tosca["nodes"][service["name"]]
      for parent in node["hierarchy"]:
         if parent in RESOURCE_MAPPING:
            #category based on mappings
            category = RESOURCE_MAPPING[parent]
            break
      platform = service["nodeProperties"]["platform"]
      if platform == "default":
         host = service["relationships"]["location"]["target"]
         platform = processed_tosca["nodes"][host]["properties"]["platform"]["value"]
         #service["nodeProperties"]["platform"] = platform
      return category, platform
   
   def getCategoryForResource(self, res, processed_tosca, objs):
      # category: type of LocationResource, mapping is defined in RESOURCE_MAPPING var
      # platform: LocationResource are not platform, so this field is not used here
      # provider: tool/connectors that can create the resource. The provider is derived from the platoform of the LocationService generating this LocationResource if provider is not set
      node = processed_tosca["nodes"][res["name"]]
      for parent in node["hierarchy"]:
         if parent in RESOURCE_MAPPING:
            #category based on mappings
            category = RESOURCE_MAPPING[parent]
            break
      if "provider" in res["nodeProperties"] and res["nodeProperties"]["provider"] != "default":
         return category, res["nodeProperties"]["provider"]
      
      generator = res["relationships"]["provider"]["target"]
      for o in objs:
         if o["node"]["name"] == generator:
            provider = o["node"]["nodeProperties"]["platform"]
            if provider == "default":
               host = res["relationships"]["location"]["target"]
               provider = processed_tosca["nodes"][host]["properties"]["platform"]["value"]
            break
      return category, provider
   
   def addEnvs(self, values, name, processed_tosca, inputs, linked_attrs, interface):
      prop_handler = PropertiesHandler(processed_tosca)
      envs = {}
      
      for i,v in values.items():
         
         value , linked_value = prop_handler.getProperty(v)
         
         if value:
            envs[i] = str(value)
         else:
            envs[i] = ""
         if linked_value and linked_value[0] == "input":
            linked_input = linked_value[1]
            if linked_input in processed_tosca["inputs"].keys():
                  entry = { linked_input : { name : { interface : [i] } } }
                  inputs = limited_deep_merge(inputs, entry, 3)
         if linked_value and linked_value[0] == "attribute":
            linked_attribute = linked_value[1]
            
            if len(linked_attribute) == 3:
                  linked_type = "capability"
            else:
                  linked_type = "node"
            entry = { linked_attribute[0] : {linked_type : { linked_attribute[1] : { linked_attribute[2] : { name : { interface :  [ i ] } }}}} }
            linked_attrs = limited_deep_merge(linked_attrs, entry, 6)
      return (envs, linked_attrs)   

      


     
     
   
