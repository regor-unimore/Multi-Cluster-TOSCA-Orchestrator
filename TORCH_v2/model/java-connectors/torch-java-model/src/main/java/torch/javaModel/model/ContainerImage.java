package torch.javaModel.model;

import java.io.Serializable;

public class ContainerImage implements Serializable {
        
    private String file;
    private String repository;

    public ContainerImage() {}

    public ContainerImage( String file, String repository){
        this.file = file;
        this.repository = repository;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    
        
}
