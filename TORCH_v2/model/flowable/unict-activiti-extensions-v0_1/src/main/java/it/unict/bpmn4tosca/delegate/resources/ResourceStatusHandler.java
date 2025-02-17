package it.unict.bpmn4tosca.delegate.resources;

import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

public class ResourceStatusHandler implements JavaDelegate {

	@Override
	public void execute(DelegateExecution execution){
		// TODO Auto-generated method stub
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		boolean error = Math.random() < .4;
//		if(error) {
//			System.out.println("BPMN error: deleted");
//			throw new BpmnError("deleted");
//		}
//		else {
//			String result = Math.random() < .1 ? "wip" : "ok";
			
			String result = "ok";
			System.out.println("Result (retry = " + execution.getVariable("retryCounter")+"): " + result);
			execution.setVariable("status", result);

//			System.out.println("BPMN error: deleted");
//			throw new BpmnError("deleted", "Throw a BPMN error");	
			
//		}
	}

}
