from toscaparser.elements.entity_type import EntityType
from toscaparser.elements.property_definition import PropertyDef
from toscaparser.functions import GetInput, GetProperty, GetAttribute

#@ELISA

def processTemplate(tosca):
   processed_tosca = {}
   
   #0) parse all repositories, policy and groups? @TODO
   repos = {}
   for r in tosca.repositories:
      repo = {"name": r.name, "value" : r.reposit}
      repos[repo["name"]] = repo
   
   processed_tosca["repositories"] = repos
   
   
   
   # 1) parse all the node templates defined in the Template
   nodes = {}
   for n in tosca.nodetemplates:
      node = getNodeDict(n, repos)
      nodes[node["name"]] = node
      #printNodeDict(node)
   processed_tosca["nodes"] = nodes
   # 2) parse all the relationship templates defined in the Template
   rel_templates = {}
   for r in tosca.relationship_templates:
      rel = getExplicitRelationshipDict(r, tosca)
      rel_templates[rel["name"]] = rel
   
   # 3) find all the possible relationship that can be enstablished (both explicit and implicit)  
   relationships = {}
   for n in nodes:
      for r, rv in nodes[n]["requirements"].items():
         rel = {}
         tpl = {}
         if rv["selected"]:
            #if EXPLICIT RELATIONSHIP is defined
            if "relationship" in rv["selected"]:
               if isinstance(rv["selected"]["relationship"], dict):
                  #if relationship is a dict it MUST be because the relationship is declared anounimously, in this case:
                  #   relationship.type is the type of the relationship
                  #   relationship.properties are the properties of the relationship
                  rel["type"] = rv["selected"]["relationship"]["type"]
                  if "properties" in rv["selected"]["relationship"]:
                     rel["properties"] = rv["selected"]["relationship"]["properties"]
               else:
                  tpl = rel_templates[rv["selected"]["relationship"]]
                  rel["name"] = tpl["name"]
                  rel["type"] = tpl["type"]
                  if "source" in tpl:
                     rel["source"] = {"node" : tpl["source"]}
                  if "target" in tpl:
                     rel["target"] = {"node" : tpl["target"]}
                  rel["description"] = tpl["description"]
                  rel["hierarchy"] = tpl["hierarchy"]
                  rel["properties"] = tpl["properties"]
            # if EXPLICIT TARGET is defined    
            if "node" in rv["selected"]:
               if not "target" in rel:
                  rel["target"] = {"node" : rv["selected"]["node"]}
               else:
                  rel["target"]["node"] = rv["selected"]["node"]
               
               if "capability" in rv["selected"]:
                  rel["target"]["capability"] = rv["selected"]["capability"]
         if "type" not in rel:
            rel["type"] = rv["relationship"]
         if "source" not in rel:
            rel["source"] = {"node" : nodes[n]["name"], "requirement": r}
         else:
            rel["source"]["requirement"] = r
         if "name" not in rel:
            rel["name"] = "rel-" + rel["source"]["node"] + "-" + rel["source"]["requirement"]
         if "target" not in rel:
            rel["target"] = {}
         elif "capability" not in rel["target"]:
            cap = None
            target_node = rel["target"]["node"]
            for c in nodes[target_node]["capabilities"]:
               if rv["capability"] in nodes[target_node]["capabilities"][c]["hierarchy"]:
                  cap = c
                  break
            rel["target"]["implicit_capability"] = cap
         
         #@TODO - serve la capacità di ottenere info sui vari tipi esistenti
         #DESCRIPTION
         if not "description" in rel:
            rel["description"] = None
         #HIERARCHY & PROPERTIES for implicit relationships (or anonymous relationships)
         if "hierarchy" not in rel:
            if "properties" not in rel:
               rel["properties"] = {}
            r_type = None
            for r_obj in nodes[n]["object"].type_definition.relationship:
               if r_obj.type == rel["type"]:
                  r_type = r_obj
                  break
            if not r_type:
               continue
            for p in r_type.get_properties_def_objects():
               prop = getPropertyDict(p)
               rel["properties"][prop["name"]] = prop
            #HIERARCHY
            rel["hierarchy"] = []
            rel["hierarchy"].append(rel["type"])
            r_type = r_type.parent_type      
            while r_type :
                rel["hierarchy"].append(r_type.type)
                #add properties derived from the parent node type
                for p in r_type.get_properties_def_objects():
                   prop = getPropertyDict(p)
                   #if property was re-defined by upper class, ignore it
                   if not prop["name"] in rel["properties"]:
                      rel["properties"][prop["name"]] = prop
                      #@TODO constraints should be summed, not ignored!
                r_type = r_type.parent_type
            # note: properties value cannot be specified in the implicit syntax, so no need to check for values
         relationships[rel["name"]] = rel
         if rv["selected"] and "relationship" not in rv["selected"]:
            rv["selected"]["relationship"] = rel["name"]
   processed_tosca["relationships"] = relationships
   
   #parse inputs
   inputs = {}
   for i in tosca.inputs:
      inp = getInputDict(i) 
      inputs[inp["name"]] = inp
   processed_tosca["inputs"] = inputs
   
   return processed_tosca

