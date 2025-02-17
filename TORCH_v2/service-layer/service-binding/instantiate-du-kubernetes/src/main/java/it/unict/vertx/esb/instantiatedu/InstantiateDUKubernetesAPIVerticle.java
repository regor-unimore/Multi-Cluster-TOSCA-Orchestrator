package it.unict.vertx.esb.instantiatedu;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
//import io.kubernetes.client.ApiClient;
//import io.kubernetes.client.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
//import io.kubernetes.client.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
//import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
//import io.kubernetes.client.models.*;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Yaml;
import io.kubernetes.client.util.credentials.ClientCertificateAuthentication;
import io.kubernetes.client.openapi.ApiException;
//import io.kubernetes.client.ApiException;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import it.unict.vertx.esb.common.Translator;
import it.unict.vertx.esb.du.InstantiateDU;

public class InstantiateDUKubernetesAPIVerticle extends AbstractVerticle implements InstantiateDU {

	@Override
	public void start(Future<Void> future) throws Exception {
		super.start();

		Router router = Router.router(vertx);
		router.route().handler(BodyHandler.create());

		// API per creare DU, controllarne lo stato e configurarle
		router.post("/dus/create").handler(this::createDU);
		router.post("/dus/check").handler(this::checkDU);
		router.post("/dus/configure").handler(this::configureDU);
		router.post("/dus/start").handler(this::startDU);

		vertx.createHttpServer().requestHandler(router::accept)
				.listen(config().getInteger("http.port"), ar -> {
					if (ar.succeeded()) {
						System.out.println("Server started");
					} else {
						System.out.println("Cannot start the server: " + ar.cause());
					}
				});
		
		//boolean sockProxy = config().getBoolean("socks.proxy");
		//if (sockProxy) {
			// Proxy settings
		//	System.getProperties().put("proxySet", config().getString("socks.proxy.set"));
	        //System.getProperties().put("socksProxyHost", config().getString("socks.proxy.host"));
	        //System.getProperties().put("socksProxyPort", config().getString("socks.proxy.port"));
		//}
		
	}

	@Override
	public void createDU(RoutingContext routingContext) {

		//NON UTILIZZATA!
		
		System.out.println("ERROR: NON dovresti mai accedere a CREATE DU con il nuovo BPMN workflow... come hai fatto???");
		// Recupero dei parametri della richiesta
		JsonObject requestBody = routingContext.getBodyAsJson();
		System.out.println("CreateDU - request body is: " + requestBody.encode());

		//We do nothing and return error as this operation is not yet implemented...
		int responseCode = 501;
		routingContext.response()
				.setStatusCode(responseCode).end();

	}

	@Override
	public void configureDU(RoutingContext routingContext) {

		// Recupero dei parametri della richiesta e print delle informazioni ottenute
		JsonObject requestBody = routingContext.getBodyAsJson();
		JsonObject node = requestBody.getJsonObject("node");
		JsonObject configureInputs = requestBody.getJsonObject("configureInputs");
		//System.out.println("configureDU in instantiateDUKubernetes");
		//System.out.println("NODE is " + node.encode());
		
		JsonObject responseBody = new JsonObject();
		int responseCode = 200;

		//*************************************/
		//2) @TODO CONFIGURE DU specific logic
		//*************************************/
		// 2.1) Parse useful configureInputs parameters
		if (configureInputs != null) {
			System.out.println("configureInputs are " + configureInputs.encode());
			//@TODO
		}
		
		try {
			//2.3) generate the Deployment and Services objects
			String kube_objs = Translator.translate(node.encode(), K8sTranslatorPlugin.class);
			//System.out.println("YAML created object are " + kube_objs);

			//2.4) restituisci NodeAttributes, CapAttributes e startInputs!!!! @TODO
			//    -> per ora nessun attributo è raccoglibile
			JsonObject startInputs = new JsonObject();
			//startInputs.put( "id", node.getString("name") + "-ID");  SERVE???
			startInputs.put("kubernetes-objects", kube_objs);
			responseBody.put("startInputs", startInputs);
			responseBody.put("status", "OK");
		
		} catch(ApiException ae ){
			ae.printStackTrace();
			responseBody.put("message", "ERROR create&configureDU()");
			responseBody.put("status", "ERROR");
			responseBody.put("details", "Response Body from kubernetes was:\n" + ae.getResponseBody());
			responseCode = 500;		
		
		} catch(Exception e) {
			e.printStackTrace();
			responseBody.put("status", "ERROR");
			responseBody.put("message", "Errore nella creazione e configurazione della DU");
			responseBody.put("details", "Couldn't create and configure DU.");
			responseCode = 500;
		}

		//*************************************/
		//*************************************/

		routingContext.response()
				.setStatusCode(responseCode)
				.putHeader("content-type", "application/json; charset=utf-8")
				.end(responseBody.encode());

		
	}

