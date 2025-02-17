

#from toscaparser import template_printer

class CapabilitiesHandler(object):

   def __init__(self, *args, **kwargs):
      super().__init__(*args, **kwargs)
   
   def getCapabilityFeature(self, cap, node):
      #the type of action that we need to perform is tied to the Capability Type of the node
      if "tosca.capabilities.Endpoint" in cap["hierarchy"]:
         return self.__getEndpointFeature(cap, node)
      elif "tosca.capabilities.Container" in cap["hierarchy"]:
         return self.__getContainerFeature(cap, node)
      else:
         return {}
   
   def __getEndpointFeature(self, cap, node):
      endpoint = {}
      endpoint["type"] = "endpoint"
      endpoint["protocol"] = cap["properties"]["protocol"]["value"]
      endpoint["port"] = cap["properties"]["port"]["value"]  #or ["type_schema"]["value"]?
      endpoint["network_name"] = cap["properties"]["network_name"]["value"]
      endpoint["secure"] = cap["properties"]["secure"]["value"]
      return endpoint
   
   def __getContainerFeature(self, cap, node):
      container = {}
      container["type"] = "container"
      return container   
         
      


