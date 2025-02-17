

#from toscaparser import template_printer

class PropertiesHandler(object):

   def __init__(self, processed_tosca):
      self.tosca = processed_tosca
   
   def getProperty(self, prop):
      value = None
      linked_prop = None
      if prop["value"]:
         if type(prop["value"]) is tuple:
            value = prop["value"][2]
            if prop["value"][0] == "input":
               linked_prop = prop["value"][1]
         else:
            value = prop["value"]
      elif "default" in prop and prop["default"]:
         value = prop["default"]
      #TORCH is interested only in links to inputs or attributes, not to properties!
      #  -> The Create/Modify Template is instead interested in links to property as well, but another API endpoint
      #     will be used for it
      if "value_linked_to" in prop:
         linked_prop = prop["value_linked_to"]
      return value, linked_prop
   
   #def getProperties(self, node, tosca_processed):
   #   #the type of action that we need to perform is tied to the Relationship Type not to the Requirement nor Capability Type
   #   if "tosca.relationships.HostedOn" in rel["hierarchy"]:
   #      return self.__getHostedOnRequirement(req, rel, source_node, target_node)
   #   else:
   #      return {}
      
         
      