	@Override
	public void startDU(RoutingContext routingContext){
		JsonObject responseBody = new JsonObject();
		int responseCode = 202;

		//1) PARSE REQUEST PARAMETERS
		// Recupero dei parametri della richiesta e print delle informazioni ottenute
		JsonObject requestBody = routingContext.getBodyAsJson();
		JsonObject node = requestBody.getJsonObject("node");
		JsonObject host = node.getJsonObject("reqProperties").getJsonObject("location");
		String clusterEndpoint = host.getString("endpoint");
		String clusterCert = (String) host.getString("cert");
		String clusterKey = host.getString("key");
		String clusterCa = host.getString("ca");
		

		JsonObject startInputs = requestBody.getJsonObject("startInputs");
		//System.out.println("startDU in instantiateDUKubernetes");
		//System.out.println("NODE is " + node.encode());
		if (startInputs == null || startInputs.getString("kubernetes-objects") == null) {
			System.out.println("ERROR: required startInputs parameters not found!");
			responseBody.put("status", "ERROR");
			responseBody.put("message", "Errore nell'avvio' della DU");
			responseBody.put("details", "startInputs.kubernetes-objects is required but was not found in the request body!");
			responseCode = 500;
		}

		String kubernetes_objects = startInputs.getString("kubernetes-objects");
		Boolean isRetry = startInputs.getBoolean("isRetry");

		// parse KubernetesTypes
		List<V1Deployment> deployments = new ArrayList<>();
		List<V1Service> services = new ArrayList<>();
		
		//*************************************/
		//2) @TODO START DU specific logic
		//*************************************/
		try {
			List<Object> objs = Yaml.loadAll(kubernetes_objects);
			if (isRetry != null && isRetry == true) deleteDu(objs);
			
			for (Object o: objs) {
				if (o.getClass() == V1Deployment.class) {
					deployments.add((V1Deployment) o);
					//System.out.println("FOUND deployment");
					
				}
				if (o.getClass() == V1Service.class){
					services.add( (V1Service)o);
					//System.out.println("FOUND service");
				}
			}

			JsonObject nodeAttrs = new JsonObject();
			JsonObject capAttrs = new JsonObject();

			JsonObject checkInputs = new JsonObject();

			byte[] key = Base64.getDecoder().decode(clusterKey.getBytes("UTF-8"));
		    byte[] cert = Base64.getDecoder().decode(clusterCert.getBytes("UTF-8"));
			byte[] ca = Base64.getDecoder().decode(clusterCa.getBytes("UTF-8"));

			ApiClient client = ClientBuilder.standard()
				.setBasePath(clusterEndpoint)
				.setAuthentication(new ClientCertificateAuthentication(cert, key))
				.setVerifyingSsl(true)
				.setCertificateAuthority(ca)
				.build();

			Configuration.setDefaultApiClient(client);

			

			//System.out.println("Kubernetes client setted");
			AppsV1Api appsApi = new AppsV1Api();
			CoreV1Api coreApi = new CoreV1Api();
			List<String> deployment_names = new ArrayList<>();
			List<String> service_names = new ArrayList<>();

			Double startTime;
			Double passedTime = 0.0;
			for(V1Deployment d : deployments){
				String namespace = d.getMetadata().getNamespace();
				if (namespace == null) namespace = "default";
				startTime = System.currentTimeMillis() / 1000.0;
				V1Deployment deployed = appsApi.createNamespacedDeployment(namespace, d, null,null,null, null);
				passedTime = passedTime + ((System.currentTimeMillis() / 1000.0) - startTime);
				updateAttributes(deployed, node, capAttrs, nodeAttrs);
				deployment_names.add(deployed.getMetadata().getName());
				//System.out.println("DEPLOYED deployment " + deployed.getMetadata().getName());
			}
			for(V1Service s : services){
				String namespace = s.getMetadata().getNamespace();
				if (namespace == null) namespace = "default";
				startTime = System.currentTimeMillis() / 1000.0;
				V1Service deployed =  coreApi.createNamespacedService(namespace,s, null ,null,null, null);
				passedTime = passedTime + ((System.currentTimeMillis() / 1000.0) - startTime);
				updateAttributes(deployed, node, capAttrs, nodeAttrs);
				service_names.add(deployed.getMetadata().getName());
				//System.out.println("DEPLOYED service " + deployed.getMetadata().getName());
			}
			System.out.println("@TIMELOG " + node.getString("template") + " TECH " + node.getString("name") + " " + passedTime);
			checkInputs.put("deployments", deployment_names);
			checkInputs.put("services", service_names);

			String status = "OK";
		
			if(nodeAttrs.containsKey("id")){
				checkInputs.put( "id", nodeAttrs.getValue("id"));
			} else {
				checkInputs.put( "id", node.getString("name") + "-ID");
			}
		
			//System.out.println("START DU completed :)");
			responseBody.put("checkInputs", checkInputs);
			responseBody.put("nodeAttributes", nodeAttrs);
			responseBody.put("capAttributes", capAttrs);

			if(status.equals("ERROR")) {
				responseBody.put("status", InstantiateDU.Status.ERROR.value());
				responseBody.put("message", "Errore nell'avvio della DU");
				responseBody.put("details", "bla bla bla");
				responseCode = 500;
			} else {
				responseBody.put("status", InstantiateDU.Status.OK.value());
				responseBody.put("message", "Avvio della DU iniziato!");
			}

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			responseBody.put("status", "ERROR");
			responseBody.put("message", "Error on DU Start");
			responseBody.put("details", "Couldn't parse cluster credentials received in inputs, reason: " + e.getMessage());
			responseCode = 500;
			
		} catch (IOException e) {
			e.printStackTrace();
			responseBody.put("status", "ERROR");
			responseBody.put("message", "Error on DU Start - IOException: " + e.getMessage());
			responseCode = 500;

		} catch (ApiException e) {
			e.printStackTrace();
			responseBody.put("status", "ERROR");
			responseBody.put("message", "Error on DU Start");
			System.out.println("message is :");
			System.out.println(e.getMessage());
			System.out.println("response is :");
			System.out.println(e.getResponseBody());
			responseBody.put("details", "Error while trying to deploy or undeploy a kubernetes object, reason: " + e.getResponseBody());
			responseCode = 500;
		} catch (Exception e) {
			e.printStackTrace();
			responseBody.put("status", "ERROR");
			responseBody.put("message", "Error on DU Start " + e.getMessage());
			responseCode = 500;
		}

		//*************************************/
		//*************************************/

		//3) send the response 
		

		routingContext.response()
				.setStatusCode(responseCode)
				.putHeader("content-type", "application/json; charset=utf-8")
				.end(responseBody.encode());
	}

