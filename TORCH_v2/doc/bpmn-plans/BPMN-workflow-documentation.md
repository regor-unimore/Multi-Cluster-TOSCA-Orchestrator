# Code documentation for TORCH_v2 BPMN workflows

TORCH_v2 has own main BPMN workflow for each major lifecycle operation.

## Deployment

<br/>
@TODO
<center><img src="IMG/BPMN4TOSCA-overall.png"  alt="Image Alt Text"></center>

 <br/><br/> 
  
### BPMN workflow to Deploy Resources:
<br/>
<center><img src="IMG/CREATESERVICE.png"  alt="Image Alt Text"></center><br/>
  
Cloud Resources usually are available after being created and the configuration is usually executed at the creation step, so in this case the BPMN workflow do not make use of the "start" or the "configured" operation of the Standard interface. A Resource that is observed (by 'check') to be created pass directly from the CREATING state to the AVAILABLE/STARTED state.
   
More detail on the CREATESERVICE workflow can be found [here](bpmn_createservice.md).
<br/><br/>

### BPMN workflow to Deploy DUs:
<br/>
<center><img src="IMG/CREATEDEPLOYMENTUNIT.png"  alt="Image Alt Text"></center>
<br/><br/>

### BPMN workflow to Deploy Packages:
<br/>
<center><img src="IMG/DEPLOYPACKAGE.png"  alt="Image Alt Text"></center><br/>
  
@TODO
<br/><br/>
