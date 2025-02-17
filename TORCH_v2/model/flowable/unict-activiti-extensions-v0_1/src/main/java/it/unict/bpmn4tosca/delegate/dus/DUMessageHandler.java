package it.unict.bpmn4tosca.delegate.dus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.io.*;

import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.runtime.Execution;

public class DUMessageHandler implements JavaDelegate {
	
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
		
		Map<String, Map<String, Object>> globalMap = (Map<String, Map<String, Object>>) execution.getVariable("globalMap");
		
		for (Execution execution2 : executions) {
		        Map<String, Map<String, Object>> personalMap = deepCopy(globalMap);		
			runtimeService.setVariable(execution2.getParentId(), "localMap", personalMap);
			runtimeService.messageEventReceivedAsync("await phase", execution2.getId());
			System.out.println("Parent ID: " + execution2.getParentId() + " Execution ID: " + execution2.getId());
		}
		
		System.out.println("Message: " + messageT + " --> # executions with message event received: " + executions.size());
		
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss:SS");
		System.out.println(sdf.format(new Date()) + "\tBusinessKey: " + businessKey + "\tMessage: " + (String)message.getValue(execution));
	}

    private Map<String, Map<String, Object>> deepCopy(Map<String, Map<String, Object>> originalMap) {
        try {
            // Serialize the originalMap into a byte array
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(originalMap);
            objectOutputStream.flush();

            // Deserialize it back to a new Map
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            return (Map<String, Map<String, Object>>) objectInputStream.readObject();
         } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

}
