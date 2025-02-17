package it.unibo.vertx.esb.instantiatedu;

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

import io.kubernetes.client.common.KubernetesObject;
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
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.servicediscovery.types.HttpEndpoint;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.ext.web.handler.BodyHandler;
import it.unict.vertx.esb.common.Translator;
import it.unict.vertx.esb.du.InstantiateDU;

public class InstantiateDULiqoAPIVerticle extends AbstractVerticle implements InstantiateDU {

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
		
		System.out.println("ERROR: 'createDU' not implemented");
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
		//System.out.println("LIQO CONFIGURE DU - received request body is:\n" + requestBody.encode());
		JsonObject node = requestBody.getJsonObject("node");
		JsonObject configureInputs = requestBody.getJsonObject("configureInputs");
		//System.out.println("configureDU in instantiateDUKubernetes");
		//System.out.println("NODE is " + node.encode());
		
		

		//*************************************/
		//CONFIGURE DU specific logic
		//*************************************/
		// 1) Parse useful configureInputs parameters if they exist
		if (configureInputs != null) {
			//System.out.println("configureInputs are " + configureInputs.encode());
			//@TODO
		}
			
		/*      
		"relationships":{
         "location":{
            "cap_type":"torch.capabilities.Location.Container",
            "rel_type":"tosca.relationships.HostedOn",
            "capability":"host",
            "target":"federation"
         }
      	}, 
	    */
		
		// 2) pass the request to the Kubernetes Connector to get a first translation of the node in Kubernetes
		// --2.1) prepare requestBody to send to the Service Broker
		String oldCategory = requestBody.getJsonObject("node").getString("category");
		requestBody.getJsonObject("node").put("category", "kubernetes-app");
		JsonObject replicas = node.getJsonObject("reqProperties").getJsonObject("location").getJsonObject("replicas");
		requestBody.getJsonObject("node").getJsonObject("reqProperties").getJsonObject("location").remove("replicas");

		// --2.2) send the request to the Service Broker
		HttpClientOptions options = new HttpClientOptions().setDefaultPort(9000).setDefaultHost("localhost");
		HttpClient client = vertx.createHttpClient(options);

