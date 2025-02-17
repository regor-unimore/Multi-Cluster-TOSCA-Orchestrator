package it.unict.bpmn4tosca.delegate.packages;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

public class PackageHandler implements JavaDelegate {

	@Override
	public void execute(DelegateExecution arg0) {
		// TODO Auto-generated method stub
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