def printProcessedTemplate(processed_template):
   print("NODE ENTITY: ")
   for key, value in processed_template["nodes"].items():
      printNodeDict(value)
      print()
   
   print("AVAILABLE RELATIONSHIPS: ")
   for key,value in processed_template["relationships"].items():
      printRelationshipDict(value)
      print()

####################################################################################################   
####################################################################################################
###                                UTILITY FUNCTIONS BELOW!                                      ###
####################################################################################################
####################################################################################################

def getNodeDict(n, repos):
   node = {}

   #NAME
   node["name"] = n.name
   #DESCRIPTION
   node["description"] = ""
   if hasattr(n, "defs") and "description" in n.defs :
      node["description"] = n.defs["description"]

   #TYPE
   node["type"] = n.type_definition.ntype
   #PROPERTIES 
   # @TODO bisogna controllare i constraint di tutti i tipi antenati se ci sono proprietà con lo stesso nome
   properties = {}
   for p in n.type_definition.get_properties_def_objects() :
      prop = getPropertyDict(p)
      properties[prop["name"]] = prop
   node["properties"] = properties
   
   #ARTIFACTS
   artifacts = {}
   if n.artifacts:
      for a in n.artifacts:
         art = getArtifactDict(a, repos)
         artifacts[art["name"]] = art
   #@TODO
   #INTERFACES
   node["interfaces"] = {"Standard" : {} }
   for i in n.interfaces:
      if i.type == "Standard": #for now we support only the Standard interface operations  @TODO
         name, op = getStandardOperationDict(i, artifacts)
         node["interfaces"]["Standard"][name] = op
   
   #HIERARCHY
   hierarchy = []
   t = n.type_definition
   while t :
      hierarchy.append(t.ntype)
      #add properties derived from the parent node type
      for p in t.get_properties_def_objects():
         prop = getPropertyDict(p)
         #if property was re-defined by upper class, ignore it
         if not prop["name"] in node["properties"]:
            node["properties"][prop["name"]] = prop
         #@TODO constraints should be summed, not ignored!      
      #add artifact if it was not re-defined by upper class
      if t.artifacts:
         for a, v in t.artifacts.items():
            v["name"] = a
            art = getArtifactDict(v, repos)
            if a not in artifacts:
               artifacts[art["name"]] = art
      
      #add all interfaces not overriden by upper class
      if t.interfaces:
         for i in t.interfaces:
            if i == "Standard": #for now we support only Standard interface operations @TODO
               for name, op in t.interfaces[i].items():
                  if name == "type": #Root return the definition of the Standard Interface, we can safely ignore it
                     continue
                  if name not in node["interfaces"]["Standard"]:
                     node["interfaces"]["Standard"][name] = {}
                     if "implementation" in op and op["implementation"] in artifacts:
                        node["interfaces"]["Standard"][name]["implementation"] = artifacts[op["implementation"]]
                     if "inputs" in op:
                        node["interfaces"]["Standard"][name]["inputs"] = op["inputs"]
                  else:
                     if "implementation" not in node["interfaces"]["Standard"][name] and "implementation" in op:
                        if op["implementation"] in artifacts:
                           node["interfaces"]["Standard"][name]["implementation"] = artifacts[op["implementation"]]
                     if "inputs" in op:
                        for in_name, in_value in op["inputs"].items():
                           if in_name not in node["interfaces"]["Standard"][name]["inputs"]:
                              node["interfaces"]["Standard"][name]["inputs"][in_name] = in_value
      t = t.parent_type

   node["hierarchy"] = hierarchy 
   for key in node["properties"]:
         #if "value" not in node["properties"][key]:
         value = n.get_property_value(key)
         if isinstance(value, GetInput):
            result = resolveGetInput(value)
            node["properties"][key]["value_linked_to"] = result["value_linked_to"]
            node["properties"][key]["value"] = result["target_value"]
         elif isinstance(value, GetProperty):
            result = resolveGetProperty(value)
            node["properties"][key]["value_linked_to"] = result["value_linked_to"]
            node["properties"][key]["value"] = result["target_value"]
         else:
            node["properties"][key]["value"] = value

   #CAPABILITIES
   capabilities = {}
   for c in n.type_definition.get_capabilities_objects():
       cap = getCapabilityDict(c)
       #UPDATE PROPERTIES
       cap_props = n.get_capability(cap["name"]).get_properties()
       for key in cap["properties"]:
          if "value" not in cap["properties"][key] or cap["properties"][key]["value"] is None:
             if key in cap_props:
                value = cap_props[key].value
                if type(value) is tuple:
                   #it's either derived from get_property or get_input
                   if value[0] == "input":
                      cap["properties"][key]["value"] = value[2]
                      cap["properties"][key]["value_linked_to"] = ("input", value[1])
                   elif value[0] == "property":
                      cap["properties"][key]["value_linked_to"] = ("property", value[1])
                      value = value[2]
                      linked_to = None
                      # we are interested only in the first linked_to if no get_input was called
                      # if the links ends with a get_input, we memorize it to easy the process of updating
                      # after the user insert the inputs
                      while type(value) is tuple:
                         linked_to = (value[0], value[1])
                         value = value[2]
                      if linked_to and linked_to[0] == "input":
                         cap["properties"][key]["value"] = (linked_to[0], linked_to[1], value)
                      else:
                         cap["properties"][key]["value"] = value
                   else:
                      cap["properties"][key]["value"] = value
                else:
                  cap["properties"][key]["value"] = value          
       capabilities[cap["name"]] = cap
       #UPDATE ATTIBUTES
       #@TODO not supported for now 
   node["capabilities"] = capabilities
    
   #@TODO
   #REQUIREMENTS
   requirements = {}
   for d in n.type_definition.requirements:
      req = getRequirementTypeDict(d)
      req["selected"] = None
      requirements[req["name"]] = req
   #fill requirements with selected value if exists 
   for r in n.requirements:   #list!
      for name, value in r.items():
         # selected.node must be the name of a valid node template
         # selected.relationship must be the name of a valid relationship template OR an extended form to specify the relationship anounimously
         # selected.capability must be the name of a capability in the selected node template and can be used only if selected.node is specifyied
         if name in requirements:
            if isinstance(value, str):
               requirements[name]["selected"] = {}
               requirements[name]["selected"]["node"] = value
            else:
               requirements[name]["selected"] = value #TO_CHANGE ELISA
               requirements[name]["selected"].pop("capability", None)
   node["requirements"] = requirements
   node["object"] = n
   
   #@TODO
   #ATTRIBUTES
   #@TODO not supported for now
   #NODE-FILTER
   #@TODO not supported for now
   #@TODO
   #DIRECTIVES
   #@TODO ???
   return node