		makeHttpRequest(client,requestBody, "/dus/configure").setHandler(fr -> {
			JsonObject responseBody = new JsonObject();
			int responseCode = 200;
			String namespace = node.getJsonObject("reqProperties").getJsonObject("location").getString("namespace");
			
			
			if(fr.succeeded()){
				JsonObject resp = fr.result();
				//System.out.println("Kubernetes createDU responded with:\n" + resp.encode());
				//System.out.println("status code was ");

				// parse response
				
				String kubernetesObjects = resp.getJsonObject("body").getJsonObject("startInputs").getString("kubernetes-objects");
				try {
					List<Object> objs = Yaml.loadAll(kubernetesObjects);
					List<KubernetesObject> result = new ArrayList<>();
					for (Object o: objs) {
						if (o.getClass() == V1Deployment.class) {
							//System.out.println("FOUND deployment");
							V1Deployment dep = (V1Deployment) o;
							
							//System.out.println("@@@@@@@@ name is " + dep.getMetadata().getName());
							String app = dep.getMetadata().getLabels().get("app");
							//System.out.println("@@@@@@@ app name is " + app);
							//System.out.println("@@@@@@@@ namespace is " + dep.getMetadata().getNamespace());
							//System.out.println("@@@@@@@@ nodeAffinity is " + dep.getMetadata().getName());
	
							dep.getMetadata().setNamespace(namespace);


							if (app.equals(node.getString("name")) && replicas != null){ //it's the MAIN deployment
								//System.out.println("Configuring position of the Pods inside the Federation...");
								String entrypoint = node.getJsonObject("reqProperties").getJsonObject("location").getString("entrypoint");
								for(Map.Entry<String,Object>cluster : replicas) {
									//deep copy of the Object using serialization
									String tmp = Yaml.dump(o);
									V1Deployment d = (V1Deployment) Yaml.load(tmp);
									V1NodeSelectorTerm selector = new V1NodeSelectorTerm();
									//update deployment
									if(cluster.getKey().equals(entrypoint)){
										//add antiaffinity for liqo.io/type=virtual-node 
										selector.addMatchExpressionsItem(new V1NodeSelectorRequirement().key("liqo.io/type").operator("NotIn").addValuesItem("virtual-node"));
									} else {
										//add affinity for <context>-<federation>=<cluster>
										selector.addMatchExpressionsItem(new V1NodeSelectorRequirement().key(namespace).operator("In").addValuesItem(cluster.getKey()));
										String newName = d.getMetadata().getName() + "-" + cluster.getKey();
										d.getMetadata().setName(newName);
									}
									V1NodeAffinity na = new V1NodeAffinity()
															.requiredDuringSchedulingIgnoredDuringExecution(new V1NodeSelector()
																												.addNodeSelectorTermsItem(selector));
									V1Affinity affinity = new V1Affinity().nodeAffinity(na);
									d.getSpec().getTemplate().getSpec().setAffinity(affinity);
									d.getSpec().setReplicas((Integer) cluster.getValue());
									result.add(d);
								}
							} else {
								//System.out.println("NO CHANGES FOR DEPLOYMENT " + dep.getMetadata().getName());
								result.add(dep);
							}				
						}
						if (o.getClass() == V1Service.class){
							//System.out.println("FOUND service");
							V1Service s = (V1Service)o;
							//System.out.println("@@@@@@@@ name is " + s.getMetadata().getName());
							s.getMetadata().setNamespace(namespace);
							result.add(s);
						}
					}
					
					String serialized = Yaml.dumpAll(result.iterator());
					//System.out.println("LIQO CONFIGURE DU - YAML kuberentes objects are:\n" + serialized);
					responseBody.put("startInputs", new JsonObject().put("kubernetes-objects", serialized));
				} catch(IOException e){
					e.printStackTrace();
					responseBody.put("status", "ERROR");
					responseBody.put("message", "Error on DU Configure - IOException: " + e.getMessage());
					responseCode = 500;
				}
				
				// completare gli oggetti kubernetes

				// restituire una risposta al client originale
				responseBody.put("status", "OK");
				routingContext.response()
					.setStatusCode(responseCode)
					.putHeader("content-type", "application/json; charset=utf-8")
					.end(responseBody.encode());
			}
			else {
				System.out.println("failed to request a createDu to the Kuberentes Connector");
				responseBody.put("status", "ERROR");
				responseBody.put("message", "ERROR in requesting configure operation to the kubernetes connector");
				responseCode = 500;	
				//responseBody.put("details", "Response Body was:\n" + ae.getResponseBody());
				routingContext.response()
					.setStatusCode(responseCode)
					.putHeader("content-type", "application/json; charset=utf-8")
					.end(responseBody.encode());
			}
		});

		//recover old Category
		//requestBody.getJsonObject("node").put("category", oldCategory);

			// 4) stampa gli oggetti che hai ottenuto



			/* 
			//2.3) generate the Deployment and Services objects
			String kube_objs = Translator.translate(node.encode(), K8sTranslatorPlugin.class);
			System.out.println("YAML created object are " + kube_objs);

			//2.4) restituisci NodeAttributes, CapAttributes e startInputs!!!! @TODO
			//    -> per ora nessun attributo è raccoglibile
			JsonObject startInputs = new JsonObject();
			//startInputs.put( "id", node.getString("name") + "-ID");  SERVE???
			startInputs.put("kubernetes-objects", kube_objs);
			responseBody.put("startInputs", startInputs);
			*/
			//responseBody.put("status", "OK");
		
		//*************************************/
		//*************************************/

