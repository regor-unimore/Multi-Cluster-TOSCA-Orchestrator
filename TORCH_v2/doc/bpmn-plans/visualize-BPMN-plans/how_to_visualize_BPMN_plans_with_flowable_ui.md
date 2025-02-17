# How can I visualize TORCH_v2 BPMN workflows?

To visualize TORCH_v2 BPMN workflows you can use any BPMN 2.0 compliant editor like [Camunda](https://camunda.com/download/modeler/) or [FlowableUI](https://www.flowable.com/open-source-code). Since we use Flowable 6.6.0 to generate our BPMN workflows we suggest the use of Flowable UI. 

### visualizing using FLOWABLE-UI 6.8.0

1) Start the flowable-ui container  
   `cd $BASE_DIR/doc/bpmn-plans/visualize-BPMN-plans`  
   `docker compose -f docker-compose-flowable-ui.yaml` 
2) Access the flowable ui webpage at "localhost:8081/flowable-ui"  
3) user is "admin", password is "test"  
3) Use the "modeler" app to upload a BPMN Plan (XML file) and display it. You can find the XML files with TORCH_v2 BPMN workflow in the folder *$BASE_DIR/bpmn-plans/bpmn_plans_v2_1*  
