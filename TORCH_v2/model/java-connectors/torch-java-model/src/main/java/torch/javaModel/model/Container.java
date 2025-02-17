package torch.javaModel.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Container implements Serializable {
    
    private String name;
    private ContainerImage image;
    private String configuration_script;
    private List<Integer> ports = new ArrayList<Integer>();
    private String command = "";
    private List<String> args = new ArrayList<String>();
    
    public Container() {}
    
    public Container(String name, ContainerImage image, String configuration_script, List<Integer> ports, String command, List<String> args ) {
        this.name = name;
        this.image = image;
        this.configuration_script = configuration_script;
        this.ports = ports;
        this.command = command;
        this.args = args;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public ContainerImage getImage() {
        return image;
    }
    public void setImage(ContainerImage image) {
        this.image = image;
    }
    public String getConfiguration_script() {
        return configuration_script;
    }
    public void setConfiguration_script(String configuration_script) {
        this.configuration_script = configuration_script;
    }

    public List<Integer> getPorts() {
        return ports;
    }

    public void setPorts(List<Integer> ports) {
        this.ports = ports;
    }

    public String getCommand(){
        return command;
    } 

    public void setCommand(String command){
        this.command = command;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args){
        this.args = args;
    }

    
}