def getStandardOperationDict(i, arts):
   operation = {}
   # OPERATION NAME
   #operation["name"] = i.name
   # PRIMARY ARTIFACT
   if i.implementation in arts:
      operation["implementation"] = arts[i.implementation]
   # INPUTS
   operation["inputs"] = getInterfaceInput(i.inputs)
   return i.name, operation

def printNodeDict(node):
   starter = "\t"
   starter2 = "\t\t"
   starter3 = "\t\t\t"
   starter4 = "\t\t\t\t"
   print(starter + "NAME: " + node["name"])
   print(starter2 + "TYPE: " + node["type"])
   print(starter2 + "DESCRIPTION: " + node["description"])
   print(starter2 + "HIERARCHY: " + str(node["hierarchy"]))
   print(starter2 + "PROPERTIES: ")
   for k, p in node["properties"].items():
      printPropertyDict(p, starter3)
      print("")
   print(starter2 + "CAPABILITIES: ")
   for k, c in node["capabilities"].items():
      printCapabilityDict(c, starter3, starter4)
   print("")
   print(starter2 + "REQUIREMENTS: ")
   for k,r in node["requirements"].items():
      printRequirementDict(r, starter3)
   #print(starter2 + "RELATIONSHIPS: ")
   #for k,r in node["relationships"].items():
   #   printRelationshipDict(r, starter3, starter4)   

