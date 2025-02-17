package it.unict.vertx.esb.broker;

import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.servicediscovery.types.HttpEndpoint;
import it.unict.vertx.esb.common.MicroServiceVerticle;
import it.unict.vertx.esb.packet.configure.ConfigureSC;
import it.unict.vertx.esb.packet.configure.ConfigureWA;
import it.unict.vertx.esb.packet.start.StartDB;
import it.unict.vertx.esb.packet.start.StartDBMS;
import it.unict.vertx.esb.packet.start.StartSC;
import it.unict.vertx.esb.packet.start.StartWA;
import it.unict.vertx.esb.packet.start.StartWS;

public class ServiceBrokerAPIVerticle extends MicroServiceVerticle {
//	private HttpClient instantiateVM, addStorage;
//	private HttpClient createDBMS, configureDBMS;
//	private HttpClient createDB, configureDB;
//	private HttpClient createWS, configureWS;
//	private HttpClient createWA, configureWA;
//	private HttpClient createSC;
	
	private Map<String, HttpClient> instantiateDU, instantiateLocation;
	private Map<String, HttpClient> instantiateVM, addStorage, InstantiateVolume;
	private Map<String, Map<String, HttpClient>> create, configure;
	
	private String discoveryFilePath;
	private int statusCode;
	
	@Override
	  public void start(Future<Void> future) throws Exception {
		super.start();
		
		// Inizializzazione		
		discoveryFilePath = System.getProperty("user.dir") + config().getString("discovery.file");		
		
		discoverServices(future);
		
		Router router = Router.router(vertx);
		router.route().handler(BodyHandler.create());
		 
//		 // Inizializzazione della sessione (in-memory e basata su cookie)
//		 router.route().handler(CookieHandler.create());
//		 router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
		 		 
		 // API per creare risorse e controllarne lo stato
		 router.post("/resources/create").handler(this::createResource);
		 router.post("/resources/check").handler(this::checkResource);

		 // API per creare deployment unit, controllarne lo stato e configurarle
		 router.post("/dus/create").handler(this::createDU);
		 router.post("/dus/check").handler(this::checkDU);
		 router.post("/dus/configure").handler(this::configureDU);
		 router.post("/dus/start").handler(this::startDU);
		 
		 // API per creare, configurare e avviare pacchetti software
		 router.post("/packets/create").handler(this::createPacket);
		 router.post("/packets/configure").handler(this::configurePacket);
		 router.post("/packets/start").handler(this::startPacket);
		 
		 vertx.createHttpServer().requestHandler(router::accept)
		 	.listen(config().getInteger("http.port"), ar -> {
	          if (ar.succeeded()) {
	            System.out.println("Server started");
	          } else {
	            System.out.println("Cannot start the server: " + ar.cause());
	          }
	        });
	}
	
