package it.unict.vertx.esb.common;

//import it.unict.vertx.esb.common.translator.utils.DeploymentUnit;
import torch.javaModel.model.DeploymentNode;

public interface TranslatorPlugin {
    String translateDu(DeploymentNode du);
}