def getArtifactDict(a, repos):
   art = {}
   # ARTIFACT NAME
   art["name"] = a["name"] 
   # ARTIFACT TYPE
   art["type"] = a["type"]
   # ARTIFACT DESCRIPTION
   if "description" in a :
      art["description"] = a["description"]
   else:
      art["description"] = ""   
   # ARTIFACT FILE
   art["file"] = a["file"]
   # ARTIFACT REPOSITORY
   if "repository" in a:
      if a["repository"] in repos:
         art["repository"] = repos[a["repository"]]
      else:
         art["repository"] = { "url": a["repository"] }
   else:
      art["repository"] = {}
   # ARTIFACT DEPLOY_PATH
   if "deploy_path" in a:
      art["deploy_path"] = a["deploy_path"]
   else:
      art["deploy_path"] = ""
   return art


def getPropertyDict(p_def):
   prop = {}
   #PROPERTY NAME
   prop["name"] = p_def.name
   #PROPERTY SCHEMA
   #prop["schema"] = p.schema
   #PROPERTY TYPE
   prop["type"] = p_def.schema["type"]
   if ".datatypes." in p_def.schema["type"]:
      datatype = getDatatypeDict(p_def.schema["type"], prop["name"])
      prop["type_schema"] = datatype   
      #prop["type_schema"][""]
   #PROPERTY DESCRIPTION
   if "description" in p_def.schema:
      prop["description"] = p_def.schema["description"]
   else:
      prop["description"] = ""
   #PROPERTY isREQUIRED
   prop["required"] = p_def.required
   #PROPERTY DEFAULT
   prop["default"] = p_def.default
   #PROPERTY VALUE CONSTRAINTS 
   if "constraints" in p_def.schema:
      prop["constraints"] = p_def.schema["constraints"]
   #@TODO
   #ENTRY_SCHEMA
   #@TODO
   
   #PROPERTY VALUE
   #@NOTE: we check if the object has attribute "value" because we use this function to parse inputs too
   if hasattr(p_def, "value") and p_def.value:
      prop["value"] =  p_def.value
   else:
      prop["value"] = None    
   return prop