	private void updateAttributes(V1Deployment deployed, JsonObject node, JsonObject capAttrs,
			JsonObject nodeAttrs) {
		// Generate attributes from current state of Kuberentes Deployment Object
		// NODE_ATTR:  ID e REPLICAS

		//@NOTA supponiamo che ci sia un solo oggetto Deployment
		//  In caso di più deployment bisognoerebbe estrarre  i dati
		//  qui sotto SOLO dal Deployment con il nome pari a
		//  'nome_nodo'-deployment in quanto è quello che contiene l'effettiva
		//  logica del nodo... gli altri deployments saranno di supporto e legati alle
		//  capabilities immagino...
		nodeAttrs.put("id", deployed.getMetadata().getUid());
		nodeAttrs.put("name", deployed.getMetadata().getName());
		nodeAttrs.put("replicas", deployed.getSpec().getReplicas());

		//System.out.println("GENERATED Node attributes :\n\t" + nodeAttrs.encode());
		
		//@TODO altro?
	}

	private void updateAttributes(V1Service deployed, JsonObject node, JsonObject capAttrs,
	JsonObject nodeAttrs) {
		// Generate attributes from current state of Kuberentes Service Object
		// CAP_ATTR: HOSTNAME, PORT, BASE_URL, IP-ADDR
		JsonObject capabilities = node.getJsonObject("capProperties");
		Map<String, Object> caps = capabilities.getMap();
		for(V1ServicePort port : deployed.getSpec().getPorts()){
			for(Map.Entry<String, Object> c : caps.entrySet()){
				Map<String, Object> props = (Map<String, Object>) c.getValue();
				String type = (String) props.get("type");
				switch(type){
					case "torch.capabilities.Endpoint.Http":
						Integer cap_port = (Integer) props.get("port");
						if(cap_port.equals(port.getPort())){
							JsonObject cap = new JsonObject();
							cap.put("port", port.getPort());
							cap.put("endpoint", props.get("url_path"));
							cap.put("ip_addr", deployed.getSpec().getClusterIP());
							cap.put("domain_name", deployed.getMetadata().getName());

							if(deployed.getSpec().getType().equals("NodePort")){
								cap.put("public_port", port.getNodePort());
								cap.put("public_ip_addrs", "@TODO ip di uno dei nodi del cluster");
								String public_hostname = deployed.getSpec().getExternalName();
								if(public_hostname == null){
									//System.out.println("External Name is null! using cluster name isntead");
									String cluster_endpoint = node.getJsonObject("reqProperties").getJsonObject("location").getString("endpoint");
									String cluster_name = cluster_endpoint.split(":")[1].replaceAll("/", "");
									cap.put("public_domain_name", cluster_name);
								} else {
									cap.put("public_domain_name", deployed.getSpec().getExternalName());
								}	
							}
							capAttrs.put(c.getKey(), cap);
							//System.out.println("GENERATED Cap attributes for cap " + c.getKey() + ":\n\t" + nodeAttrs.encode());
						}
						break;
				}
			}
	
		}
		

	}

