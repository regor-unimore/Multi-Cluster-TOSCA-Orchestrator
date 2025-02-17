package it.unict.vertx.esb.createdb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import it.unict.vertx.esb.packet.create.CreateDB;

public class CreateDBMySqlAPIVerticle extends AbstractVerticle implements CreateDB {
	
	private String keyName, keyPath, scriptName;
	
	@Override
	  public void start(Future<Void> future) throws Exception {
		super.start();
		
		// Inizializzazione
//		keyName = System.getProperty("user.dir") + config().getString("key.name");
		keyName = config().getString("key.name");
		keyPath = System.getProperty("user.dir");
		scriptName = System.getProperty("user.dir") + config().getString("script.name");
				
		Router router = Router.router(vertx);
		router.route().handler(BodyHandler.create());	 
		 
		// API per creare il DB
		router.post("/db/create").handler(this::create);
		 
		vertx.createHttpServer().requestHandler(router::accept)
			.listen(config().getInteger("http.port"), ar -> {
	         if (ar.succeeded()) {
	        	 System.out.println("Server started");
	         } else {
	        	 System.out.println("Cannot start the server: " + ar.cause());
	         }
	      });
		
	}

	@Override
	public void create(RoutingContext routingContext) {
		// Recupero dei parametri della richiesta
		JsonObject requestBody = routingContext.getBodyAsJson();
		String name = requestBody.getString("name");
		Map<String, Object> properties = requestBody.getJsonObject("properties").getMap();
		
		Set<String> usr = properties.keySet().stream()
				.filter(s -> s.matches("^dbserver\\d?\\.username"))
				.collect(Collectors.toSet());
		
		Set<String> addr = properties.keySet().stream()
				.filter(s -> s.matches("^dbserver\\d?\\.address"))
				.collect(Collectors.toSet());
		
		Set<String> key = properties.keySet().stream()
				.filter(s -> s.matches("^dbserver\\d?\\.key"))
				.collect(Collectors.toSet());		
		
		String dbUsername = (String) properties.get(usr.toArray()[0]);
		String dbAddress = (String) properties.get(addr.toArray()[0]);
		String dbKey = (String) properties.get(key.toArray()[0]);		
		
		//String dbUsername = (String) properties.get("dbserver.username");
		//String dbAddress = (String) properties.get("dbserver.address");
		String dbName = (String) properties.get("name");
		String dbRootPwd = (String) properties.get("db.root.pwd");
		
		vertx.executeBlocking(future -> {		
			File keyFile = null;
			BufferedWriter keyBufferedWriter = null;
			
			 try {
				keyFile = File.createTempFile(keyName, ".pem", new File(keyPath));
//				keyFile.setReadable(true, true);
//				keyFile.setWritable(true, true);
				
				Set<PosixFilePermission> perms = new HashSet<>();
				perms.add(PosixFilePermission.OWNER_READ);
				perms.add(PosixFilePermission.OWNER_WRITE);	
				
				Files.setPosixFilePermissions(keyFile.toPath(), perms);					
				
				keyBufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(keyFile), StandardCharsets.UTF_8));
				keyBufferedWriter.write(dbKey);
				keyBufferedWriter.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
		
			String command = "ssh -o 'UserKnownHostsFile=/dev/null' -o 'StrictHostKeyChecking=no' -i " 
					+ keyFile.getAbsolutePath() + " " + dbUsername + "@" + dbAddress 
					+ " 'bash -s' < " + scriptName + " '" + dbName 
					+ "' '" + dbRootPwd + "' | tail -n 1";
			
			String result = executeCommand(command);
			keyFile.delete();
			future.complete(result);
		}, res -> {
			if (res.succeeded()) {
				JsonObject output = new JsonObject((String) res.result());
				String code = output.getString("code");
				String message = output.getString("message");
				
				CreateDB.Status mappedStatus = CreateDB.Status.OK; 
				int responseCode = 200;
				
				if (!code.equals("0")) {
					responseCode = 500;
					mappedStatus = CreateDB.Status.ERROR;
				}
			
				JsonObject responseBody = new JsonObject();
				responseBody.put("status", mappedStatus.value());
				responseBody.put("message", message);
				
				routingContext.response()
					.setStatusCode(responseCode)
				    .putHeader("content-type", "application/json; charset=utf-8")
				    .end(responseBody.encode());
			}			
		});
		
//		String command = "ssh -o 'UserKnownHostsFile=/dev/null' -o 'StrictHostKeyChecking=no' -i " 
//				+ keyName + " " + dbUsername + "@" + dbAddress 
//				+ " 'bash -s' < " + scriptName + " '" + dbName 
//				+ "' '" + dbRootPwd + "' | tail -n 1";
		
//		vertx.executeBlocking(future -> {
//			String result = executeCommand(command);
//			future.complete(result);
//		}, res -> {
//			if (res.succeeded()) {
//				JsonObject output = new JsonObject((String) res.result());
//				String code = output.getString("code");
//				String message = output.getString("message");
//				
//				CreateDB.Status mappedStatus = CreateDB.Status.OK; 
//				int responseCode = 200;
//				
//				if (!code.equals("0")) {
//					responseCode = 500;
//					mappedStatus = CreateDB.Status.ERROR;
//				}
//			
//				JsonObject responseBody = new JsonObject();
//				responseBody.put("status", mappedStatus.value());
//				responseBody.put("message", message);
//				
//				routingContext.response()
//					.setStatusCode(responseCode)
//				    .putHeader("content-type", "application/json; charset=utf-8")
//				    .end(responseBody.encode());
//			}
//		});
				
	}
	
	private String executeCommand(String command) {
		StringBuffer output = new StringBuffer();
		
		try {
			Process process = Runtime.getRuntime().exec(new String[] {"bash", "-c", command});			
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			String line = "";
			while ((line = reader.readLine()) != null) {
				output.append(line);
			}
			
			process.waitFor();
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}	
		
		return output.toString();
		
	}

}
