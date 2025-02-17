package it.unibo.vertx.esb.instantiateliqofederation;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
//import com.google.gson.Gson;

import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import io.kubernetes.client.util.PatchUtils;
import io.kubernetes.client.util.Yaml;
import io.kubernetes.client.util.PatchUtils.PatchCallFunc;
import io.kubernetes.client.util.credentials.ClientCertificateAuthentication;
import io.kubernetes.client.openapi.JSON;


import java.util.HashMap;
import java.util.HashSet;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import it.unict.vertx.esb.resource.InstantiateLocation;
import java.util.Base64;
import java.util.Arrays;

public class InstantiateLiqoFederationAPIVerticle extends AbstractVerticle implements InstantiateLocation {
		
	//private String ApiServerUri;
	//private String KeyFilePath, CertFilePath, CAFilePath;
	//private Map<String, Map<String, String>> clusters;

	@Override
	  public void start(Future<Void> future) throws Exception {
		super.start();
		
		// Inizializzazione
		//ApiServerUri = config().getString("apiserver.uri");

		// Authentication
		//CAFilePath =  System.getProperty("user.dir") + config().getString("ca.cert.file");
		//KeyFilePath =  System.getProperty("user.dir") + config().getString("user.key.file");
		//CertFilePath =  System.getProperty("user.dir") + config().getString("cert.file");
		
		//clusters = new HashMap< String, Map<String, String>>();
		
		Router router = Router.router(vertx);
		router.route().handler(BodyHandler.create());	 
		 
		// API per creare istanze e controllarne lo stato
		router.post("/federations").handler(this::createLocation);
		router.get("/federations/:id").handler(this::checkLocation);
		
		// install Liqoctl if not already installed
		System.out.println("[LIQO connector] Checking if Liqoctl is installed...");
		Process checkProcess = new ProcessBuilder("bash", "-c", "command -v liqoctl").redirectErrorStream(true).start();
		captureOutput(checkProcess);
		int checkExitCode = checkProcess.waitFor();
		if (checkExitCode != 0) { // ERROR?
			
			// Install liqoctl to user directory
			System.out.println("[LIQO connector] Chack Failed! Installing liqoctl...");

			// Installation Directory and Binary Directory
			//String homeDir = System.getProperty("user.home");
			//String installDir = "/usr/app/utils";
			//String binaryDir = "/usr/app/utils/bin";
			String installDir = config().getString("workdir");
			String binaryDir = config().getString("workdir") + "/bin";
			System.out.println("[LIQO connector] Installation Dir is " + installDir + " while binary Dir is " + binaryDir);

			// Liqoctl version
			String version = config().getString("liqo.version");
			System.out.println("[LIQO connector] using liqo version " + version);

			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			// DOWNLOAD LIQOCTL ARCHIVE
			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			String downloadLiqo = 
                "curl --fail -LS \"https://github.com/liqotech/liqo/releases/download/"+ version + "/liqoctl-linux-amd64.tar.gz\" -o " + installDir + "/liqoctl-linux-amd64.tar.gz";

            ProcessBuilder curlProcessBuilder = new ProcessBuilder("bash", "-c", downloadLiqo);
            Process curlProcess = curlProcessBuilder.redirectErrorStream(true).start();

            // Capture curl output
            captureOutput(curlProcess);

            int curlExitCode = curlProcess.waitFor();
            if (curlExitCode != 0) {
                System.err.println("Failed to download liqoctl with command " + downloadLiqo);
                throw new Exception("Failed to download liqoctl with command " + downloadLiqo);
            }

			System.out.println("[LIQO connector] Liqoctl archive download completed ");
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			// UNTAR LIQOCTL
			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			String tarCommand = "tar -xz -f " + installDir + "/liqoctl-linux-amd64.tar.gz -C" + binaryDir ;

            ProcessBuilder tarProcessBuilder = new ProcessBuilder("bash", "-c", tarCommand);
            Process tarProcess = tarProcessBuilder.redirectErrorStream(true).start();

            // Capture tar output
            captureOutput(tarProcess);

            int tarExitCode = tarProcess.waitFor();
            if (tarExitCode == 0) {
                System.out.println("liqoctl archive extraction completed");
            } else {
                System.err.println("Failed to extract liqoctl.");
				throw new Exception("Failed to extract liqoctl.");
            }
			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			


			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			// CONFIGURE LIQOCTL
			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			String chmodCommand = "chmod 755 " + binaryDir + "/liqoctl";
			Process chmodProcess = new ProcessBuilder("bash", "-c", chmodCommand).redirectErrorStream(true).start();
			captureOutput(chmodProcess);
			int chmodExitCode = chmodProcess.waitFor();
			if (chmodExitCode == 0) {
				System.out.println("liqoctl permission configured successfully.");
			} else {
				System.err.println("Failed to configure permission for liqoctl.");
				throw new Exception("Failed to configure permission for liqoctl.");
			}
			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			


			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			// CHECK IF INSTALLATION IS SUCCESSFUL
			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			checkProcess = new ProcessBuilder("bash", "-c", "command -v liqoctl").redirectErrorStream(true).start();
			captureOutput(checkProcess);
			checkExitCode = checkProcess.waitFor();
			if (checkExitCode == 0) {
				System.out.println("liqoctl installed successfully.");
			} else {
				System.err.println("Failed to install liqoctl.");
				throw new Exception("Failed to install liqoctl.");
			}

		} else {
			System.out.println("liqoctl is already installed.");
		}

		// setup workdir
		Path liqoFolder = Paths.get(config().getString("workdir") + "/liqo");
		Path kubeconfigFolder = Paths.get(liqoFolder + "/kubeconfigs");

		try {
            if (!Files.exists(kubeconfigFolder)) {
                // Create the folder (since this folder is children of LiqoFolder, LiqoFolder will be creates as well)
                Files.createDirectories(kubeconfigFolder);
                System.out.println("Folders created successfully.");
            } else {
                System.out.println("Folders already exist");
            }
        } catch (IOException e) {
            System.err.println("Failed to create the folder: " + e.getMessage());
			throw new Exception("Failed to setup the working directory for the Liqo Connector");
        }

		vertx.createHttpServer().requestHandler(router::accept)
			.listen(config().getInteger("http.port"), ar -> {
	         if (ar.succeeded()) {
	        	 System.out.println("Federation started");
	         } else {
	        	 System.out.println("Cannot start the federation: " + ar.cause());
	         }
	      });
		
	}
		
