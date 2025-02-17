package it.unibo.vertx.esb.instantiateliqofederation;
import io.vertx.core.DeploymentOptions;
import it.unict.vertx.esb.common.MicroServiceVerticle;

public class InstantiateLiqoFederationVerticle extends MicroServiceVerticle {

	@Override
	  public void start() {
	    super.start();
	    
	    // Deploy the verticle with a configuration.
	    vertx.deployVerticle(InstantiateLiqoFederationAPIVerticle.class.getName(), new DeploymentOptions().setConfig(config()));

	    // Publish the services in the discovery infrastructure.
//	    publishHttpEndpoint("instantiate-k8s-cluster-manual", config().getString("host", "localhost"), config().getInteger("http.port", 8080), ar -> {
	    publishHttpEndpoint("instantiate-federation-liqo", config().getString("host", "localhost"), config().getInteger("http.port", 8080), ar -> {	    
	      if (ar.failed()) {
	        ar.cause().printStackTrace();
	      } else {
	        System.out.println("Instantiate Liqo Federation (Rest endpoint) published : " + ar.succeeded());
	      }
	    });
	  }

}
