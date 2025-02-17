package it.unict.vertx.esb.instantiatevolumekubernetes;

import it.unict.vertx.esb.common.TranslatorPlugin;
import it.unict.vertx.esb.common.translator.utils.DeploymentUnit;
import it.unict.vertx.esb.common.translator.utils.Volume;
import torch.javaModel.model.DeploymentNode;
import torch.javaModel.model.Container;
import torch.javaModel.model.ContainerImage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.util.List;

import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1LabelSelector;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServicePort;
import io.kubernetes.client.openapi.models.V1ServiceSpec;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerPort;
import io.kubernetes.client.util.Yaml;
import io.kubernetes.client.common.KubernetesType;
import io.kubernetes.client.custom.IntOrString;
import io.vertx.core.json.JsonObject;

public class K8sTranslatorPlugin implements TranslatorPlugin {
    
    @Override
    public String translateDu(DeploymentNode du) {
        String context = "template-name";
        Map<Integer, Map<String, String>> ports = new HashMap<>();
        Map<String,String> labels = new HashMap<>();
        labels.put("app", du.getName());
        labels.put("context", context);
        
        //@ELISA
        System.out.println("INSIDE TranslateDU java service");
        // Parse each capabilities
        for(Map.Entry<String, Map<String,Object> > cap : du.getCapProperties().entrySet()){
            Map<String, Object> outputs = parseCapability(cap);
            if(outputs == null){
                System.out.println("outputs for cap " + cap.getKey() + " is NULL :( ");
            } else {
                if(outputs.containsKey("ports")){
                    System.out.println("outputs non è null e contiene la key 'ports'");
                    for(Map.Entry<Integer, Object> p : ((Map<Integer,Object>) outputs.get("ports")).entrySet()){
                        System.out.println("port : " + Integer.toString(p.getKey()));
                        ports.put(p.getKey(), (Map<String,String>) p.getValue());
                    }
                    //ports.putAll( (Map<Integer, Map<String, String>>) outputs.get("ports"));
                } 
            }
            //@TODO
        }

        //@ELISA
        System.out.println("PORTS is " + ports.toString());

        // ---------------- DEPLOYMENT -----------------------//

        
        V1Deployment deployment = new V1Deployment();
        deployment.apiVersion("apps/v1").kind("Deployment");
        deployment.setMetadata( new V1ObjectMeta()
                    .name(du.getName().replaceAll("_", "-") + "-deployment")
                    .putLabelsItem("app", du.getName())
                    .putLabelsItem("context", context)
                    );
        
        V1PodTemplateSpec pod = new V1PodTemplateSpec();
        pod.setMetadata(new V1ObjectMeta()
                        .putLabelsItem("app", du.getName())
                        .putLabelsItem("context", context)
                        );
        
        for(Container c : du.getContainers()){
            List<V1EnvVar> env = new ArrayList<>();

            //PARSE ENVS
            for(Map.Entry<String, String> entry : du.getEnvs().entrySet()){
                env.add(new V1EnvVar().name(entry.getKey()).value(entry.getValue()));
            }

            //PARSE PORTS
            List<V1ContainerPort> container_ports = new ArrayList<>();
            if(!c.getPorts().isEmpty()){
                System.out.println("ports defined in the container definition - IGNORE FOR NOW");
            } else if (c.getName() == du.getName()){
                //we can use the ports parsed from the capabilities of the node, but only for the "MAIN" container
                //  which is by convention the one named after the node/du
                for(Integer i : ports.keySet()){
                    container_ports.add(new V1ContainerPort()
                            .containerPort(i)
                            .protocol( ( (Map<String,String>)ports.get(i) ).get("svc-protocol"))
                            .name( ( (Map<String,String>)ports.get(i)).get("protocol") + "-" + Integer.toString(i))
                            );
                }
            }

            System.out.println("container_ports is " + container_ports.toString());

            //PARSE COMMAND

            List<String> args = c.getArgs();

            V1Container container = new V1Container().name(c.getName().replaceAll("_", "-"));
            container.env(env);
            container.ports(container_ports);
            //@TODO handle privare repository different from docker hub !
            container.image(c.getImage().getFile());
            if(pod.getSpec() == null){
                pod.spec(new V1PodSpec().addContainersItem(container));
            } else {
                pod.getSpec().addContainersItem(container);
            }
            
            //PARSE COMMAND AND ARGS
            String command = c.getCommand();
            if(! command.isEmpty()){
                List<String> entrypoint = new ArrayList<String>();
                entrypoint.add(command);
                container.command(entrypoint);
                System.out.println("ENTRYPOINT IS " + entrypoint.toString());
                args = c.getArgs();
                if(!args.isEmpty()){
                    List<String> correct_args = new ArrayList<String>();
                    for(int i = 0; i < args.size(); i++ ){
                        if(i >= 2){
                            correct_args.add( "$(" + args.get(i).replace("$","") + ")");
                        } else {
                            correct_args.add(args.get(i));
                        }
                    }
                    container.args(correct_args);
                }
            }


            /* 
            pod.getSpec().addContainersItem( new V1Container()
                    .name(c.getName())
                    .env(env)
                    .ports(container_ports)
                    );
            */
            System.out.println("container is " + container.toString());
        }
           
        //retrieve replicas number from reqProperties
        // DEFAULT IS 1, if no relationship was explicitly declared
        Integer replicas = 1;
        if(du.getReqProperties().containsKey("location") &&
            du.getReqProperties().get("location").containsKey("replicas")) {            
                replicas = (Integer) du.getReqProperties().get("location").get("replicas");
        }
        
        System.out.println("replicas set to " + replicas);

        deployment.setSpec ( new V1DeploymentSpec()
                    .replicas(replicas)
                    .selector( new V1LabelSelector()
                            .matchLabels(labels)
                            )
                    .template(pod)
                    );

        List<KubernetesType> objs = new ArrayList<>();
        objs.add(deployment);
        String deploymentString = Yaml.dump(deployment);
        System.out.println("deployment is:\n" + deploymentString);
        //@ELISA

        // ---------------- SERVICE -----------------------//
        List<V1ServicePort> internals = new ArrayList<>();
        List<V1ServicePort>externals = new ArrayList<>();

        for(Map.Entry<Integer, Map<String, String> > port : ports.entrySet()){
            if(port.getValue().containsKey("scope")){
                System.out.println(port.getValue().get("scope"));
                if(port.getValue().get("scope").equals("PRIVATE")){
                    System.out.println("PRIVATE FOUND!!!");
                    internals.add(new V1ServicePort()
                                    .port(port.getKey())
                                    .targetPort(new IntOrString(port.getKey()))
                                    .protocol(port.getValue().get("svc-protocol"))
                                    .name(port.getValue().get("protocol") + "-" + Integer.toString(port.getKey()))
                                    );
                }
                else if(port.getValue().get("scope").equals("PUBLIC")){
                    externals.add(new V1ServicePort()
                                    .port(port.getKey())
                                    .targetPort(new IntOrString(port.getKey()))
                                    .protocol(port.getValue().get("svc-protocol"))
                                    .name(port.getValue().get("protocol") + "-" + Integer.toString(port.getKey()))
                    );
                }
            }
        }
        
        System.out.println("internals is " + internals.toString() + "\n externals is " + externals.toString());

        List<V1Service> services = new ArrayList<>();
        if(! internals.isEmpty()) {
            //create ClusterIP service
            V1ServiceSpec service_spec = new V1ServiceSpec()
                                            .selector(labels)
                                            .ports(internals);

            V1Service service = new V1Service()
                                    .apiVersion("v1").kind("Service")
                                    .metadata( new V1ObjectMeta().name(du.getName().replaceAll("_", "-") + "-private-service"))
                                    .spec(service_spec);
                                    
            services.add(service);
        }
        if(! externals.isEmpty()) {
            //create ClusterIP service
            V1ServiceSpec service_spec = new V1ServiceSpec()
                                            .selector(labels)
                                            .type("NodePort")
                                            .ports(externals);

            V1Service service = new V1Service()
                                    .apiVersion("v1").kind("Service")
                                    .metadata( new V1ObjectMeta().name(du.getName().replaceAll("_", "-") + "-nodeport-service"))
                                    .spec(service_spec);
            services.add(service);
        }

        //Stringify ???? ŢODO
        
        for(V1Service s : services){
            try {
                String service = Yaml.dump(services);
                System.out.println("service is:\n" + service);
                objs.add(s);
            } catch (Exception e) {
                System.out.println("ERROR: could not translate Service " + s.getMetadata().getName() +" to YAML!");
            }
            
        }
        
        //@TODO relegare la logica di aggiunta/raffinamento degli oggetti in base a capacità a requisiti qui :)

        //update objs based on the du requirements
        objs = updateWithRequirements(objs, du);

        System.out.println("DU was translated in the following string:\n\n" + Yaml.dumpAll(objs.iterator()));
        return Yaml.dumpAll(objs.iterator());

    }