	@Override
	public void createLocation(RoutingContext routingContext) {
		JsonObject responseBody = new JsonObject();
		
		// Recupero dei parametri della richiesta
		JsonObject requestBody = routingContext.getBodyAsJson();
		//System.out.println(requestBody);

		//String templateContext = "template-name"; //@TODO
		String templateContext = requestBody.getString("template");
		String name = requestBody.getString("name");

		// 1. retrieve the Kubernetes Cluster that can be used and the amount of resources that can be used
		JsonObject reqProps = requestBody.getJsonObject("reqProperties");
		JsonObject members = reqProps.getJsonObject("members");
		if (members == null || members.isEmpty()){
			System.out.println("error: no reqProperties called 'members'");
			routingContext.response()
			.setStatusCode(400)
			.end();
			return;
		}

		JsonObject nodeProps = requestBody.getJsonObject("nodeProperties");
		JsonObject clusters = nodeProps.getJsonObject("clusters");
		// 2.   select the entrypoint cluster of the Liqo Federation based on some logic
		String winner = null;
		for(Map.Entry<String, Object> entry : clusters){

			//for now we choose the first cluster as entrypoint, in the future we may decide based on resource availability and positioning/distance between each cluster
			if(winner == null)  winner = entry.getKey();
			
			JsonObject cluster = members.getJsonObject(entry.getKey());
			// 3. check if Liqo resources are installed in the cluster, otherwise install them with liqoctl
			//System.out.println("checking cluster " + entry.getKey());
			// 3.a generate a kubeconfig for the cluster if it was not already generated
			
			Path kubeconfigPath = Paths.get(config().getString("workdir") + "/liqo/kubeconfigs/" + templateContext + "/" + entry.getKey() + ".yaml");
			
			if(!generateKubeconfigForCluster(templateContext, entry.getKey(), members.getJsonObject(entry.getKey()), kubeconfigPath)){
				System.err.println("Failed to generate kubeconfig of cluster " + entry.getKey());
				routingContext.response()
				.setStatusCode(500)
				.end();
				return;
			}

			// 3.b use the generated kubeconfig to check if Liqo is installed in the cluster
			try{
				String checkLiqo = "liqoctl version --kubeconfig " + kubeconfigPath.toString();
				ProcessBuilder checkProcessBuilder = new ProcessBuilder("bash", "-c", checkLiqo);
				Double startTime = System.currentTimeMillis() / 1000.0;
				Process checkProcess = checkProcessBuilder.redirectErrorStream(true).start();
	
				BufferedReader reader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()));
				String line;
				boolean installed = false;
	
				while ((line = reader.readLine()) != null) {
					//System.out.println(line);
					if (line.contains("Server version: " + config().getString("liqo.version"))) {
						installed = true;
						break;
					}
				}
	
				int exitCode = checkProcess.waitFor();
				Double passedTime = (System.currentTimeMillis() / 1000.0) - startTime;
			    System.out.println("@TIMELOG " + templateContext + " TECH " + name + " " + passedTime);
				if( exitCode == 0 && installed){
					System.out.println("Liqo is already installed with the correct version on cluster " + entry.getKey());
				} 
				else {
					System.out.println("START installing procedure to install Liqo " + config().getString("liqo.version") + " on cluster " + entry.getKey());
					/*
					@TODO: to decide the best approach for installing Liqo we need to inspect the cluster and choose the correct configuration
						-> cluster provider may influence the choice:
								1. some provider are natively supported by Liqo and a shortcut for installing Liqo is provided
								2. otherwise we need to manually get the info required
								3. But in both case only certain CNI are supported by Liqo!! If the cluster use a not supported CNI, what happens?
					*/
					String provider = reqProps.getJsonObject("nodeAttrs").getJsonObject(entry.getKey()).getString("provider");
					System.out.println("PROVIDER is " + provider);
	
				}

			} catch(Exception e){
				System.err.println("ERROR: " + e.getMessage());
				e.printStackTrace();
				routingContext.response()
				.setStatusCode(500)
				.end();
				return;
			}
			