	private void discoverServices(Future<Void> future) {	
		
		try {
			Reader reader = Files.newBufferedReader(Paths.get(discoveryFilePath));
			ObjectMapper objectMapper = new ObjectMapper();
			
			JsonNode tree = objectMapper.readTree(reader);
			if (tree.isArray()) {				
				instantiateLocation = new HashMap<String, HttpClient>();
				instantiateDU = new HashMap<String, HttpClient>();
				instantiateVM = new HashMap<String, HttpClient>();
				addStorage = new HashMap<String, HttpClient>();
				InstantiateVolume = new HashMap<String, HttpClient>();
				
				create = new HashMap<String, Map<String, HttpClient>>();
				configure = new HashMap<String, Map<String, HttpClient>>();				
				
				for (JsonNode node : tree) {
					final String category = node.get("category").asText();
					final String name = node.get("name").asText();
					final String type = node.get("type").asText();
					
					switch(type) {
						case "resource": {
							switch(category) {
								case "vm":
									// Discovery del servizio InstantiateVM
									HttpEndpoint.getClient(
										discovery,
										new JsonObject().put("name", "instantiate-vm-" + name),
										client -> {
											if (client.failed()) {
												future.fail("InstantiateVM-" + name + " discovery failed: " + client.cause());
											} else {
												instantiateVM.put(name, client.result());
												System.out.println("InstantiateVM-" + name + " discovery succeeded");
											}
										});
									break;
								case "storage":
									// Discovery del servizio AddStorage
									HttpEndpoint.getClient(
										discovery,
										new JsonObject().put("name", "add-storage-" + name),
										client -> {
											if (client.failed()) {
												future.fail("AddStorage-" + name + " discovery failed: "
														+ client.cause());
											} else {
												addStorage.put(name, client.result());
												System.out.println("AddStorage-" + name + " discovery succeeded");
											}
										});							
									break;
								case "volume":
									// Discovery del servizio AddStorage
									HttpEndpoint.getClient(
										discovery,
										new JsonObject().put("name", "instantiate-volume-" + name),
										client -> {
											if (client.failed()) {
												future.fail("InstantiateVolume-" + name + " discovery failed: "
														+ client.cause());
											} else {
												InstantiateVolume.put(name, client.result());
												System.out.println("InstantiateVolume-" + name + " discovery succeeded");
											}
										});							
									break;
								case "cluster":
									// Discovery del servizio InstantiateLocation
									HttpEndpoint.getClient(
										discovery,
										new JsonObject().put("name", "instantiate-cluster-" + name),
										client -> {
											if (client.failed()) {
												future.fail("InstantiateLocation-" + name + " discovery failed: " + client.cause());
											} else {
												instantiateLocation.put(name, client.result());
												System.out.println("InstantiateLocation-" + name + " discovery succeeded");
											}
										});
									break;
								case "federation":
									// Discovery del servizio InstantiateLocation
									HttpEndpoint.getClient(
										discovery,
										new JsonObject().put("name", "instantiate-federation-" + name),
										client -> {
											if (client.failed()) {
												future.fail("InstantiateLocation-" + name + " discovery failed: " + client.cause());
											} else {
												instantiateLocation.put(name, client.result());
												System.out.println("InstantiateLocation-" + name + " discovery succeeded");
											}
										});
									break;
								default:
									break;
							}
							break;
						}
						case "package":						
							// Discovery del servizio Create Package
							HttpEndpoint.getClient(
								discovery,
								new JsonObject().put("name", "create-" + category + "-" + name),
								client -> {
									if (client.failed()) {
										future.fail("Create-" + category + "-" + name + " discovery failed: "
												+ client.cause());
									} else {
										if(create.isEmpty() || !create.containsKey(category)) {
											Map<String, HttpClient> createMap = new HashMap<String, HttpClient>();
											createMap.put(name, client.result());
											create.put(category, createMap);
										} else {
											Map<String, HttpClient> createMap = create.get(category);
											createMap.put(name, client.result());
										}
										System.out.println("Create-" + category + "-" + name + " discovery succeeded");
									}
								});
				
							// Discovery del servizio Configure Package
							HttpEndpoint
								.getClient(
										discovery,
										new JsonObject().put("name", "configure-" + category + "-" + name),
										client -> {
											if (client.failed()) {
												future.fail("Configure-" + category + "-" + name + " discovery failed: "
														+ client.cause());
											} else {
												if(configure.isEmpty() || !configure.containsKey(category)) {
													Map<String, HttpClient> configureMap = new HashMap<String, HttpClient>();
													configureMap.put(name, client.result());
													configure.put(category, configureMap);
												} else {
													Map<String, HttpClient> configureMap = configure.get(category);
													configureMap.put(name, client.result());
												}			
												System.out.println("Configure-" + category + "-" + name + " discovery succeeded");
											}
										});						
							break;
						case "du":
							// Discovery del servizio InstantiateDU
							final String fName = name;
							HttpEndpoint.getClient(
								discovery,
								new JsonObject().put("name", "instantiate-du-" + fName),
								client -> {
									if (client.failed()) {
										future.fail("InstantiateDU-" + fName + " discovery failed: " + client.cause());
									} else {
										instantiateDU.put(fName, client.result());
										System.out.println("InstantiateDU-" + fName + " discovery succeeded");
									}
								});						
							break;
						default:
					}						
				}
				
			}			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void createResource(RoutingContext routingContext) {
		Future<String> future = Future.future();
		//System.out.println("Inside createResource!!! ...");
		JsonObject requestBody = routingContext.getBodyAsJson();
		//System.out.println("requestBody of create Resource is " + requestBody.toString());
		
		//extract info
		String name = requestBody.getString("name");
		String category = requestBody.getString("category");
		String template = requestBody.getString("template");
		JsonObject nodeProperties = requestBody.getJsonObject("nodeProperties");
		JsonObject reqProperties = requestBody.getJsonObject("reqProperties");
		JsonObject capProperties = requestBody.getJsonObject("capProperties");
		JsonObject relationships = requestBody.getJsonObject("relationships");
//		String businessKey = requestBody.getString("businessKey");
		//String platform = properties.getString("platform");
		//String provider = properties.getString("provider");

		if(name == null || category == null || nodeProperties == null)
			// Se i parametri sono vuoti, allora viene restituita una Bad Request
			routingContext.response().setStatusCode(400).end();
		
//		// TODO: Recupero della sessione. Aspetti da chiarire:
//		// 1) la sessione e' veramente utile per la gestione dello scenario?
//		// 2) la business key e' sufficiente?
//		Session session = routingContext.session();
//		if (session.get("businessKey") == null)
//			session.put("businessKey", businessKey);
		
		JsonObject body = new JsonObject()
			.put("name", name)
			.put("category", category)
			.put("template", template)
			.put("nodeProperties", nodeProperties)
			.put("reqProperties", reqProperties)
			.put("capProperties", capProperties)
			.put("relationships", relationships);		
		
	
	        String provider = nodeProperties.getString("provider");
		switch(category) {
		case "vm":	
//			instantiateVM.post("/vms", response -> {
			instantiateVM.get(provider).post("/vms", response -> {			
				response.exceptionHandler(future::fail); /* Eccezione sulla response da gestire */
				
				statusCode = response.statusCode();
				response.bodyHandler(buffer -> {
					future.complete(buffer.toString());
				});
				
		  })
			.exceptionHandler(future::fail) /* Eccezione sulla request da gestire */
//			.exceptionHandler(x -> future.fail(x)) /* Eccezione sulla request da gestire */
			.putHeader("content-type", "application/json; charset=utf-8")
			.end(body.encode());
//			.setChunked(true)
//			.write(body.encode()) /* Inserimento del body nella richiesta */
//			.end();
			break;
		case "storage":
		  	// look up for specific connector for this storage and platform
			String lookupname = provider + "-" + nodeProperties.getString("platform");
			if(addStorage.containsKey(lookupname)){
				addStorage.get(lookupname).post("/resources", response -> {
					response.exceptionHandler(future::fail);
				
					statusCode = response.statusCode();
					response.bodyHandler(buffer -> {
						future.complete(buffer.toString());
					});
				})
				.exceptionHandler(future::fail)
				.putHeader("content-type", "application/json; charset=utf-8")
				.end(body.encode());
				break;
			}
			// otherwise we use the more general connector
			addStorage.get(provider).post("/resources", response -> {
				response.exceptionHandler(future::fail);
				
				statusCode = response.statusCode();
				response.bodyHandler(buffer -> {
					future.complete(buffer.toString());
				});
			})
			.exceptionHandler(future::fail)
			.putHeader("content-type", "application/json; charset=utf-8")
			.end(body.encode());
			break;
			//otherwise we try with a connector with name "provider"
		case "volume":
			//			addStorage.post("/volumes", response -> {
						
			InstantiateVolume.get(provider).post("/resources", response -> {
				response.exceptionHandler(future::fail);
							
				statusCode = response.statusCode();
				response.bodyHandler(buffer -> {
				future.complete(buffer.toString());
				});
			})
			.exceptionHandler(future::fail)
			.putHeader("content-type", "application/json; charset=utf-8")
			.end(body.encode());
			break;
		case "cluster": {
			//instantiateLocation.get(platform).post("/clusters", response -> {
			String platform = nodeProperties.getString("platform");
			//System.out.println("[createResource]: name=" + name + " category=" + category + " provider=" + provider + " platform=" + platform);
			provider = "manual"; // For now we do not support the creation of cluster from zero, but a user can specify the provider used in TOSCA
 			instantiateLocation.get(provider + "-" + platform).post("/clusters", response -> {			
		    	response.exceptionHandler(future::fail);

	   			statusCode = response.statusCode();
	   			response.bodyHandler(buffer -> {
		   			future.complete(buffer.toString());
	   			});
   			})
		   .exceptionHandler(future::fail)
		   .putHeader("content-type", "application/json; charset=utf-8")
		   .end(body.encode());
   			break;
		}
		case "federation": {
			//instantiateLocation.get(platform).post("/clusters", response -> {
			String platform = nodeProperties.getString("platform");
			//System.out.println("[createResource]: name=" + name + " category=" + category + " provider=" + provider + " platform=" + platform);
   			// 1. first we try and see if there is a specific connector for this provider + platform
			String key = provider + "-" + platform;
			if(! instantiateLocation.containsKey(key)){
				// 2. if a specific connector does not exists, we use the general connector installed for this platform
				key = platform;
			}
			instantiateLocation.get(key).post("/federations", response -> {			
	   			response.exceptionHandler(future::fail);

	   			statusCode = response.statusCode();
	   			response.bodyHandler(buffer -> {
		   			future.complete(buffer.toString());
	   			});
   			})
		   .exceptionHandler(future::fail)
		   .putHeader("content-type", "application/json; charset=utf-8")
		   .end(body.encode());
   			break;
		}

		case "network": break;
		case "subnet": break;
		}
		
		future.setHandler(ar -> {
			/* Controllo se il future è in stato di failure:  */
//			if (future.failed());		--> Significa che c'e' stato un failure o nella richiesta o nella risposta
//			if(future.succeeded());	--> Significa che la richiesta e' andata a buon fine, ma lo status-code della response potrebbe comunque corrispondere a un errore.
              //System.out.println("BROKER-createResource - response body is: " + future.result());
              routingContext.response()
		      .setStatusCode(statusCode)
		      .putHeader("content-type", "application/json; charset=utf-8")
		      .end(future.result());
	        });
	}
	
	private void checkResource(RoutingContext routingContext) {
		Future<String> future = Future.future();
		//System.out.println("Inside checkResource!!! ...");
		//String categoryParam = routingContext.request().getParam("category");
		//String[] categoryParamSplit = categoryParam.split(":");
		//String category = categoryParamSplit[0];
		//String provider = categoryParamSplit[1];
		
        JsonObject requestBody = routingContext.getBodyAsJson();
        //System.out.println("requestBody of check Resource is " + requestBody.toString());
		String name = requestBody.getString("name");
		String category = requestBody.getString("category");
//		String businessKey = requestBody.getString("businessKey");
		JsonObject nodeProperties = requestBody.getJsonObject("nodeProperties");
		JsonObject reqProperties = requestBody.getJsonObject("reqProperties");
		JsonObject capProperties = requestBody.getJsonObject("capProperties");
		JsonObject nodeAttributes = requestBody.getJsonObject("nodeAttributes");
		JsonObject capAttributes = requestBody.getJsonObject("capAttributes");
		
		if(nodeAttributes == null || nodeProperties == null)
			// Se i parametri sono vuoti, allora viene restituita una Bad Request
			routingContext.response().setStatusCode(400).end();
			
		String id = nodeAttributes.getString("id");
		String provider = nodeProperties.getString("provider");
		
		switch(category) {
		case "vm": 
			//System.out.println("Invocazione di instantiateVM...");
//			instantiateVM.get("/vms/" + encode(id), response -> {
			instantiateVM.get(provider).get("/vms/" + encode(id), response -> { 			
			    response.exceptionHandler(future::fail); /* Eccezione sulla response da gestire */
			    
			    statusCode = response.statusCode();
			    response.bodyHandler(buffer -> {
			    	future.complete(buffer.toString());
			    });
			})
		    .exceptionHandler(future::fail)  /* Eccezione sulla request da gestire */                                                
		    .end();  			
			break;
		case "storage":
		  	// look up for specific connector for this storage and platform			  
			String lookupname = provider + "-" + nodeProperties.getString("platform");  
			if(addStorage.containsKey(lookupname)){
				  addStorage.get(lookupname).get("/resources/" + encode(id), response -> {
					  response.exceptionHandler(future::fail);
				  
					  statusCode = response.statusCode();
					  response.bodyHandler(buffer -> {
						  future.complete(buffer.toString());
					  });
				  })
				  .exceptionHandler(future::fail)
				  .end();
				  break;
			  }
			// otherwise we use the more general connector
			addStorage.get(provider).get("/resources/" + encode(id), response -> {			
				response.exceptionHandler(future::fail);
				
				statusCode = response.statusCode();
				response.bodyHandler(buffer -> {
					future.complete(buffer.toString());
				});
			})
			.exceptionHandler(future::fail)
			.end();
			break;
		case "volume":
//			addStorage.get("/volumes/" + encode(id), response -> {
			InstantiateVolume.get(provider).get("/resources/" + encode(id), response -> {			
				response.exceptionHandler(future::fail);
				
				statusCode = response.statusCode();
				response.bodyHandler(buffer -> {
					future.complete(buffer.toString());
				});
			})
			.exceptionHandler(future::fail)
			.end();
			break;
		case "cluster": {
			String platform = nodeProperties.getString("platform");
			provider = "manual"; // For now we do not support the creation of cluster from zero, but a user can specify the provider used in TOSCA
			instantiateLocation.get(provider + "-" + platform).get("/clusters/" + encode(id), response -> {
				response.exceptionHandler(future::fail);

				statusCode = response.statusCode();
				response.bodyHandler(buffer -> {
					future.complete(buffer.toString());
				});
			})
					.exceptionHandler(future::fail)
					.end();
			break;
		}
		case "federation": {
			String platform = nodeProperties.getString("platform");
   			// 1. first we try and see if there is a specific connector for this provider + platform
			String key = provider + "-" + platform;
			if(! instantiateLocation.containsKey(key)){
				// 2. if a specific connector does not exists, we use the general connector installed for this platform
				key = platform;
			}			
			instantiateLocation.get(key).get("/federations/" + encode(id), response -> {
				response.exceptionHandler(future::fail);

				statusCode = response.statusCode();
				response.bodyHandler(buffer -> {
					future.complete(buffer.toString());
				});
			})
					.exceptionHandler(future::fail)
					.end();
			break;
		}
		case "network": break;
		case "subnet": break;
		}
		
		future.setHandler(ar -> {
			
			routingContext.response()
				.setStatusCode(statusCode)
				.putHeader("content-type", "application/json; charset=utf-8")
				.end(future.result());
	        });
		
	}
	
	private void createPacket(RoutingContext routingContext) {		
		Future<String> future = Future.future();
		
		JsonObject requestBody = routingContext.getBodyAsJson();
		String name = requestBody.getString("name");
		String category = requestBody.getString("category");
		JsonObject properties = requestBody.getJsonObject("properties");
		
		if(name == null || category == null || properties == null) {
			// Se i parametri sono vuoti, allora viene restituita una Bad Request
			routingContext.response().setStatusCode(400).end();
		} else {	
			name = name.replaceAll(category, "");
			
			JsonObject body = new JsonObject()
			.put("name", name)
			.put("properties", properties);
			
			create.get(category).get(name).post("/" + category + "/create", response -> {
					response.exceptionHandler(future::fail);
					
					statusCode = response.statusCode();
					response.bodyHandler(buffer -> {
						future.complete(buffer.toString());
					});
				})
				.exceptionHandler(future::fail)
				.putHeader("content-type", "application/json; charset=utf-8")
				.end(body.encode());
				
//			switch(category) {
//			case "ws":
//				createWS.post("/ws/create", response -> {
//					response.exceptionHandler(future::fail);
//					
//					statusCode = response.statusCode();
//					response.bodyHandler(buffer -> {
//						future.complete(buffer.toString());
//					});
//				})
//				.exceptionHandler(future::fail)
//				.putHeader("content-type", "application/json; charset=utf-8")
//				.end(body.encode());
//				break;
//			case "sc":
//				createSC.post("/sc/create", response -> {
//					response.exceptionHandler(future::fail);
//					
//					statusCode = response.statusCode();
//					response.bodyHandler(buffer -> {
//						future.complete(buffer.toString());
//					});
//				})
//				.exceptionHandler(future::fail)
//				.putHeader("content-type", "application/json; charset=utf-8")
//				.end(body.encode());
//				break;
//			case "dbms":
//				createDBMS.post("/dbms/create", response -> {
//					response.exceptionHandler(future::fail);
//					
//					statusCode = response.statusCode();
//					response.bodyHandler(buffer -> {
//						future.complete(buffer.toString());
//					});
//				})
//				.exceptionHandler(future::fail)
//				.putHeader("content-type", "application/json; charset=utf-8")
//				.end(body.encode());
//				break;
//			case "db":
//				createDB.post("/db/create", response -> {
//					response.exceptionHandler(future::fail);
//					
//					statusCode = response.statusCode();
//					response.bodyHandler(buffer -> {
//						future.complete(buffer.toString());
//					});
//				})
//				.exceptionHandler(future::fail)
//				.putHeader("content-type", "application/json; charset=utf-8")
//				.end(body.encode());
//				break;
//			case "wa":
//				createWA.post("/wa/create", response -> {
//					response.exceptionHandler(future::fail);
//					
//					statusCode = response.statusCode();
//					response.bodyHandler(buffer -> {
//						future.complete(buffer.toString());
//					});
//				})
//				.exceptionHandler(future::fail)
//				.putHeader("content-type", "application/json; charset=utf-8")
//				.end(body.encode());
//				break;
//			}
			
			future.setHandler(ar -> {
				routingContext.response()
					.setStatusCode(statusCode)
					.putHeader("content-type", "application/json; charset=utf-8")
					.end(future.result());
		        });
		}
	}
	
	private void configurePacket(RoutingContext routingContext) {		
		Future<String> future = Future.future();
		
		JsonObject requestBody = routingContext.getBodyAsJson();
		String name = requestBody.getString("name");
		String category = requestBody.getString("category");
		JsonObject properties = requestBody.getJsonObject("properties");
		
		if(name == null || category == null || properties == null) {
			// Se i parametri sono vuoti, allora viene restituita una Bad Request
			routingContext.response().setStatusCode(400).end();
		} else {
			name = name.replaceAll(category, "");
			
			JsonObject body = new JsonObject()
			.put("name", name)
			.put("properties", properties);			

			configure.get(category).get(name).post("/" + category + "/configure", response -> {
				response.exceptionHandler(future::fail);
				
				statusCode = response.statusCode();
				response.bodyHandler(buffer -> {
					future.complete(buffer.toString());
				});
			})
			.exceptionHandler(future::fail)
			.putHeader("content-type", "application/json; charset=utf-8")
			.end(body.encode());			
			
//			switch(category) {
//			case "ws":
//				configureWS.post("/ws/configure", response -> {
//					response.exceptionHandler(future::fail);
//					
//					statusCode = response.statusCode();
//					response.bodyHandler(buffer -> {
//						future.complete(buffer.toString());
//					});
//				})
//				.exceptionHandler(future::fail)
//				.putHeader("content-type", "application/json; charset=utf-8")
//				.end(body.encode());
//				break;
//			case "sc":
//				statusCode = 200;
//				JsonObject result = new JsonObject();
//				
//				result.put("status", ConfigureSC.Status.OK.value());
//				result.put("message", "");
//			
//				future.complete(result.encode());
//				break;
//			case "dbms": 
//				configureDBMS.post("/dbms/configure", response -> {
//					response.exceptionHandler(future::fail);
//					
//					statusCode = response.statusCode();
//					response.bodyHandler(buffer -> {
//						future.complete(buffer.toString());
//					});
//				})
//				.exceptionHandler(future::fail)
//				.putHeader("content-type", "application/json; charset=utf-8")
//				.end(body.encode());
//				break;
//			case "db": 
//				configureDB.post("/db/configure", response -> {
//					response.exceptionHandler(future::fail);
//					
//					statusCode = response.statusCode();
//					response.bodyHandler(buffer -> {
//						future.complete(buffer.toString());
//					});
//				})
//				.exceptionHandler(future::fail)
//				.putHeader("content-type", "application/json; charset=utf-8")
//				.end(body.encode());
//				break;
//			case "wa":
//				configureWA.post("/wa/configure", response -> {
//					response.exceptionHandler(future::fail);
//					
//					statusCode = response.statusCode();
//					response.bodyHandler(buffer -> {
//						future.complete(buffer.toString());
//					});
//				})
//				.exceptionHandler(future::fail)
//				.putHeader("content-type", "application/json; charset=utf-8")
//				.end(body.encode());
//				break;
//			}
			
			future.setHandler(ar -> {
				routingContext.response()
					.setStatusCode(statusCode)
					.putHeader("content-type", "application/json; charset=utf-8")
					.end(future.result());
		        });			
		}
	}

	private void startPacket(RoutingContext routingContext) {		
		Future<String> future = Future.future();
		
		JsonObject requestBody = routingContext.getBodyAsJson();
		String name = requestBody.getString("name");
		String category = requestBody.getString("category");
		JsonObject properties = requestBody.getJsonObject("properties");
		
		if(name== null || category == null || properties == null) {
			// Se i parametri sono vuoti, allora viene restituita una Bad Request
			routingContext.response().setStatusCode(400).end();
		} else {
			statusCode = 200;
			JsonObject result = new JsonObject();
						
			switch(category) {
			case "ws": 
				result.put("status", StartWS.Status.OK.value());
//				result.put("message", "Apache WS startup suceeded");
				break;
			case "sc": 
				result.put("status", StartSC.Status.OK.value());
//				result.put("message", "PHP startup suceeded");
				break;
			case "dbms":
				result.put("status", StartDBMS.Status.OK.value());
//				result.put("message", "MySQL DBMS startup suceeded");
				break;
			case "db": 
				result.put("status", StartDB.Status.OK.value());
//				result.put("message", "MySQL database startup suceeded");				
				break;
			case "wa": 
				result.put("status", StartWA.Status.OK.value());
//				result.put("message", "WordPress WA startup suceeded");				
				break;
			}
			
			result.put("message", "");	
						
			routingContext.response()
			.setStatusCode(statusCode)
			.putHeader("content-type", "application/json; charset=utf-8")
			.end(result.encode());
			
		}
				
	}
	
	private void createDU(RoutingContext routingContext) {
		//@TODO -  non usato e non modificato per essere compatibile con TORCH-v2 
		Future<String> future = Future.future();
		//System.out.println("createDU request received --> :) ");
		JsonObject requestBody = routingContext.getBodyAsJson();
		String name = requestBody.getString("name");
		JsonObject properties = requestBody.getJsonObject("properties");

		// Add check on cluster: is it an existing cluster or must we create it? Add a dashboard-managed parameter
		if(name == null || properties == null)
			// Se i parametri sono vuoti, allora viene restituita una Bad Request
			routingContext.response().setStatusCode(400).end();
		
		String platform = properties.getString("platform");
		
		//instantiateDU.post("/dus/create", response -> {
		instantiateDU.get(platform).post("/dus/create", response -> {
			response.exceptionHandler(future::fail); /* Eccezione sulla response da gestire */
			
			statusCode = response.statusCode();
			response.bodyHandler(buffer -> {
				future.complete(buffer.toString());
			});
			
		})
		.exceptionHandler(future::fail) /* Eccezione sulla request da gestire */
		.putHeader("content-type", "application/json; charset=utf-8")
		.end(requestBody.encode());
		
		future.setHandler(ar -> {
			/* Controllo se il future è in stato di failure:  */
//			if (future.failed());		--> Significa che c'e' stato un failure o nella richiesta o nella risposta
//			if(future.succeeded());	--> Significa che la richiesta e' andata a buon fine, ma lo status-code della response potrebbe comunque corrispondere a un errore.
			routingContext.response()
		      .setStatusCode(statusCode)
		      .putHeader("content-type", "application/json; charset=utf-8")
		      .end(future.result());
	        });
		
	}

	private void configureDU(RoutingContext routingContext) {
		Future<String> future = Future.future();
		//System.out.println("Service Broker - configureDu start -2");
		JsonObject requestBody = routingContext.getBodyAsJson();
		JsonObject node = requestBody.getJsonObject("node");
		//System.out.println("body is " + requestBody.encode());

		if(node == null)
			// Node è necessario, restituisco una Bad Request
			routingContext.response().setStatusCode(400).end();

		//@TODO: non essendoci lo step START, l'id viene creato dal connettore durante lo step CONFIGURE
		
		String name = node.getString("name");
		String category = node.getString("category");
		if(name == null || category == null)
			// Se i parametri sono vuoti, allora viene restituita una Bad Request
			routingContext.response().setStatusCode(400).end();
		
		String platform = category.split("-")[0];

		//System.out.println("configureDU --> name: " + name + " category: " + category + " platform: " + platform);
		
		 
		//instantiateDU.post("/dus/configure", response -> {   
		instantiateDU.get(platform).post("/dus/configure", response -> {   
		    response.exceptionHandler(future::fail); /* Eccezione sulla response da gestire */
		    
		    statusCode = response.statusCode();
		    response.bodyHandler(buffer -> {
		    	future.complete(buffer.toString());
		    });
		})
	    .exceptionHandler(future::fail)  /* Eccezione sulla request da gestire */                                                
	    .end(requestBody.encode());  			
		
		future.setHandler(ar -> {
			routingContext.response()
				.setStatusCode(statusCode)
				.putHeader("content-type", "application/json; charset=utf-8")
				.end(future.result());
	        });
		
	}

	private void checkDU(RoutingContext routingContext) {
		Future<String> future = Future.future();
		//System.out.println("Service Broker - checkDu start");
		JsonObject requestBody = routingContext.getBodyAsJson();
		JsonObject node = requestBody.getJsonObject("node");
		JsonObject checkInputs = requestBody.getJsonObject("checkInputs");
		String name = node.getString("name");
		String id = checkInputs.getString("id");
		String category = node.getString("category");
		String platform = category.split("-")[0];

		if(id == null || category == null)
			// Se i parametri sono vuoti, allora viene restituita una Bad Request
			routingContext.response().setStatusCode(400).end();
		
		//System.out.println("checkDU --> name: " + name + " id: " + id + " platform: " + platform);
		
		 
		//instantiateDU.post("/dus/check", response -> {
		instantiateDU.get(platform).post("/dus/check", response -> {
		    response.exceptionHandler(future::fail); /* Eccezione sulla response da gestire */
		    
		    statusCode = response.statusCode();
		    response.bodyHandler(buffer -> {
		    	future.complete(buffer.toString());
		    });
		})
	    .exceptionHandler(future::fail)  /* Eccezione sulla request da gestire */                                                
	    .end(requestBody.encode());  			
		
		future.setHandler(ar -> {
			routingContext.response()
				.setStatusCode(statusCode)
				.putHeader("content-type", "application/json; charset=utf-8")
				.end(future.result());
	        });
		
	}
	


	private void startDU(RoutingContext routingContext){
		Future<String> future = Future.future();
		//System.out.println("Service Broker - startDu start");
		JsonObject requestBody = routingContext.getBodyAsJson();
		JsonObject node = requestBody.getJsonObject("node");
		JsonObject startInputs = requestBody.getJsonObject("startInputs");
		String name = node.getString("name");
		String category = node.getString("category");
		String platform = category.split("-")[0];

		if(category == null || platform == null){
			// Se i parametri sono vuoti, allora viene restituita una Bad Request
			routingContext.response().setStatusCode(400).end();
		} else {
			//System.out.println("startDU --> name: " + name + " platform: " + platform);
		
		 
			//instantiateDU.post("/dus/check", response -> {
			instantiateDU.get(platform).post("/dus/start", response -> {
		    	response.exceptionHandler(future::fail); /* Eccezione sulla response da gestire */
		    
		    	statusCode = response.statusCode();
		    	response.bodyHandler(buffer -> {
		    		future.complete(buffer.toString());
		    	});
			})
	    	.exceptionHandler(future::fail)  /* Eccezione sulla request da gestire */                                                
	    	.end(requestBody.encode());  			
		
			future.setHandler(ar -> {
				routingContext.response()
				.setStatusCode(statusCode)
				.putHeader("content-type", "application/json; charset=utf-8")
				.end(future.result());
	    	});
		}
	}
	
	private static String encode(String value) {
		try {
			return URLEncoder.encode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unsupported encoding");
		}
	}
	
}