def printPropertyDict(prop, starter):
   starter2 = starter + "\t"
   print(starter + "NAME: " + prop["name"])
   print(starter + "TYPE: " + prop["type"])
   if "type_schema" in prop:
      print(starter + "TYPE_SCHEMA: ")
      printDataTypeDict(prop["type_schema"], starter2)
   print(starter + "DESCRIPTION: " + prop["description"])
   print(starter + "REQUIRED: " + str(prop["required"]))
   print(starter + "DEFAULT: " + str(prop["default"]))
   print(starter + "VALUE: " + str(prop["value"]))
   if "value_linked_to" in prop:
      #print(starter + "LINKED_TO: " + prop["value_linked_to"]["target_type"] + "( " + prop["value_linked_to"]["target_name"] + " )")
      print("PRINT value_linked_to @TODO")
   if "constraints" in prop:
      print(starter + "CONSTRAINTS: " + str(prop["constraints"]))
   #@TODO entry_schema

def getAttributeDict(a_def):
   attr = {}
   #ATTRIBUTE NAME
   attr["name"] = a_def.name
   #ATTRIBUTE SCHEMA
   #prop["schema"] = p.schema
   #ATTRIBUTE TYPE
   attr["type"] = a_def.schema["type"]
   if ".datatypes." in a_def.schema["type"]:
      datatype = getDatatypeDict(a_def.schema["type"], attr["name"])
      attr["type_schema"] = datatype   
      #prop["type_schema"][""]
   #ATTRIBUTE DESCRIPTION
   if "description" in a_def.schema:
      attr["description"] = a_def.schema["description"]
   else:
      attr["description"] = ""
   #ATTRIBUTE DEFAULT
   #prop["default"] = a_def.default    -> does not exist in AttributeDef object?!?
   # ATTRIBUTE VALUE
   attr["value"] = None
   return attr

def getDatatypeDict(type_name, base_name):
   definition = EntityType.TOSCA_DEF[type_name]
   datatype = {}
   #VERSION @TODO - not supported
   if "description" in definition:
      datatype["description"] = definition["description"]
   else:
      datatype["description"] = None
   datatype["properties"] = {}
   datatype["hierarchy"] = [type_name]
   if "properties" in definition:
      for p,v in definition["properties"].items():
        property_def = PropertyDef(p, None, v)
        prop = getPropertyDict(property_def)
        datatype["properties"][base_name + "." + prop["name"]] = prop
      tmp_definition = definition
      while "derived_from" in tmp_definition:
         datatype["hierarchy"].append(tmp_definition["derived_from"])
         tmp_definition = EntityType.TOSCA_DEF[tmp_definition["derived_from"]]
         if "properties" in tmp_definition:
            for p,v in tmp_definition["properties"].items():
               property_def = PropertyDef(p, None, v)
               prop = getPropertyDict(property_def)
               datatype["properties"][base_name + "." + prop["name"]] = prop
   if not datatype["properties"]: #if no properties is defined, type must be defined. It should not happen thou...
      datatype["type"] = definition["type"]
      if "constraints" in definition:
         datatype["constraints"] = definition["constraints"]
   return datatype

def printDataTypeDict(datatype, starter):
   print(starter + "DESCRIPTION: " + str(datatype["description"]))
   if "type" in datatype:
      print(starter + "TYPE: " + datatype["type"])
      if "constraints" in datatype:
         print(starter + "CONSTRAINTS: " + str(datatype["constraints"]))
   else:
      print(starter + "HIERARCHY: " + str(datatype["hierarchy"]))
      if "properties" in datatype:
         starter2 = starter + "\t"
         print(starter + "PROPERTIES: ")
         for p,v in datatype["properties"].items():
            printPropertyDict(v, starter2)
            print()

def resolveGetInput(function):
   result = {}
   value = function.result()
   if type(value) is tuple:
      result["value_linked_to"] = (value[0], value[1])
      result["target_value"] = value[2]   
   return result
   
def resolveGetAttribute(function):
   result = {}
   value = function.result()
   target = value[1]
   if len(target) == 3:
      node = target[0]
      capability = target[1]
      attribute = target[2].name
      result["value_linked_to"] = (value[0], (node, capability, attribute))
   else:
      node = target[0]
      attribute = target[1]
      result["value_linked_to"] = (value[0], (node, attribute))
   result["value"] = None
   return result
   
