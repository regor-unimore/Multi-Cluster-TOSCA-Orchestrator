
from toscaparser.torch_handlers.properties_handler import PropertiesHandler
#from toscaparser import template_printer

class RequirementsHandler(object):

   def __init__(self, processed_tosca):
      self.prop_handler = PropertiesHandler(processed_tosca)
   
   #requirements do not have properties, but relationships created to satisfy the requirements can have properties!!
   '''
     returned object:
        [relationship_type]
        [properties]
       
   '''
   def getRequirementFeature(self, req, rel, source_node, target_node):
      #the type of action that we need to perform is tied to the Relationship Type not to the Requirement nor Capability Type
      feature = {}
      feature["relationship_type"] = rel["type"]
      feature["linked_capability_name"] = None
      if "capability" in rel["target"]:
         feature["linked_capability_name"] = rel["target"]["capability"]
      elif "implicit_capability" in rel["target"]:
         feature["linked_capability_name"] = rel["target"]["implicit_capability"]
      feature["properties"] = {}
      for p,v in rel["properties"].items():
            value, linked_input = self.prop_handler.getProperty(v)
            # if value is None and no input is associated with the properties, it can be ignored
            if value is None and linked_input is None:
               continue
            feature["properties"][v["name"]] = value
            #if linked_input:
            #   if linked_input in tosca_processed["inputs"].keys():
            #      if not linked_input in du["inputs"]:
            #         du["inputs"]["linked_targets"] = [node["name"] + "." + prop["name"]]
            #      else:
            #         du["inputs"]["linked_targets"].append(node["name"] + "." + prop["name"])
      '''
      if "tosca.relationships.HostedOn" in rel["hierarchy"]:
         return self.__getHostedOnRelationshipProps(req, rel, source_node, target_node)
      elif "tosca.relationships.ConnectsTo" in rel["hierarchy"]:
         return self.__getConnectsToRelationshipProps(req, rel, source_node, target_node) #@TODO
      else:
         return {}
      '''
      return feature
   
   def __getHostedOnRelationshipProps(self, req, rel, source_node, target_node):
      host = {}
      host["type"] = "host"
      if "torch.nodes.Location" in target_node["hierarchy"]:
         host.update(self.__getLocationRequirement(req, rel, source_node, target_node))
      return host
   '''      
   def __getLocationRequirement(self, req, rel, source_node, target_node):
      location = {}
      location["subtype"] = "location"
      if target_node["type"] == "torch.nodes.Location.Cluster":
         location["level"] = "Cluster"
         location["properties"] = {}
         for p,v in target_node["properties"].items():
            value, linked_input = self.prop_handler.getProperty(v)
            # if value is None and no input is associated with the properties, it can be ignored
            if value is None and linked_input is None:
               continue
            location["properties"][v["name"]] = value
            if linked_input:
               if linked_input in tosca_processed["inputs"].keys():
                  if not linked_input in du["inputs"]:
                     du["inputs"]["linked_targets"] = [node["name"] + "." + prop["name"]]
                  else:
                     du["inputs"]["linked_targets"].append(node["name"] + "." + prop["name"])
         location["properties"].update(rel["properties"])
      return location
   '''
      
   def __getConnectsToRequirement(self, req, rel, source_node, target_node):
      cap = None
      if "capability" in rel["target"]:
         cap = rel["target"]["capability"]
      elif "implicit_capability" in rel["target"]:
         cap = rel["target"]["implicit_capability"]
      