	@Override
	public void checkDU(RoutingContext routingContext) {
		//1) PARSE REQUEST PARAMETERS
		// Recupero dei parametri della richiesta e print delle informazioni ottenute
		JsonObject requestBody = routingContext.getBodyAsJson();
		JsonObject node = requestBody.getJsonObject("node");
		JsonObject checkInputs = requestBody.getJsonObject("checkInputs");
		JsonObject location = node.getJsonObject("reqProperties").getJsonObject("location");
		String namespace = location.getString("namespace");
		if (namespace == null) namespace = "default";
		//System.out.println("checkDU in instantiateDUKubernetes");
		//System.out.println("NODE is " + node.encode());
		
		JsonObject responseBody = new JsonObject();
		int responseCode = 200;
		
		
		if (checkInputs != null) {
			//System.out.println("checkInputs are " + checkInputs.encode());
			
			List<String> deploymentNames = checkInputs.getJsonArray("deployments").getList();
			List<String> serviceNames = checkInputs.getJsonArray("services").getList();

			//extract the credentials to access the cluster
			JsonObject host = node.getJsonObject("reqProperties").getJsonObject("location");
			String clusterEndpoint = host.getString("endpoint");
			String clusterCert = (String) host.getString("cert");
			String clusterKey = host.getString("key");
			String clusterCa = host.getString("ca");

			//*************************************/
			//2) @TODO CHECK DU specific logic
			//*************************************/

			try {
				byte[] key = Base64.getDecoder().decode(clusterKey.getBytes("UTF-8"));
		    	byte[] cert = Base64.getDecoder().decode(clusterCert.getBytes("UTF-8"));
				byte[] ca = Base64.getDecoder().decode(clusterCa.getBytes("UTF-8"));

				ApiClient client = ClientBuilder.standard()
				.setBasePath(clusterEndpoint)
				.setAuthentication(new ClientCertificateAuthentication(cert, key))
				.setVerifyingSsl(true)
				.setCertificateAuthority(ca)
				.build();

				Configuration.setDefaultApiClient(client);
				CoreV1Api coreApi = new CoreV1Api();
				AppsV1Api appsApi = new AppsV1Api();

				JsonObject nodeAttrs = new JsonObject();
			    JsonObject capAttrs = new JsonObject();

				JsonObject deployments_status = new JsonObject(); 
				JsonObject services_status = new JsonObject(); 
				JsonObject deployment_errors = new JsonObject();
				JsonObject service_errors = new JsonObject();

				InstantiateDU.Status status = InstantiateDU.Status.OK; //= InstantiateDU.Status.UNRECOGNIZED;
				//InstantiateDU.Status status = InstantiateDU.Status.UNRECOGNIZED;

				for(String d : deploymentNames){
					V1Deployment deployed = appsApi.readNamespacedDeployment(d, namespace, null);
					if (deployed.getStatus() == null || deployed.getStatus().getConditions() == null || deployed.getStatus().getConditions().isEmpty()) {
						// Kubernetes needs time: WORK IN PROGRESS
						deployments_status.put(d, InstantiateDU.Status.WIP); 
						status = updateStatus(status, InstantiateDU.Status.WIP);

					} else {
						updateAttributes(deployed, node, capAttrs, nodeAttrs);
						List<V1DeploymentCondition> conditions = deployed.getStatus().getConditions();
						//System.out.println(d + " conditions are: " + conditions.toString());
						
						for( V1DeploymentCondition dc : conditions){
							if(dc.getType().equals("Available") && dc.getStatus().equals("True")){ //AVAILABLE
								status = updateStatus(status, InstantiateDU.Status.OK);
								deployments_status.put(d, InstantiateDU.Status.OK);
								break;
							} 
							else if (dc.getType().equals("Progressing") && dc.getStatus().equals("True")){ //PROGRESSING
								status = updateStatus(status, InstantiateDU.Status.WIP);
								deployments_status.put(d, InstantiateDU.Status.WIP);
								break;
							} else if (dc.getType().equals("ReplicaFailure") && dc.getStatus().equals("True")){ //REPLICAFAILURE
								status = updateStatus(status, InstantiateDU.Status.ERROR);
								deployments_status.put(d, InstantiateDU.Status.ERROR);
								JsonObject error = new JsonObject();
								error.put("reason", dc.getReason());
								error.put("message", dc.getMessage());
								deployment_errors.put(d, error);
								break;
							}
							// there are other conditions that can happens, but we do not consider them
						}
					}					
				}

				for(String s : serviceNames){
					V1Service deployed = coreApi.readNamespacedService(s, namespace, null);
					if (deployed.getStatus() == null) {
						// Kubernetes needs time: WORK IN PROGRESS
						services_status.put(s, InstantiateDU.Status.WIP);
						status = updateStatus(status, InstantiateDU.Status.WIP);
					} else {
						//you cannot use conditions with Service! They are often empty...
						// to check if a service is "ready" we can check if an ip was assigned to it
						// but for now, let's suppose it is alwasy ready :)
						status = updateStatus(status, InstantiateDU.Status.OK);
						services_status.put(s, InstantiateDU.Status.OK);
					}	
				}

				JsonObject checkOutputs = new JsonObject();
				responseBody.put("status", status.value());
				responseBody.put("checkInputs", checkInputs);
			    responseBody.put("nodeAttributes", nodeAttrs);
			    responseBody.put("capAttributes", capAttrs);
				checkOutputs.put("deployments_status", deployments_status);
				checkOutputs.put("services_status", services_status);
				checkOutputs.put("service_errors", service_errors);
				checkOutputs.put("deployment_errors", deployment_errors);
				responseBody.put("checkOutputs", checkOutputs);
			} catch(UnsupportedEncodingException e){
				e.printStackTrace();
				responseBody.put("status", "ERROR");
				responseBody.put("message", "Error on DU Check");
				responseBody.put("details", "Couldn't parse cluster credentials received in inputs, reason: " + e.getMessage());
				responseCode = 500;
			} catch (ApiException e) {
				e.printStackTrace();
				responseBody.put("status", "ERROR");
				responseBody.put("message", "Error on DU Check");
				System.out.println("message is :");
				System.out.println(e.getMessage());
				System.out.println("response is :");
				System.out.println(e.getResponseBody());
				responseBody.put("details", "Error while trying to read a kubernetes object, reason: " + e.getResponseBody());
				responseCode = 500;
			} catch (IOException e) {
				e.printStackTrace();
				responseBody.put("status", "ERROR");
				responseBody.put("message", "Error on DU Start - IOException: " + e.getMessage());
				responseCode = 500;
			}
		} else {
			System.out.println("ERROR: required startInputs parameters not found!");
			responseBody.put("status", "ERROR");
			responseBody.put("message", "Errore nell'avvio' della DU");
			responseBody.put("details", "startInputs.kubernetes-objects is required but was not found in the request body!");
			responseCode = 500;
		}
		
		//*************************************/
		//*************************************/

		routingContext.response()
				.setStatusCode(responseCode)
				.putHeader("content-type", "application/json; charset=utf-8")
				.end(responseBody.encode());
	}


