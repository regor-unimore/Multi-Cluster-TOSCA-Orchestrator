@extends('layouts.app')

@section('content')
<div class="container">
    <div class="row">
        
        <div class="col-md-8">
            <div class="card">
                <div class="card-header">
                    {{ $template->name }}
                </div>
                <div class="card-body" style="height: 80vh">
                    <tosca-graph json_graph="{{$template->json_graph}}"></tosca-graph>            
                </div>
            </div>
        
        </div>
        <div class="col-md-4">
            <div class="card">
                <div class="card-header">
                    Control Panel
                </div>
                <div class="card-body">
                        @if ($template->active)
                        <script>
                        setTimeout(function() {
                            location.reload();
                        }, 30000);
                        </script>
                        <!-- TODO: add some fancy graphics-->
                        <table class="table table-striped">
                            <thead>
                                <tr>
                                    <!-- <th scope="col">Deployment Unit</th> -->
				    <th scope="col">TOSCA Node</th>
                                    <th scope="col">Status</th>
                                </tr>
                            </thead>
                            <tbody>
                            @foreach($template_info as $node => $node_info)
                                <tr>
                                <td>{{$node}}</td>
                                <td>{{($node_info->nodeAttributes)->status}}</td>
                                </tr>
                            @endforeach   
                            </tbody>
                        </table>
                        @else
                        <div class="container">
                            <form method="POST" action="{{ route('bpmn-engine') }}">
                                @csrf
                                <!--
                                <div class="form-group row">
                                    <label for="cloudProvider" class="col-md-6 col-form-label">Cloud Provider</label>
                                    <div class="col-md-6">
                                        <div class="form-check form-check-inline">
                                            <input class="form-check-input" type="radio" name="cloudProvider" id="azure" value="azure" required>
                                            <label class="form-check-label" for="azure">Azure</label>
                                        </div>
                                        <div class="form-check form-check-inline">
                                            <input class="form-check-input" type="radio" name="cloudProvider" id="manual" value="manual" required>
                                            <label class="form-check-label" for="manual">Manual</label>
                                        </div>
                                        <div class="form-check form-check-inline">
                                            <input class="form-check-input" type="radio" name="cloudProvider" id="openstack" value="openstack" required>
                                            <label class="form-check-label" for="swarm">OpenStack</label>
                                        </div>
                                    </div>
                                </div>
                                <div class="form-group row">
                                    <label for="clusterPlatform" class="col-md-6 col-form-label">Cluster Platform</label>
                                    <div class="col-md-6">
                                        <div class="form-check form-check-inline">
                                            <input class="form-check-input" type="radio" name="clusterPlatform" id="kubernetes" value="kubernetes">
                                            <label class="form-check-label" for="kubernetes">Kubernetes</label>
                                        </div>
                                        <div class="form-check form-check-inline">
                                            <input class="form-check-input" type="radio" name="clusterPlatform" id="swarm" value="swarm">
                                            <label class="form-check-label" for="swarm">Swarm</label>
                                        </div>
                                    </div>
                                </div> -->
                                <div class="form-group row">
                                    <label for="bpmnEngineEndpoint" class="col-md-6 col-form-label">BPMN Engine Endpoint </label>
                                    <div class="col-md-6">
                                    <input type="text" class="form-control" name="bpmnEngineEndpoint" id="bpmnEngineEndpoint" value="{{ old('bpmnEngineEndpoint')? old('bpmnEngineEndpoint') : env('BPMN_ENGINE') }}">
                                    </div>
                                </div>
                                <div class="form-group row">
                                    <label for="serviceBrokerEndpoint" class="col-md-6 col-form-label">Service Broker Endpoint </label>
                                    <div class="col-md-6">
                                    <input type="text" class="form-control" name="serviceBrokerEndpoint" id="serviceBrokerEndpoint" value="{{ old('serviceBrokerEndpoint')? old('serviceBrokerEndpoint') : env('SERVICE_BROKER_URI') }}">
                                    </div>
                                </div>
                                <div class="form-group row">
                                    <label for="retryCounter" class="col-md-6 col-form-label">Retry counter </label>
                                    <div class="col-md-6">
                                    <input type="text" class="form-control" name="retryCounter" id="retryCounter" value="{{ old('retryCounter')? old('retryCounter') : 1 }}">
                                    </div>
                                </div>
                                <div class="form-group row">
                                    <label for="checkPeriod" class="col-md-6 col-form-label">Check period </label>
                                    <div class="col-md-6">
                                    <input type="text" class="form-control" name="checkPeriod" id="checkPeriod" value="{{ old('checkPeriod')? old('checkPeriod') : 30 }}">
                                        <small>seconds</small>
                                    </div>
                                </div>
                                <div class="form-group row">
                                    <label for="createTimeout" class="col-md-6 col-form-label">Create timeout </label>
                                    <div class="col-md-6">
                                        <input type="text" class="form-control" name="createTimeout" id="createTimeout" value="{{ old('createTimeout')? old('createTimeout') : 30}}">
                                        <small>minutes</small>
                                    </div>
                                </div>
                                <div class="form-group row">
                                    <div class="col-md-12">
                                        <button type="submit" class="float-right btn btn-primary">Deploy Scheme</button>
                                    </div>
                                </div>
                                <input type="hidden" name="templateID" value="{{$template->id}}" />
                            </form>
                        </div>
                        @endif
                        @if (session('error'))
                            <div class="container">
                                <div class="alert alert-danger">
                                    <strong>Error!</strong> It was impossible to deploy the scheme.
                                    <br>
                                    {{session('error') == TRUE? "" : session('error')}}
                                </div>
                            </div>
                        @endif
                    </div>
            </div>
      </div>
    </div>
    @if ($template->active)
    <div class="row" style="margin-top: 15px">
        <div class="col-12">
            <div class="card">
                <div class="card-header">
                Attributes of Nodes whose deployment has ended:
                </div>
                <div class="card-body" style="height: 80vh; overflow-y: auto">
                @foreach($template_info as $node => $node_info)
                              @if (($node_info->nodeAttributes)->status == 'STARTED')
                              <!--<div class="form-group row"> 
                                 <label class="col-md-6 col-form-label">{{$node}}</label>
                                 <div class="col-md-6">-->
                              <div class="div-attributes">
                                 <h5>{{$node}}</h5>
                                 <div class="div-attributes-1" style="margin-left: 15px">
                                    <p>NODE PROPERTIES:</p>
                                    @foreach($node_info->nodeAttributes as $name => $value)
                                       <div class="div-attribute-node" style="margin-left: 30px">
                                          <p><span>{{$name}}</span><span>: </span><span>{{json_encode($value)}}</span></p>
                                       </div>
                                    @endforeach
                                 </div>
                                 <div class="div-attributes-2" style="margin-left: 15px">
                                    @if (property_exists($node_info, "capAttributes"))
                                       @foreach($node_info->capAttributes as $cap_name => $cap_value)
                                          <p>{{strtoupper($cap_name)}}:</p>
                                             @foreach($cap_value as $name => $value)
                                                <div class="div-attribute-cap" style="margin-left: 30px">
                                                   @if (strlen(json_encode($value)) > 256)
                                                   <p><span>{{$name}}</span><span>: </span><span>{{substr(json_encode($value, JSON_UNESCAPED_SLASHES), 0, 255)}} ...</span></p>
                                                   @else
                                                   <p><span>{{$name}}</span><span>: </span><span>{{json_encode($value, JSON_UNESCAPED_SLASHES)}}</span></p>
                                                   @endif
                                                </div>
                                             @endforeach
                                       @endforeach
                                    @endif
                                 </div>
                              </div>
                              @elseif (($node_info->nodeAttributes)->status == 'ERROR')
                              <div class="div-attributes">
                                 <h5>{{$node}}</h5>
                                 <div class="div-attributes-1">
                                    <p>ERROR REASON:</p>
                                       <div class="div-attribute-node">
                                          <p><span>{{($node_info->nodeAttributes)->status_reason}}</span></p>
                                       </div>
                                 </div>
                              </div>
                              @endif 
                           @endforeach           
                </div>
            </div>
        </div>
    </div>
    @endif
</div>
@endsection
