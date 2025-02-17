package it.unict.bpmn4tosca.delegate.resources;

import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;

import it.unict.bpmn4tosca.model.Node;

public class ResourceHandler implements JavaDelegate {

	@Override
	public void execute(DelegateExecution execution) {
		// TODO Auto-generated method stub
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		ExecutionEntity executionEntity = (ExecutionEntity) execution;
//		ExecutionEntity processInstance = executionEntity.getProcessInstance();
//		ExecutionEntity superExecution = processInstance.getSuperExecution();
//
//		if(superExecution != null) {
//		  String superProcessInstanceId = superExecution.getProcessInstanceId();
//		  System.out.println("Process instance ID: " + processInstance.getProcessInstanceId() + 
//				  "\tParent process instance ID: " + superExecution.getProcessInstanceId());
//		}
//		
//		Node node = (Node)execution.getVariable("node");
//		
//		if(node.getName().equals("appserver")) {
//			System.out.println("BPMN error: deleted");
//			throw new BpmnError("deleted", "Throw a BPMN error");	
//		}

	}

}
