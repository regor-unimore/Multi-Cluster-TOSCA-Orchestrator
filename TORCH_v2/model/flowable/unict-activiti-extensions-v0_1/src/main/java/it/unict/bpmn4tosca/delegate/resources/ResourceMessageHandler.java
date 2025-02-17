package it.unict.bpmn4tosca.delegate.resources;

import it.unict.bpmn4tosca.model.Node;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;
import java.io.*;

import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ExecutionQuery;

public class ResourceMessageHandler implements JavaDelegate {

	private Expression message;

	@Override
	public void execute(DelegateExecution execution) {
		String businessKey = execution.getProcessInstanceBusinessKey();
		String messageT = (String)message.getValue(execution);
		RuntimeService runtimeService = Context.getProcessEngineConfiguration().getRuntimeService();
		
//		ExecutionQuery eq = runtimeService.createExecutionQuery()
//				.processInstanceBusinessKey(businessKey, true);
//
//		List<Execution> pi = eq.list();
//		List<Execution> executions = eq.variableValueEquals("message", messageT).list();
				
		List<Execution> executions = runtimeService.createExecutionQuery()
				.processInstanceBusinessKey(businessKey, true)
				.messageEventSubscriptionName("await phase")
				.processVariableValueEquals("message", messageT).list();
		
//		Node node = (Node) execution.getVariable("node");
//		if(node.getCategory().equals(Node.Category.VM.value())) {
//			Map<String, Object> variables = new HashMap<String, Object>();
//			variables.put("vms", execution.getVariable("vms"));
//			
//			for(Execution execution2 : pi) {
//				runtimeService.setVariables(execution2.getId(), variables);
//			}
//		}
		
		Map<String, Map< String,Map<String, Object>>> globalMap = (Map<String, Map<String, Map<String, Object>>>) execution.getVariable("globalMap");
		
		for (Execution execution2 : executions) {			
//			Map<String, Map<String, Object>> execShared = (Map<String, Map<String, Object>>) runtimeService.getVariable(execution2.getParentId(), "shared");
//			if(execShared == null)
//				runtimeService.setVariable(execution2.getParentId(), "shared", shared);
//			else {
//				for(Map.Entry<String, Map<String, Object>> entry : shared.entrySet())
//					execShared.putIfAbsent(entry.getKey(), entry.getValue());
//				
//				runtimeService.setVariable(execution2.getParentId(), "shared", execShared);
//			}
                        Map<String, Map<String, Map<String, Object>>> personalMap = deepCopy(globalMap);
                        runtimeService.setVariable(execution2.getParentId(), "localMap", personalMap);
			//runtimeService.setVariable(execution2.getParentId(), "localMap", globalMap);
			runtimeService.messageEventReceivedAsync("await phase", execution2.getId());
//			runtimeService.trigger(execution2.getId());
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss:SS");
		System.out.println(sdf.format(new Date()) + "\tBusinessKey: " + businessKey + "\tMessage: " + (String)message.getValue(execution));
		System.out.println(System.currentTimeMillis() / 1000.0);

	}
    
    private Map<String, Map<String, Map<String, Object>>> deepCopy(Map<String, Map<String, Map<String, Object>>> originalMap) {
        try {
            // Serialize the originalMap into a byte array
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(originalMap);
            objectOutputStream.flush();

            // Deserialize it back to a new Map
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            return (Map<String, Map<String, Map<String, Object>>>) objectInputStream.readObject();
         } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

}