	private Status updateStatus(Status oldStatus, Status newStatus) {
		
		if(oldStatus.equals(Status.ERROR)){
			return oldStatus;
		}
		else if (oldStatus.equals(Status.UNRECOGNIZED)) {
			return oldStatus;
		}
		else if (oldStatus.equals(Status.WIP)){
			if(newStatus.equals(Status.ERROR) || newStatus.equals(Status.UNRECOGNIZED)){
				return newStatus;
			} else {
				return oldStatus;
			}
		}
		else {
			return newStatus;
		}
	}


	private void deleteDu(List<Object> objs) throws ApiException, Exception
	{
		List<V1Deployment> deployments = new ArrayList<>();
		List<V1Service> services = new ArrayList<>();
		
		for (Object o: objs) {
			if (o.getClass() == V1Deployment.class) {
				deployments.add((V1Deployment) o);
				//System.out.println("FOUND deployment");
				
			}
			if (o.getClass() == V1Service.class){
				services.add( (V1Service)o);
				//System.out.println("FOUND service");
			}
		}

		AppsV1Api appsApi = new AppsV1Api();
		CoreV1Api coreApi = new CoreV1Api();

		for(V1Service s : services){
			String namespace = s.getMetadata().getNamespace();
			if (namespace == null) namespace = "default";
			V1Service deleted = coreApi.deleteNamespacedService(s.getMetadata().getName(), namespace,
			null, null, null, null, null, null);
			//System.out.println("service status is: " + deleted.getStatus().toString());
				if (!deleted.getStatus().toString().equals("Success")){
					System.out.println("@DEBUG ERROR: deleted service status is different from Success, it is " + deleted.getStatus().toString());
					throw new Exception();
				} 
		}
		
		for(V1Deployment d : deployments){
			String namespace = d.getMetadata().getNamespace();
			if (namespace == null) namespace = "default";
			V1Status status = appsApi.deleteNamespacedDeployment(d.getMetadata().getName(), namespace,
						null,null,null, null, null, null);
				if (!status.getStatus().equals("Success")) throw new Exception();
		}
	}

}