			/*
			if(!installLiqoOnCluster()){
				System.err.println("Failed to install Liqo on cluster " + entry.getKey());
				routingContext.response()
				.setStatusCode(500)
				.end();
				return;
			}
			*/

		}

		// 4. Peer the clusters based on the characteristics of the required federation
		// 4.1 check if the cluster are already in a Peering relationships, if not peer them
		// 4.2 in any case, we need to add a label to the node to indicate that it is part of this specific federation too
		// @TODO - la relazione dovrebbe tenere conto della quantità di risorse messe a disposizione da 
		//			ciascun cluster alla federazione! (idealmente)
		System.out.println("LIQO: check the current peer relationships established");
		if(!peerClusters(winner, nodeProps, members, name, templateContext)){
			System.err.println("Failed to complete the peering procedure");
			responseBody.put("status", "ERROR");
			responseBody.put("message", "Liqo Connector: Failed to complete the peering procedure");
			//responseBody.put("details", "startInputs.kubernetes-objects is required but was not found in the request body!");
			routingContext.response()
			.setStatusCode(500)
			.end();
			return;
		}



		// 3. create the shared namespace with the characteristics requested by the federation TOSCA entity
		System.out.println("LIQO: creating offloaded namespace");
		String namespace = createNamespace(winner, nodeProps, members, name, templateContext);
		if(namespace == null){
			System.err.println("Failed to create and offload the namespace representing the Federation");
			responseBody.put("status", "ERROR");
			responseBody.put("message", "Liqo Connector: Failed to create and offload the namespace representing the Federation");
			//responseBody.put("details", "startInputs.kubernetes-objects is required but was not found in the request body!");
			routingContext.response()
			.setStatusCode(500)
			.end();
			return;
		}		

		// 4. store the name of the namespace and the info/name of the entrypoint cluster

		
		JsonObject nodeAttrs = new JsonObject();
		nodeAttrs.put("entrypoint", winner);
		JsonObject capAttrs = new JsonObject();
		JsonObject host = new JsonObject();
		int responseCode = 200;
		
		//id could be the name of the federation as indicated in tosca.nodes.Container.Runtime!
		nodeAttrs.put("id", name);

		// 4. store the name of the namespace and the info/name of the entrypoint cluster
		
        //add host attributes
		/*		String clusterAddress = props.getString("endpoint");
		String ca = props.getString("ca");
		String crt = props.getString("cert");
		String key = props.getString("key"); */
		host.put("endpoint", members.getJsonObject(winner).getString("endpoint"));
		host.put("ca_file", members.getJsonObject(winner).getString("ca_file"));
		host.put("private_key_file", members.getJsonObject(winner).getString("private_key_file"));
		host.put("crt_file", members.getJsonObject(winner).getString("crt_file"));
		host.put("ca", members.getJsonObject(winner).getString("ca"));
		host.put("cert", members.getJsonObject(winner).getString("cert"));
		host.put("key", members.getJsonObject(winner).getString("key"));
		host.put("platform", nodeProps.getString("platform"));
		host.put("entrypoint", winner);
		host.put("namespace", namespace);
		host.put("clusters", clusters);

		capAttrs.put("host", host);
		
		responseBody.put("nodeAttributes", nodeAttrs);
		responseBody.put("capAttributes", capAttrs);

		//System.out.println("createLocation - response is " + responseBody.encode());
		routingContext.response()
	      .setStatusCode(responseCode)
	      .putHeader("content-type", "application/json; charset=utf-8")
	      .end(responseBody.encode());
		
	}

	@Override
	public void checkLocation(RoutingContext routingContext) {
		// Recupero dei parametri della richiesta
		String id = routingContext.request().getParam("id");	
		//System.out.println("checkCluster - received request with id: " + id);
		InstantiateLocation.Status mappedStatus = InstantiateLocation.Status.OK;
		JsonObject responseBody = new JsonObject();
		int responseCode = 200;

		JsonObject nodeAttrs = new JsonObject();
		JsonObject capAttrs = new JsonObject();
		JsonObject host = new JsonObject();
		

		//check that all the peering session are in the established state (conditions in foreignClusters crd)

		// CHECK healthiness

		// set Attributes
		//	ricorda gli attributi della capacità HOST -> tutte quelli normalmente presenti nel Cluster + quelli della Federation se ci sono
		// uso come nome del Namespace condiviso l'ID della Federazione + context dell'applicazione (nome breve del template)

		/* 
		//@TODO
        */

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

	// Utility method to capture process output
    private static void captureOutput(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        //System.out.println(output.toString());
    }

	private boolean generateKubeconfigForCluster(String context, String clusterName, JsonObject props, Path kubeconfigPath){
		return generateKubeconfigForCluster(context, clusterName, props, false, kubeconfigPath);
	}

	private boolean generateKubeconfigForCluster(String context, String clusterName, JsonObject props, Boolean force, Path kubeconfigPath) {
		// if force is false, check if kubeconfig was previously generated. If true skip.
		if(!force){
			if(Files.exists(kubeconfigPath))  return true;
		}

		// generate kubeconfig using the provided information
		String clusterAddress = props.getString("endpoint");
		String ca = props.getString("ca");
		String crt = props.getString("cert");
		String key = props.getString("key");

		Map<String, Object> kubeconfig = new HashMap<>();

		// METADATA
        kubeconfig.put("apiVersion", "v1");
        kubeconfig.put("kind", "Config");
		// SECTION:  CLUSTERS
		HashMap<String, String > clusterDetails = new HashMap<>();
		clusterDetails.put("server", clusterAddress);
		clusterDetails.put("certificate-authority-data", ca);
		HashMap<String, Object > cluster = new HashMap<>();
		cluster.put("name", clusterName);
		cluster.put("cluster", clusterDetails);
        kubeconfig.put("clusters", Arrays.asList(cluster));

		// SECTION:  CONTEXTS
		HashMap<String, String > contextDetails = new HashMap<>();
		contextDetails.put("cluster", clusterName);
		contextDetails.put("user", "torch");
		HashMap<String, Object > kubeContext = new HashMap<>();
		kubeContext.put("name", context);
		kubeContext.put("context", contextDetails);
		kubeconfig.put("contexts", Arrays.asList(kubeContext));
		// CURRENT-CONTEXT
		kubeconfig.put("current-context", context);

		// SECTION:   USERS
		HashMap<String, String > userDetails = new HashMap<>();
		userDetails.put("client-certificate-data", crt);
		userDetails.put("client-key-data", key);
		HashMap<String, Object > user = new HashMap<>();
		user.put("name", "torch");
		user.put("user", userDetails);	
		kubeconfig.put("users", Arrays.asList(user));

		// Save kubeconfig on the filesystem
		//System.out.println(" Generated kubeconfig for cluster " + clusterName);
		//System.out.println(kubeconfig.toString());
		String yamlKubeconfig = Yaml.dump(kubeconfig);
		//System.out.println("YAML version is:\n" + yamlKubeconfig);

		try {
            // Write string to file (overwriting if it exists, creating if it doesn't)
			if(! Files.exists(kubeconfigPath.getParent())){
				Files.createDirectories(kubeconfigPath.getParent());
			}
            Files.write(kubeconfigPath, yamlKubeconfig.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            //System.out.println("File written successfully!");
        } catch (IOException e) {
            e.printStackTrace();
			return false;
        }
        
		return true;
	}

	private boolean peerClusters(String entrypointCluster, JsonObject federationProps , JsonObject members, String id, String templateContext){
		//System.out.println("\n\n\nINSIDE PEER CLUSTERS function\n\n\n");
		//String templateContext = "template-name"; //@TODO
		Path entrypointKubeconfig = Paths.get(config().getString("workdir") + "/liqo/kubeconfigs/" + templateContext + "/" + entrypointCluster + ".yaml");
		Path kubeconfig;
		
		//ProcessBuilder checkProcessBuilder = new ProcessBuilder("bash", "-c", checkLiqo);
		//Process checkProcess = checkProcessBuilder.redirectErrorStream(true).start();

		// retrieve all the foreign clusters known by the entrypointCluster
		String foreignClusters = "";

		try{
			ApiClient client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(entrypointKubeconfig.toString()))).build();
			//client.setDebugging(true);
			Configuration.setDefaultApiClient(client);

			// Create a CustomObjectsApi instance	
    		CustomObjectsApi customObjectsApi = new CustomObjectsApi();
			CoreV1Api api = new CoreV1Api();
			//AppsV1Api appApi = new AppsV1Api();
		
			// Replace with appropriate group, version, and plural
			String group = "core.liqo.io"; // API group of the ForeignCluster
			String version = "v1beta1";             // Version of the CRD
			String plural = "foreignclusters"; // Plural name of the resource
			String namespace = null;          // Use null for cluster-wide resources
		
			// Fetch the list of ForeignCluster objects
			Set<String> knownFC = new HashSet<String>();
			Double startTime = System.currentTimeMillis() / 1000.0;
			Object fcs = customObjectsApi.listClusterCustomObject(
				group,
				version,
				plural,
				null, // pretty print
				null, // allowWatchBookmark
				null, // continue
				null, // fieldSelector
				null, // labelSelector
				null, // limit
				null, // resourceVersion
				null, // resourceVersionMatch
				null,  // timeoutSeconds
				null //
			);
			Double passedTime = (System.currentTimeMillis() / 1000.0) - startTime;
			//System.out.println("@TIMELOG " + templateContext + " TECH " + id + " " + passedTime);
			JSON json = new JSON();
			//System.out.println("listClusterCustomObject returned:\n" + json.serialize(fcs));
			JsonObject tst = new JsonObject(json.serialize(fcs));
			JsonArray items = (new JsonObject(json.serialize(fcs))).getJsonArray("items");

			for(int i = 0; i < items.size(); i++){
				JsonObject obj = items.getJsonObject(i);
				//System.out.println( "OBJECT: " + obj.encode());
				//System.out.println( "CLUSTERID: " + obj.getJsonObject("spec").getString("clusterID"));
				knownFC.add(obj.getJsonObject("spec").getString("clusterID"));
			}

			//@ELISA

			// check if a peering relationship is already established from each cluster to the entrypointCluster
			JsonObject clusters = federationProps.getJsonObject("clusters");
			for(Map.Entry<String, Object> entry : clusters){
				boolean found = false;
				if(entry.getKey().equals(entrypointCluster)) continue;
				
				if(knownFC.contains(entry.getKey())){
					// @TODO, dovremmo accertarci che la sessione sia correttamente configurata per mettere a disposizione il giusto
					//  	quantitativo di risorse... per ora saltiamo questo passaggio
					System.out.println(entry.getKey() + " is already registered as foreign cluster");
					

					// @TODO dovremmo cercare tra i nodi del cluster entrypoint quello che è associato a questo cluster in particolare
					// Il check dovrebbe usare il label "liqo.io/remote-cluster-id" perché non è detto che il nodo si chiami come il cluster

					// add a label to indicate that this cluster is part of this federation
					//String patchLabels = "[{\"op\":\"add\",\"path\":\"/spec/template/spec/terminationGracePeriodSeconds\",\"value\":27}]";
					System.out.println( "adding label to node " + entry.getKey() + " of the entrypoint cluster");
					String labelName = templateContext + "-" + id;
					String labelValue = entry.getKey();
					String patchLabels = "[{\"op\":\"add\",\"path\":\"/metadata/labels/" + labelName + "\",\"value\":\"" + labelValue + "\"}]";
					//() -> api.patchNode(entry.getKey(), new V1Patch(patchLabels), null, null, null, null, null),
					 
					//V1Patch patch = new V1Patch(patchLabels);
					
					//V1Node node = api.patchNode(entry.getKey(), patch, null, null, null, null, null);
					startTime = System.currentTimeMillis() / 1000.0;
					V1Node node = PatchUtils.patch(
											V1Node.class, 
											() -> api.patchNodeCall(entry.getKey(), new V1Patch(patchLabels),null, null, null, null, null,null), 
											V1Patch.PATCH_FORMAT_JSON_PATCH
											); 
					passedTime = passedTime + ((System.currentTimeMillis() / 1000.0) - startTime);
					//System.out.println(node.toString());
					//PatchUtils.patch(null, null, patchLabels);
					
				} else {
					// @TODO set up a peering session between this cluster (as provider) and the entrypoint cluster (as consumer)
					System.out.println("Cluster " + entry.getKey() + " has no peering session active (as a Provider) with the entrypoint Cluster " + entrypointCluster);
					System.out.println("Establishing peering session: " + entry.getKey() + " -> " + entrypointCluster);

					kubeconfig = Paths.get(config().getString("workdir") + "/liqo/kubeconfigs/" + templateContext + "/" + entry.getKey() + ".yaml");
				
					String establishSession = "liqoctl peer --remote-kubeconfig \"" + kubeconfig.toString() + "\" --server-service-type NodePort --kubeconfig " + entrypointKubeconfig;
					System.out.println(establishSession);
				}				
	
				System.out.println("@TIMELOG " + templateContext + " TECH " + id + " " + passedTime);
			}
		} catch (ApiException ae) {
			ae.printStackTrace();
			System.out.println("message is :");
			System.out.println(ae.getMessage());
			System.out.println("response is :");
			System.out.println(ae.getResponseBody());
			return false;
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	private String createNamespace(String entrypointCluster, JsonObject federationProps , JsonObject members, String id, String templateContext){
		//System.out.println("\n\n\nINSIDE CREATE NAMESPACE function\n\n\n");
		//String templateContext = "template-name"; //@TODO
		String offloadNamespace = templateContext + "-" + id;

		
		Path entrypointKubeconfig = Paths.get(config().getString("workdir") + "/liqo/kubeconfigs/" + templateContext + "/" + entrypointCluster + ".yaml");

		try{
			// 1. create the namespace
			ApiClient client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(entrypointKubeconfig.toString()))).build();
			//client.setDebugging(true);
			Configuration.setDefaultApiClient(client);
			CoreV1Api api = new CoreV1Api();
			Double startTime = System.currentTimeMillis() / 1000.0;
			V1Namespace ns = api.createNamespace(new V1Namespace().metadata(new V1ObjectMeta().name(offloadNamespace)), null, null, null, null);
			Double passedTime = (System.currentTimeMillis() / 1000.0) - startTime;
			//System.out.println("created namespace " + offloadNamespace + "\n" + ns.toString());

			// 2. offload the namespace
			String offloadCommand = 
                "liqoctl offload namespace " + offloadNamespace + 
					" --namespace-mapping-strategy EnforceSameName " + 
					" --pod-offloading-strategy LocalAndRemote " +
					" --selector '" + offloadNamespace + "' " +
					" --kubeconfig " + entrypointKubeconfig.toString();

            ProcessBuilder offloadProcessBuilder = new ProcessBuilder("bash", "-c", offloadCommand);
			startTime = System.currentTimeMillis() / 1000.0;
            Process offloadProcess = offloadProcessBuilder.redirectErrorStream(true).start();

            // Capture curl output
            captureOutput(offloadProcess);

            int curlExitCode = offloadProcess.waitFor();
			passedTime = passedTime + ((System.currentTimeMillis() / 1000.0) - startTime);
            if (curlExitCode != 0) {
                System.err.println("Failed to download liqoctl with command " + offloadCommand);
                throw new Exception("Failed to download liqoctl with command " + offloadCommand);
            }

			System.out.println("[LIQO connector] Liqoctl namespace offloading completed with success!");
			System.out.println("@TIMELOG " + templateContext + " TECH " + id + " " + passedTime);

		} catch (ApiException ae) {
			ae.printStackTrace();
			System.out.println("message is :");
			System.out.println(ae.getMessage());
			System.out.println("response is :");
			System.out.println(ae.getResponseBody());
			return null;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}

		

		return offloadNamespace;
	}


}