		//routingContext.response()
		//		.setStatusCode(responseCode)
		//		.putHeader("content-type", "application/json; charset=utf-8")
		//		.end(responseBody.encode());

		
	}

    private Future<JsonObject> makeHttpRequest(HttpClient client, JsonObject requestBody, String uri) {
        Future<JsonObject> future = Future.future();

		client.post( uri, response -> {
			response.exceptionHandler(future::fail); /* Eccezione sulla response da gestire */
			JsonObject resp = new JsonObject();
			resp.put("status-code", response.statusCode());
			resp.put("status-message", response.statusMessage());
			response.bodyHandler(buffer -> {
				try {
					JsonObject body = buffer.toJsonObject();
					resp.put("body", body);
					future.complete(resp);
				} catch(Exception e){
					future.fail(e);
				}
		    });
		})
		.exceptionHandler(future::fail) /* Eccezione sulla request da gestire */
		.putHeader("content-type", "application/json; charset=utf-8")
		.end(requestBody.encode());
        return future;
    }


	@Override
	public void startDU(RoutingContext routingContext){
		
		//1) PARSE REQUEST PARAMETERS
		// Recupero dei parametri della richiesta e print delle informazioni ottenute
		JsonObject requestBody = routingContext.getBodyAsJson();
		//System.out.println("LIQO START DU - received request body is:\n" + requestBody.encode());

		// 2) USE KUBERNETES CONNECTOR TO DEPLOY THE NODE
		requestBody.getJsonObject("node").put("category", "kubernetes-app");
		HttpClientOptions options = new HttpClientOptions().setDefaultPort(9000).setDefaultHost("localhost");
		HttpClient client = vertx.createHttpClient(options);

		makeHttpRequest(client,requestBody, "/dus/start").setHandler(fr -> {
			JsonObject responseBody = new JsonObject();
			int responseCode = 202;
			
			if(fr.succeeded()){
				JsonObject resp = fr.result();
				//System.out.println("Kubernetes startDU responded with:\n" + resp.encode());
				//System.out.println("status code was ");
				responseBody = resp.getJsonObject("body");
				responseCode = resp.getInteger("status-code");
				// parse response
				
				
				
				// completare gli oggetti kubernetes

				// restituire una risposta al client originale
				routingContext.response()
					.setStatusCode(responseCode)
					.putHeader("content-type", "application/json; charset=utf-8")
					.end(responseBody.encode());
			}
			else {
				System.out.println("failed to request a createDu to the Kuberentes Connector");
				responseBody.put("status", "ERROR");
				responseBody.put("message", "ERROR in requesting start operation to the kubernetes connector");
				responseCode = 500;	
				//responseBody.put("details", "Response Body was:\n" + ae.getResponseBody());
				routingContext.response()
					.setStatusCode(responseCode)
					.putHeader("content-type", "application/json; charset=utf-8")
					.end(responseBody.encode());
			}
		});
	}

	@Override
	public void checkDU(RoutingContext routingContext) {
		//1) PARSE REQUEST PARAMETERS
		// Recupero dei parametri della richiesta e print delle informazioni ottenute
		JsonObject requestBody = routingContext.getBodyAsJson();
		//System.out.println("LIQO CHECK DU - received request body is:\n" + requestBody.encode());

		// 2) USE KUBERNETES CONNECTOR TO DEPLOY THE NODE
		requestBody.getJsonObject("node").put("category", "kubernetes-app");
		HttpClientOptions options = new HttpClientOptions().setDefaultPort(9000).setDefaultHost("localhost");
		HttpClient client = vertx.createHttpClient(options);

		makeHttpRequest(client,requestBody, "/dus/check").setHandler(fr -> {
			JsonObject responseBody = new JsonObject();
			int responseCode = 200;
			
			if(fr.succeeded()){
				JsonObject resp = fr.result();
				//System.out.println("Kubernetes startDU responded with:\n" + resp.encode());
				//System.out.println("status code was ");
				responseBody = resp.getJsonObject("body");
				responseCode = resp.getInteger("status-code");
				// parse response
				
				
				
				// completare gli oggetti kubernetes

				// restituire una risposta al client originale
				responseBody.put("status", responseBody.getString("status"));
				routingContext.response()
					.setStatusCode(responseCode)
					.putHeader("content-type", "application/json; charset=utf-8")
					.end(responseBody.encode());
			}
			else {
				//System.out.println("failed to request a createDu to the Kuberentes Connector");
				responseBody.put("status", "ERROR");
				responseBody.put("message", "ERROR in requesting check operation to the kubernetes connector");
				responseCode = 500;	
				//responseBody.put("details", "Response Body was:\n" + ae.getResponseBody());
				routingContext.response()
					.setStatusCode(responseCode)
					.putHeader("content-type", "application/json; charset=utf-8")
					.end(responseBody.encode());
			}
		});
	}


	private void deleteDu(List<Object> objs) throws ApiException, Exception
	{
		//@TODO		
		System.out.println("@TODO");
		// da fare insieme a check
	}

}