def resolveGetProperty(function):

   result = {}
   value = function.result()

   result["value_linked_to"] = (value[0], value[1])
   linked_to = None
   value = value[2]
   while type(value) is tuple:
      linked_to = (value[0], value[1])
      value = value[2]
   
   if linked_to and linked_to[0] == "input":
      result["target_value"] = (linked_to[0], linked_to[1], value)
   else:
      result["target_value"] = value
   return result

def getCapabilityDict(c_def):
   # -> definition: + attributes + valid_source_types + occurrences
   cap = {}
   #NAME
   cap["name"] = c_def.name
   #TYPE
   cap["type"] = c_def.type
   #DESCRIPTION
   #if "description" in c_def.schema:
   #   prop["description"] = c_def.schema["description"]
   if hasattr(c_def, "defs") and "description" in c_def.defs :
      cap["description"] = c_def.defs["description"]
   else:
      cap["description"] = ""
   #PROPERTIES
   properties = {}
   for p in c_def.get_properties_def_objects() :
         prop = getPropertyDict(p)
         properties[prop["name"]] = prop
   cap["properties"] = properties
   #HIERARCHY
   hierarchy = []
   hierarchy.append(cap["type"])
   t = c_def.parent_type
   while t :
      hierarchy.append(t.type)
      #add properties derived from the parent capability type
      for p in t.get_properties_def_objects():
         prop = getPropertyDict(p)
         #if property was re-defined by upper class, ignore it
         if not prop["name"] in cap["properties"]:
            cap["properties"][prop["name"]] = prop
         #@TODO constraints should be summed, not ignored!
      t = t.parent_type
   cap["hierarchy"] = hierarchy 
   #ATTRIBUTES - not fully supported
   attributes = {}
   for a in c_def.get_attributes_def_objects() :
     attr = getAttributeDict(a)
     attributes[attr["name"]] = attr
   cap["attributes"] = attributes
   #VALID_SOURCE_TYPES
   #@TODO not supported for now
   #OCCURRENCES 
   if hasattr(c_def, "defs") and "occurrences" in c_def.defs :
      cap["occurrences"] = c_def.defs["occurrences"]
   else:
      cap["occurrences"] = ["1", "UNBOUNDED"]
   #else:
   #   node["occurrences"]
   return cap
   
def printCapabilityDict(c, starter, starter2):
   print(starter + "NAME: " + c["name"])
   print(starter + "TYPE: " + c["type"])
   print(starter + "DESCRIPTION: " + c["description"])
   print(starter + "HIERARCHY: " + str(c["hierarchy"]))
   print(starter + "OCCURRENCES: " + str(c["occurrences"]))
   print(starter + "PROPERTIES: ")
   for k, p in c["properties"].items():
      printPropertyDict(p, starter2)
      print("")
   
def getRequirementTypeDict(req):
   requirement = {}
   for r,v in req.items():
      # NAME
      requirement["name"] = r
      # CAPABILITY -> desired target capability type
      requirement["capability"] = v["capability"]
      # NODE -> desired target node type
      if "node" in v:
         requirement["node"] = v["node"]
      else:
         requirement["node"] = None
      # RELATIONSHIP -> expected relationship type
      requirement["relationship"] = v["relationship"]
      # OCCURRENCES
      if "occurrences" in v:
         requirement["occurrences"] = v["occurrences"]
      else:
         requirement["occurrences"] = ["1", "1"]
      #NODE_FILTER @TODO allows custom selection by the TOSCA Orchestrator
   return requirement
   
def printRequirementDict(r, starter):
      print(starter + "NAME: " + r["name"])
      print(starter + "SELECTED: " + str(r["selected"]))
      print(starter + "OCCURRENCES: " + str(r["occurrences"]))
      print(starter + "CAPABILITY: " + r["capability"])
      print(starter + "NODE: " + str(r["node"]))
      print(starter + "RELATIONSHIP: " + r["relationship"])
      print("")

