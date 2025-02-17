package it.unict.bpmn4tosca.delegate.packages;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.runtime.Execution;

public class PackageMessageHandler implements JavaDelegate {
	
	private Expression message;

	@Override
	public void execute(DelegateExecution execution) {
		String businessKey = execution.getProcessInstanceBusinessKey();
		String messageT = (String)message.getValue(execution);
		RuntimeService runtimeService = Context.getProcessEngineConfiguration().getRuntimeService();
		
		List<Execution> executions = runtimeService.createExecutionQuery()
				.processInstanceBusinessKey(businessKey, true)
				.messageEventSubscriptionName("await phase")
				.processVariableValueEquals("message", messageT).list();
//		  .variableValueEquals("message", (String)message.getValue(execution))
//		  .list();
		
		Map<String, Object> globalMap = (Map<String, Object>) execution.getVariable("globalMap");

		for (Execution execution2 : executions) {
//			Map<String, Object> execShared = (Map<String, Object>) runtimeService.getVariable(execution2.getId(), "shared");
//			if(execShared == null)
//				runtimeService.setVariable(execution2.getId(), "shared", shared);
//			else {
//				for(Map.Entry<String, Object> entry : shared.entrySet())
//					execShared.putIfAbsent(entry.getKey(), entry.getValue());
//				
//				runtimeService.setVariable(execution2.getId(), "shared", execShared);
//			}
			
			runtimeService.setVariable(execution2.getParentId(), "localMap", globalMap);
			runtimeService.messageEventReceivedAsync("await phase", execution2.getId());
//			runtimeService.trigger(execution2.getId());
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss:SS");
		System.out.println(sdf.format(new Date()) + "\tBusinessKey: " + businessKey + "\tMessage: " + (String)message.getValue(execution));

	}

}