    private Map<String, Object> parseCapability(Entry<String, Map<String, Object>> cap) {
        Map<String, Object> outputs = new HashMap<>();
        Map<String, Object> properties = cap.getValue();
        String type = (String) properties.get("type");
        System.out.println("PARSE_CAP: inspecting cap " + cap.getKey() + " with type " + type);

        switch(type) {
            case "torch.capabilities.Endpoint.Http" :
                System.out.println("parsing with parseEndpointHTTP!");
                return parseEndpointHTTP(cap);
                    
        }
        return outputs;
    }


    private Map<String, Object> parseEndpointHTTP(Entry<String, Map<String, Object>> cap) {
        // TODO Auto-generated method stub
        Map<String, Object> outputs = new HashMap<>();
        Map<String, String> port_info = new HashMap<>();
        Integer port = null;
        System.out.println("INSIDE parseEndpointHTTP");
        for(Map.Entry<String, Object> info : cap.getValue().entrySet()){
            switch(info.getKey()){
                case "port"         : port = (Integer) info.getValue(); break;
                case "network_name" : port_info.put("scope", (String) info.getValue()); break;
                case "protocol"     : port_info.put("protocol", ((String) info.getValue()).toLowerCase() );
                                      port_info.put("svc-protocol", "TCP"); break;
            }
        }
        if(!port_info.containsKey("protocol")){
            port_info.put("protocol", "http");
            port_info.put("svc-protocol", "TCP");
        }
        System.out.println("port_info is ");
        port_info.forEach( (key, value) -> System.out.println(key + ":" + value));
        if(!port_info.isEmpty() && port != null){
            Map<Integer, Object> tmp = new HashMap<>();
            tmp.put(port, port_info);
            outputs.put("ports", tmp);
            System.out.println("added PORTS to outputs map and port " + Integer.toString(port));
        }

        System.out.println("END parseEndpointHTTP");
        return outputs;
    }
    
