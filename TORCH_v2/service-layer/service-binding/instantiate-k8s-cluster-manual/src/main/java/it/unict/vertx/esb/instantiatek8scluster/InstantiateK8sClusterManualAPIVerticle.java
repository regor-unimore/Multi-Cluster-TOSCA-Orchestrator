package it.unict.vertx.esb.instantiatek8scluster;


import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.credentials.ClientCertificateAuthentication;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import it.unict.vertx.esb.resource.InstantiateCluster;
import java.util.Base64;

public class InstantiateK8sClusterManualAPIVerticle extends AbstractVerticle implements InstantiateCluster {
		
	private String ApiServerUri;
	private String KeyFilePath, CertFilePath, CAFilePath;
	private Map<String, Map<String, String>> clusters;

	@Override
	  public void start(Future<Void> future) throws Exception {
		super.start();
		
		// Inizializzazione
		//ApiServerUri = config().getString("apiserver.uri");

		// Authentication
		//CAFilePath =  System.getProperty("user.dir") + config().getString("ca.cert.file");
		//KeyFilePath =  System.getProperty("user.dir") + config().getString("user.key.file");
		//CertFilePath =  System.getProperty("user.dir") + config().getString("cert.file");
		
		clusters = new HashMap< String, Map<String, String>>();
		
		Router router = Router.router(vertx);
		router.route().handler(BodyHandler.create());	 
		 
		// API per creare istanze e controllarne lo stato
		router.post("/clusters").handler(this::createCluster);
		router.get("/clusters/:id").handler(this::checkCluster);
		 
		vertx.createHttpServer().requestHandler(router::accept)
			.listen(config().getInteger("http.port"), ar -> {
	         if (ar.succeeded()) {
	        	 System.out.println("Cluster started");
	         } else {
	        	 System.out.println("Cannot start the cluster: " + ar.cause());
	         }
	      });
		
	}
		
	@Override
	public void createCluster(RoutingContext routingContext) {
		// Recupero dei parametri della richiesta
		JsonObject requestBody = routingContext.getBodyAsJson();
		String name = requestBody.getString("name");
		String template = requestBody.getString("template");
		JsonObject nodeProperties = requestBody.getJsonObject("nodeProperties");
		String ca = nodeProperties.getJsonObject("ca").getString("crt");
		String crt = nodeProperties.getString("user_crt");
		String key = nodeProperties.getString("private_key");
		String endpoint = nodeProperties.getString("api-endpoint");
		String path = nodeProperties.getString("path");
		if(path == null){
		   path = System.getProperty("user.dir") + "/src/main/resources/";
		}
		//update the clusters map
		HashMap configs = new HashMap();
		configs.put("CAFilePath", path + ca);
		configs.put("KeyFilePath", path + key);
		configs.put("CertFilePath", path + crt);
		configs.put("Endpoint", endpoint);
		configs.put("Namespace", template);
		clusters.put(name, configs);
		
		
		//System.out.println("createCluster - request body is: " + requestBody);
		
		if (nodeProperties.getString("platform").equals("swarm")) return;
		
		JsonObject responseBody = new JsonObject();
		JsonObject nodeAttrs = new JsonObject();
		JsonObject capAttrs = new JsonObject();
		JsonObject host = new JsonObject();
		int responseCode = 200;
		

		//id could be the name of the cluster as indicated in tosca.nodes.Container.Runtime!
		nodeAttrs.put("id", name);
		
		//properties are also attributes in TOSCA, for now i add only the properties whose value can be useful to external connectors
		nodeAttrs.put("platform", nodeProperties.getString("platform"));
		nodeAttrs.put("provider", nodeProperties.getString("provider"));
		
        //add host attributes
		//host.put("api-endpoint", endpoint); -> sent in checkCluster!
		host.put("ca_file", path + ca);
		host.put("private_key_file", path + key);
		host.put("crt_file", path + crt);
		host.put("platform", nodeProperties.getString("platform"));
		

		capAttrs.put("host", host);
		responseBody.put("nodeAttributes", nodeAttrs);
		responseBody.put("capAttributes", capAttrs);
        
		//System.out.println("createCluster - response is " + responseBody.encode());
		routingContext.response()
	      .setStatusCode(responseCode)
	      .putHeader("content-type", "application/json; charset=utf-8")
	      .end(responseBody.encode());
		
	}

