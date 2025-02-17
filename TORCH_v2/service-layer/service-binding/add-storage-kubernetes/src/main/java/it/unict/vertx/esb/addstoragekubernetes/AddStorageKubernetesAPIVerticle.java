package it.unict.vertx.esb.addstoragekubernetes;


import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import it.unict.vertx.esb.resource.AddStorage;
import java.util.Base64;

public class AddStorageKubernetesAPIVerticle extends AbstractVerticle implements AddStorage {

	@Override
	  public void start(Future<Void> future) throws Exception {
		super.start();
		
		Router router = Router.router(vertx);
		router.route().handler(BodyHandler.create());	 
		 
		// API per creare istanze e controllarne lo stato
		router.post("/resources").handler(this::createStorage);
		router.get("/resources/:id").handler(this::checkStorage);
		 
		vertx.createHttpServer().requestHandler(router::accept)
			.listen(config().getInteger("http.port"), ar -> {
	         if (ar.succeeded()) {
	        	 System.out.println("Storage started");
	         } else {
	        	 System.out.println("Cannot start the storage: " + ar.cause());
	         }
	      });
		
	}
		
	@Override
	public void createStorage(RoutingContext routingContext) {
		// Recupero dei parametri della richiesta
		System.out.println("INSIDE CREATE STORAGE");
		JsonObject requestBody = routingContext.getBodyAsJson();
		System.out.println("create Storage with request:\n " + requestBody.encode());
		
		String name = requestBody.getString("name");
		JsonObject nodeProperties = requestBody.getJsonObject("nodeProperties");

		// Installa lo Storage System
		// in questo caso usiamo i meccanismi di default di Kuberentes, quindi 
		//    non c'e' nulla da installare

		// Restituisci gli attributi
		JsonObject responseBody = new JsonObject();
		JsonObject nodeAttrs = new JsonObject();
		JsonObject capAttrs = new JsonObject();
		JsonObject service = new JsonObject();
		int responseCode = 200;
		
		//id could be the name of the cluster as indicated in tosca.nodes.Container.Runtime!
		nodeAttrs.put("id", name);
		
        //add service attributes
		//host.put("api-endpoint", endpoint); -> sent in checkCluster!
		service.put("provider", nodeProperties.getString("provider"));
		service.put("platform", nodeProperties.getString("platform"));

		capAttrs.put("service", service);
		responseBody.put("nodeAttributes", nodeAttrs);
		responseBody.put("capAttributes", capAttrs);
        
		System.out.println("createStorage - response is " + responseBody.encode());
		System.out.println("END OF CREATE STORAGE");
		routingContext.response()
	      .setStatusCode(responseCode)
	      .putHeader("content-type", "application/json; charset=utf-8")
	      .end(responseBody.encode());
		
	}

	@Override
	public void checkStorage(RoutingContext routingContext) {
		System.out.println("INSIDE CHECK STORAGE");
		//Visto che non abbiamo installato nulla possiamo restituire subito Ok
		
		// restituisci risposta
		AddStorage.Status mappedStatus = AddStorage.Status.OK;
		JsonObject responseBody = new JsonObject();
		JsonObject nodeAttrs = new JsonObject();
		int responseCode = 200;


		nodeAttrs.put("status", mappedStatus );
		responseBody.put("nodeAttributes", nodeAttrs);
		System.out.println("checkStorage - response body is: " + responseBody.encode());
		System.out.println("END OF CREATE STORAGE");
		routingContext.response()
	      .setStatusCode(responseCode)
	      .putHeader("content-type", "application/json; charset=utf-8")
	      .end(responseBody.encode());
				
	}


}