    private List<KubernetesType> updateWithRequirements(List<KubernetesType> objs, DeploymentNode du){
        // "relationships":{"location":{"cap_type":"torch.capabilities.Location.Container","rel_type":"tosca.relationships.HostedOn","capability":"host","target":"cluster1"}}
        // reqProperties":{"location":{"ca_file":"/usr/app/service-binding/instantiate-k8s-cluster-manual/src/main/resources/ca.crt","private_key_file":"/usr/app/service-bi
        //     ---> sostituire location a host in reqProperties
        
        return objs;
    }
    
    /* 
    private String translateVolume(Volume v)
    {
        String s = "apiVersion: v1\n" +
                "kind: PersistentVolumeClaim\n" +
                "metadata:\n" +
                "  name: "+ v.getName().replaceAll("_","-").concat("-claim") + "\n" +
                "  labels:\n" +
                "    app: kubernetes-application\n" +
                "spec:\n" +
                "  accessModes:\n" +
                "    - ReadWriteOnce\n" +
                "  resources:\n" +
                "    requests:\n";
        String storage = null;
        for (Map.Entry<String,String> entry : v.getProperties().entrySet())
        {
            if(entry.getKey().compareToIgnoreCase("size") == 0)
                storage = entry.getValue().replaceAll("[b|B]", "");
        }
        if( storage == null)
            s = s.concat("      storage: 1G\n");
        else
            s = s.concat("      storage: " + storage.replaceAll(" ", "") + "\n");
        s = s.concat("---\n");
        return s;
    }
    */
    /* 
    private String translateContainer(Container c)
    {
        String targetPort = null;    	
    	
        String s = "      - image: "+ c.getImage() +"\n" +
                "        name: " + c.getName().replaceAll("_", "-") +"\n" +
                "        env:\n";
        String port = null;
        // using for-each loop for iteration over Map.entrySet()
        for (Map.Entry<String,String> entry : c.getProperties().entrySet())
        {
            if(entry.getKey().compareToIgnoreCase("port") == 0)
            {
                port = entry.getValue();
                String[] ports = port.split(":");
                targetPort = ports[1];                
                continue;
            }
            String value = entry.getValue();
            if (value.equals("true")) value = "\"true\"";
            if (value.equals("false")) value = "\"false\"";
//            s = s.concat("        - name: " + entry.getKey() + "\n" + //Removed: toUpperCase()
//                    "          value: " + value  + "\n"
//            );
            s = s.concat("        - name: " + entry.getKey().toUpperCase() + "\n" +
                    "          value: " + value  + "\n"
            );     
        }
        // DEPRECATED        
//        for (Map.Entry<String,String> entry : c.getExt_requirements().entrySet())
//        {
//            s = s.concat("        - name: " + entry.getKey().toUpperCase() + "\n" +
//                    "          value: " + entry.getValue().replaceAll("_", "-").concat("-service") + "\n"
//            );
//        }
        if (targetPort != null)
        {
            String portName = c.getName().replaceAll("_", "-");
            if (portName.length() > 15)
                portName = portName.substring(0, 15);
            s = s.concat("        ports:\n" +
                    "        - containerPort: "+ targetPort + "\n" +
                    "          name: " +  portName + "\n"
            );
        }
        for (Volume v : c.getVolumes())
        {
            s = s.concat("        volumeMounts:\n" +
                    "        - name: " + c.getName().replaceAll("_", "-").concat("-persistent-storage") + "\n");
            for (Map.Entry<String,String> entry : v.getProperties().entrySet())
            {
                if(entry.getKey().compareToIgnoreCase("location") == 0)
                {
                    s = s.concat("          mountPath: " + entry.getValue() + "\n");
                }
            }
        }
        for (Volume v : c.getVolumes())
        {
            s = s.concat(                "      volumes:\n" +
                    "      - name: " + c.getName().replaceAll("_", "-").concat("-persistent-storage") +"\n" +
                    "        emptyDir:\n" +
                    "          medium: Memory\n");
            // TODO: for true volumes
            //"        persistentVolumeClaim:\n" +
            //"          claimName: " + v.getName().replaceAll("_","-").concat("-claim") + "\n");
        }
        return s;
    }
        */
}
