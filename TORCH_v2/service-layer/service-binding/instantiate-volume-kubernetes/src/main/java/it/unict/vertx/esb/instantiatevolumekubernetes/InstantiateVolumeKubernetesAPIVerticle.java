package it.unict.vertx.esb.instantiatevolumekubernetes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kubernetes.client.custom.Quantity;
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
import io.kubernetes.client.openapi.ApiException;
//import io.kubernetes.client.ApiException;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import it.unict.vertx.esb.resource.InstantiateLocationResource;

public class InstantiateVolumeKubernetesAPIVerticle extends AbstractVerticle implements InstantiateLocationResource {


	@Override
	public void start(Future<Void> future) throws Exception {
		super.start();

		Router router = Router.router(vertx);
		router.route().handler(BodyHandler.create());

		// API per creare istanze e controllarne lo stato
		router.post("/resources").handler(this::createLocationResource);
		router.get("/resources/:id").handler(this::checkLocationResource);

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
	public void createLocationResource(RoutingContext routingContext) {

		//retrieve request parameters
		JsonObject node = routingContext.getBodyAsJson();
		System.out.println("CreateLocationResource Volume - request body is: " + node.encode());
		// node is not passed by the BPMN plans when working with resources...
		//JsonObject node = requestBody.getJsonObject("node");
		JsonObject nodeProperties = node.getJsonObject("nodeProperties");
		String data_retention = nodeProperties.getString("data_retention");
		String access_mode = nodeProperties.getString("access_mode");
		String fancy_size = nodeProperties.getString("size");
		JsonObject prepopulator = nodeProperties.getJsonObject("pre-populate");

		//parse size if not null
		JsonObject responseBody = new JsonObject();
		JsonObject nodeAttrs = new JsonObject();
		JsonObject capAttrs = new JsonObject();
		JsonObject volume =  new JsonObject();
		int responseCode = 200;

		
		try {
			Float sizeinMB = null;
			Quantity kubernetes_size = null;
			if(fancy_size != null){
				String[] parts = fancy_size.split(" ");
				if (parts.length != 2) {
					throw new IllegalArgumentException("Invalid size format.");
				}
				sizeinMB = getSizeInMega(parts[0], parts[1]);
				kubernetes_size = getSizeInKube(parts[0], parts[1] );
				System.out.println("size in MB is " + sizeinMB.toString());
			}

			
			//Kubernetes specific logic
			String volume_type = getVolumeMapping(data_retention, access_mode, sizeinMB, prepopulator);
			if (volume_type == null){
				throw new IllegalArgumentException("Could not create a volume with the requested characteristics: " + nodeProperties.encode() );
			}

			switch(volume_type){
				case "configmap":
						System.out.println("configmaps @TODO");
						break;
				case "emptydir":
						System.out.println("emptydir was chosen! @TODO");
						String id = node.getString("name");
						nodeAttrs.put("id", id);
						volume.put("name", id.replaceAll("_", "-"));
						//set sizeLimit if size was setted in nodeProperties @TODO
						V1EmptyDirVolumeSource emptydir = new V1EmptyDirVolumeSource();
						if(sizeinMB != null){
							emptydir.setSizeLimit(kubernetes_size);
						}
						V1Volume v = new V1Volume().name(id.replaceAll("_", "-"))
												   .emptyDir(emptydir);
						
						System.out.println("V1Volume created is: " + Yaml.dump(v));
						volume.put("definition",Yaml.dump(v));

						//create init-container or sidecar container for preloading if required
						if(prepopulator!= null){
							JsonObject additional_info = emptydirPrepopulator(prepopulator, v);
							volume.mergeIn(additional_info);
						}
						break;
				case "local":
						System.out.println("local @TODO");
						break;
				default:
						System.out.println("ERROR: volume not supported @TODO");
						break;
			}
			

			capAttrs.put("volume", volume);
			responseBody.put("nodeAttributes", nodeAttrs);
			responseBody.put("capAttributes", capAttrs);
			System.out.println("[CreateLocationResource Volume]: responseBody is " + responseBody.encode());
		}
		catch(IllegalArgumentException ia){
			ia.printStackTrace();
			responseBody.put("status", "ERROR");
			responseBody.put("message", "node properties not parsable");
			responseBody.put("details", ia.getMessage());
			responseCode = 500;			
		}
			
		routingContext.response()
			.setStatusCode(responseCode)
			.putHeader("content-type", "application/json; charset=utf-8")
			.end(responseBody.encode());
			
	}
			
			
	private Float getSizeInMega(String scalar, String unit) {
		Float size = null;
		if(unit.contains("k") || unit.contains("K")){
			return Float.parseFloat(scalar)/1000;
		} else if (unit.contains("m") || unit.contains("M")){
			return Float.parseFloat(scalar);
		} else if (unit.contains("g") || unit.contains("G")){
			return Float.parseFloat(scalar)*1000;
		} else if (unit.contains("t") || unit.contains("T")){
			return Float.parseFloat(scalar)*1000*1000;
		} else { //bytes
			return (Float.parseFloat(scalar)/1000)/1000;
		}

	}

	private Quantity getSizeInKube(String scalar, String unit) {
		switch(unit){
			case "B": {
				//Byte are not allowed for volumes
				return null;
			}
			case "KB": {
				return new Quantity(scalar + "K");
			}
			case "KiB": {
				return new Quantity(scalar + "Ki");
			}
			case "MB": {
				return new Quantity(scalar + "M");
			}
			case "MiB": {
				return new Quantity(scalar + "Mi");
			}
			case "GB": {
				return new Quantity(scalar + "G");
			}
			case "GiB": {
				return new Quantity(scalar + "Gi");
			}
			case "TB": {
				return new Quantity(scalar + "T");
			}
			case "TiB": {
				return new Quantity(scalar + "K");
			}
			default:{
				return null;
			}
		}

	}
			
	@Override
	public void checkLocationResource(RoutingContext routingContext) {

		JsonObject responseBody = new JsonObject();
		JsonObject nodeAttrs = new JsonObject();

		InstantiateLocationResource.Status mappedStatus = InstantiateLocationResource.Status.OK;
		nodeAttrs.put("status", mappedStatus );
		responseBody.put("nodeAttributes", nodeAttrs);
		int responseCode = 200;

		routingContext.response()
				.setStatusCode(responseCode)
				.putHeader("content-type", "application/json; charset=utf-8")
				.end(responseBody.encode());

	}

	private String getVolumeMapping(String data_retention, String access_mode, Float size, JsonObject prepopulator){
		if(data_retention.equals("transient")){
			if(access_mode.equals("read-only")){
				if((prepopulator != null) && (size != null) && (size < 1000)) return "configmap";
				return "emptydir";
			} else {
				return "emptydir";
			}
		}
		else if(data_retention.equals("persistent")){
			if(access_mode.equals("read-only") && (prepopulator != null) && (size != null) && (size < 1000)) return "configmap";
			return "local";
		}
		return null;
	}

	private JsonObject emptydirPrepopulator(JsonObject prepopulator, V1Volume volume){
		JsonObject result = new JsonObject();
		//required fields
		String protocol = prepopulator.getString("protocol");
		String content = prepopulator.getString("content");
		String source = prepopulator.getString("source");
		if(protocol == null || content == null || source == null){
			throw new IllegalArgumentException("Missing required (protocol, content , source) fields in prepopulator: prepopulator is " + prepopulator.encode());
		}

		//optional fields
		JsonObject source_repo = prepopulator.getJsonObject("source_repo");
		String load_path = prepopulator.getString("load_path");
		String periodic_update = prepopulator.getString("periodic_update");

		switch (protocol){
			case "webdav": {
				if(source_repo == null){
					throw new IllegalArgumentException("Missing 'source_repo' fields in prepopulator, it is required when using webdav protocol!");
				}
				JsonObject repo = source_repo.getJsonObject("value");
				String name = protocol + "-preloader";
				String image = "edrudi97/torch-preloader-webdav:v1.0.1";
				JsonObject credential = repo.getJsonObject("credential");
				String user = credential.getString("user");
				String token = credential.getString("token");
				String url = repo.getString("url");
				/*
				 *           
				 * 
				 * - name: USER
            value: 202184@unimore.it
          - name: PASSWORD
            value: CGnxa-GcsKk-erkko-yxsr6-jCTxB
          - name: WEBDAV_SHARE
            value: https://nxc.wl.ing.unimore.it/remote.php/dav/files/202184%40unimore.it
          - name: FILE
            value: /ESCALATION/OSRM_Map/map/italy-latest.osrm.datasource_names        
          - name: OUTPUT_DIR
            value: /mnt/data
				 */
				List<V1EnvVar> envs = new ArrayList<>();
				// USER
				envs.add(new V1EnvVar().name("USER").value(user));
				// PASSWORD
				envs.add(new V1EnvVar().name("PASSWORD").value(token));
				//WEBDAV_SHARE
				envs.add(new V1EnvVar().name("WEBDAV_SHARE").value(url));
				//FILE
				envs.add(new V1EnvVar().name("FILE").value(source));
				//OUTPUT_DIR
				String out_dir = load_path;
				if(out_dir == null) {
					out_dir = "/mnt/workdir";
				} else {
					if(!out_dir.startsWith("/"))
						out_dir = "/" + out_dir;
				}
				envs.add(new V1EnvVar().name("OUTPUT_DIR").value(out_dir));
				V1VolumeMount vm = new V1VolumeMount().name(volume.getName()).mountPath(out_dir);
				V1Container preloader = new V1Container().name(name)
														 .image(image)
														 .env(envs)
														 .addVolumeMountsItem(vm);
				String serialized_preloader = Yaml.dump(preloader);
				System.out.println("[InstantiateVolume Kuberentes] - initContainer is " + serialized_preloader);
				result.put("init-container", serialized_preloader);
				//@TODO handle periodic_update
				 break;
			}
			case "s3": {
				//@TODO
				break;
			}
			default:{
				break;
			}	
		}


		return result;
	}

}