	@Override
	public void checkCluster(RoutingContext routingContext) {
		// Recupero dei parametri della richiesta
		String id = routingContext.request().getParam("id");
		//System.out.println("checkCluster - received request with id: " + id);
		InstantiateCluster.Status mappedStatus = InstantiateCluster.Status.OK;
		JsonObject responseBody = new JsonObject();
		int responseCode = 200;

		JsonObject nodeAttrs = new JsonObject();
		JsonObject capAttrs = new JsonObject();
		JsonObject host = new JsonObject();
		
		try {
			byte[] clusterKey = Files.readAllBytes(Paths.get(clusters.get(id).get("KeyFilePath")));
			byte[] userCert = Files.readAllBytes(Paths.get(clusters.get(id).get("CertFilePath")));
			byte[] clusterCa = Files.readAllBytes(Paths.get(clusters.get(id).get("CAFilePath")));
			//byte[] cert = Files.readAllBytes(Paths.get(CertFilePath));
			//byte[] ca = Files.readAllBytes(Paths.get(CAFilePath));
		    
			// create namespace
			//byte[] key = Base64.getDecoder().decode(clusterKey);
		    //byte[] cert = Base64.getDecoder().decode(userCert);
			//byte[] ca = Base64.getDecoder().decode(clusterCa);
			String clusterEndpoint = clusters.get(id).get("Endpoint");
			String namespace = clusters.get(id).get("Namespace");

			ApiClient client = ClientBuilder.standard()
				.setBasePath(clusterEndpoint)
				.setAuthentication(new ClientCertificateAuthentication(userCert, clusterKey))
				.setVerifyingSsl(true)
				.setCertificateAuthority(clusterCa)
				.build();

			Configuration.setDefaultApiClient(client);
			CoreV1Api api = new CoreV1Api();
			Double startTime = System.currentTimeMillis() / 1000.0;
			V1Namespace ns = api.createNamespace(new V1Namespace().metadata(new V1ObjectMeta().name(namespace)), null, null, null, null);
			Double passedTime = (System.currentTimeMillis() / 1000.0) - startTime;
			//System.out.println("@TIMELOG " + namespace + " TECH " + id + " " + passedTime);
			//System.out.println("created namespace " + namespace + "\n" + ns.toString());
			

			//responseBody.put("endpoint", ApiServerUri);
			host.put("endpoint", clusters.get(id).get("Endpoint"));
			// CERTIFICATES
			host.put("ca", Base64.getEncoder().encodeToString(clusterCa));
			host.put("cert", Base64.getEncoder().encodeToString(userCert));
			host.put("key", Base64.getEncoder().encodeToString(clusterKey));
			host.put("namespace", namespace);
			
		} catch (ApiException ae) {
			ae.printStackTrace();
			System.out.println("message is :");
			System.out.println(ae.getMessage());
			System.out.println("response is :");
			System.out.println(ae.getResponseBody());
			responseBody.put("message", "ERROR checkCluster()");
			responseCode = 500;	
			mappedStatus = InstantiateCluster.Status.ERROR;
		} catch (Exception e) {
			
			e.printStackTrace();
			responseBody.put("message", "ERROR checkCluster()");
			responseCode = 500;	
			mappedStatus = InstantiateCluster.Status.ERROR;
		}
        
		capAttrs.put("host", host);
		responseBody.put("capAttributes", capAttrs);
		nodeAttrs.put("status", mappedStatus );
		responseBody.put("nodeAttributes", nodeAttrs);
		//System.out.println("checkCluster - response body is: " + responseBody.encode());
		routingContext.response()
	      .setStatusCode(responseCode)
	      .putHeader("content-type", "application/json; charset=utf-8")
	      .end(responseBody.encode());
				
	}


}