def getExplicitRelationshipDict(t, tosca):
   #print(type(t))  #Relationship Template ! (e ho bisogno di type)
   relationship = {}
   #NAME
   relationship["name"] = t.name
   #TYPE
   relationship["type"] = t.type_definition.type
   #DESCRIPTION
   if "description" in t.entity_tpl:
      relationship["description"] = t.entity_tpl["description"]
   else:
      relationship["description"] = ""
   #PROPERTIES
   relationship["properties"] = {}
   rel_type = t.type_definition
   for p in rel_type.get_properties_def_objects():
      prop = getPropertyDict(p)
      relationship["properties"][prop["name"]] = prop
   #HIERARCHY
   hierarchy = []
   hierarchy.append(relationship["type"])
   r = t.type_definition.parent_type      
   while r :
      hierarchy.append(r.type)
      #add properties derived from the parent node type
      for p in r.get_properties_def_objects():
         prop = getPropertyDict(p)
         #if property was re-defined by upper class, ignore it
         if not prop["name"] in relationship["properties"]:
            relationship["properties"][prop["name"]] = prop
         #@TODO constraints should be summed, not ignored!
      r = r.parent_type
   relationship["hierarchy"] = hierarchy
   # Update all properties with values if they are defined in the template!
   # @TODO relationships properties could be linked via get_attribute to target capability attributes!! and to get_property too!
   for p in t.get_properties_objects():
      if p.name in relationship["properties"]:
         if isinstance(p.value, GetInput):
            result = resolveGetInput(p.value)
            relationship["properties"][p.name]["value_linked_to"] = result
            relationship["properties"][p.name]["value"] = result["target_value"]
         elif isinstance(p.value, dict) and "get_input" in p.value:
            input_name = p.value["get_input"]
            input_obj = GetInput(tosca, None, None, [input_name])
            result = resolveGetInput(input_obj)
            relationship["properties"][p.name]["value_linked_to"] = result
            relationship["properties"][p.name]["value"] = result["target_value"]
         else:
            relationship["properties"][p.name]["value"] = p.value
         
   #SOURCE
   relationship["source"] = t.source.name
   #TARGET
   relationship["target"] = t.target.name
   return relationship
   # INTERFACES @TODO  -> template & type
   # ATTRIBUTES @TODO  -> template & type
   # Valid_target_types -> type @TODO

def printRelationshipDict(r):
   starter = "\t"
   starter2 = "\t\t"
   
      
   print(starter + "NAME: " + r["name"])
   print(starter + "TYPE: " + r["type"])
   print(starter + "SOURCE: " + str(r["source"]))
   print(starter + "TARGET: " + str(r["target"]))
   print(starter + "DESCRIPTION: " + str(r["description"]))
   print(starter + "HIERARCHY: " + str(r["hierarchy"]))
   print(starter + "PROPERTIES: ")
   for k, p in r["properties"].items():
      printPropertyDict(p, starter2)
      print("")
      
def getInputDict(inp):
   prop = getPropertyDict(inp)
   #NOTE: field "value" will always be None since the current model do not support "value" for inputs in TOSCA Template!
   return prop

def getInterfaceInput(if_inputs):
   inputs = {}
   if if_inputs:
     for inp, value in if_inputs.items():
       if isinstance(value, GetInput):
          result = resolveGetInput(value)
          inputs[inp] = {"value_linked_to" : result["value_linked_to"], "value" : result["target_value"]}
       elif isinstance(value, GetProperty):
          result = resolveGetProperty(value)
          inputs[inp] = {"value_linked_to" : result["value_linked_to"], "value" : result["target_value"]}
       elif isinstance(value, GetAttribute):
          result = resolveGetAttribute(value)
          inputs[inp] = {"value_linked_to" : result["value_linked_to"], "value" : result["value"]}
       else:
          inputs[inp] = {"value" : str(value)}
   return inputs

def dump(obj):
  for attr in dir(obj):
    print("obj.%s = %r" % (attr, getattr(obj, attr)))
