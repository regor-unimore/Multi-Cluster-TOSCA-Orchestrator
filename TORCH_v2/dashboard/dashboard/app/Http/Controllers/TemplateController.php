<?php

namespace App\Http\Controllers;

use App\Template;
use Illuminate\Http\Request;
use App\User;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\File;
use Illuminate\Support\Facades\Storage;
use Illuminate\Contracts\Filesystem\FileNotFoundException; 


use Symfony\Component\Process\Process;
use Symfony\Component\Process\Exception\ProcessFailedException;

class TemplateController extends Controller
{
    /**
     * Display a listing of the resource.
     *
     * @return \Illuminate\Http\Response
     */
    public function index()
    {
        //
    }

    /**
     * Show the form for creating a new resource.
     *
     * @return \Illuminate\Http\Response
     */
    public function create($id)
    {
        $user = User::findOrFail($id);
        return view('templates.create', [
            'user' => $user
        ]);
    }

    /**
     * Show the graphical modeller
     * 
     *  @return View
     */
    public function createWithModeler()
    {
        return view('templates.modeler');
    }

    /**
     * Store a newly created resource in storage.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return \Illuminate\Http\Response
     */
    public function validateTemplate(Request $request)
    {
        $base64_template = base64_encode(file_get_contents($request->yaml_template));
        
    	//retrieve and store the pre-processed original TOSCA file
        $user = Auth::user(); // Get the authenticated user
        $file = $request->file('yaml_template');
        $extension = $file->getClientOriginalExtension();
        $id = uniqid();
	$filename = $id . ".$extension";
        $path = $file->storeAs("users/{$user->id}", $filename); //Saves in storage/app/users/{user_id}
        $absolute_path = storage_path("app/$path");
        
        $tosca_tmpl = null;
        if($file->getClientOriginalExtension() === 'zip') {
           //error_log("The file to be validated is a zip");
           $tosca_tmpl = $base64_template;
        }
        else {
           //$tosca_tmpl = Storage::url($path);
           $tosca_tmpl = $absolute_path;
           error_log("tosca_tmp is ".$tosca_tmpl);
        }
        
        

        $process = new Process("python3 ./json4tosca-parser/tosca_parser.py --template-name=".$request->name." --template-file=".$tosca_tmpl);
        $startTime = microtime(true);
        $process->run();
        $endTime = microtime(true);
        $executionTime = $endTime - $startTime;
        $ouput_json = $process->getOutput();
	$error_output = $process->getErrorOutput(); //@DEBUG
        
        if(strcmp($ouput_json,"") == 0)
        {
            Storage::delete($path);
            return redirect()->back()->with('templateIsValid', FALSE)->with("error", "Invalid template submitted: ".$error_output)->withInput();
        }
        else
        {
	    //@DEBUG
	    error_log("@TIMELOG ".$request->name." ".number_format($executionTime, 4));
            error_log("JSON OUTPUT\n".$ouput_json);
            $json_graph = json_decode($ouput_json)->objects;
            $template_inputs = json_decode($ouput_json)->inputs;
            
            return redirect()->back()->with('templateIsValid', TRUE)->with('json_graph', json_encode($json_graph))->with('template_inputs', json_encode($template_inputs))->with('yaml_template', json_encode($tosca_tmpl))->withInput();
        }
        // try {

        //     $data = File::get('output.json');
        //     dd($data);
        //     $request->json_graph = $data;
        //     unlink(public_path()."/output.json");
            
        //     return redirect()->back()->with('templateIsValid', TRUE)->with('json_graph', $data)->with('yaml_template', $filename)->withInput();
        // }
        // catch (FileNotFoundException $e)
        // {
        //     return redirect()->back()->with('templateIsValid', FALSE)->with("error", "Invalid template submitted")->withInput();
        // }
        
    }

    /**
     * Store a newly created resource in storage.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return \Illuminate\Http\Response
     */
    public function store(Request $request)
    {        
        $template = Template::create([
            'name' => $request->name, 
            'user_id' => Auth::id(), 
            'yaml_template' => $request->yaml_template, 
            'json_graph' => $request->json_graph
        ]);
        return redirect('home');
    }

    /**
     * Display the specified resource.
     *
     * @param  \App\Template  $template
     * @return \Illuminate\Http\Response
     */
    public function show(User $user, Template $template)
    {
        if (strcmp($template->process_id,"")==0) $template_info = '';
        else {
	    /********* TO DO: AGGIORNARE BPMN ENDPOINT  *********/
            //$endpoint = env("BPMN_ENGINE", "");
	    $endpoint = $template->bpmn_endpoint;

            // TODO: cycle directly on history with size > 100 so that you find all DUs with their state
            $curl = curl_init();
        
            curl_setopt_array($curl, array(
                CURLOPT_URL => $endpoint."/flowable-rest/service/history/historic-process-instances?superProcessInstanceId=".$template->process_id."&includeProcessVariables=true&size=1000",
                CURLOPT_RETURNTRANSFER => true,
                CURLOPT_ENCODING => "",
                CURLOPT_TIMEOUT => 30000,
                CURLOPT_USERPWD => "rest-admin:test",
                CURLOPT_HTTP_VERSION => CURL_HTTP_VERSION_1_1,
                CURLOPT_CUSTOMREQUEST => "GET",
                CURLOPT_HTTPHEADER => array(
                    // Set Here Your Requested Headers
                    'Content-Type: application/json',
                ),
            ));
            $response = curl_exec($curl);
            $err = curl_error($curl);
            curl_close($curl);
            if ($err) {
                $template_info = "cURL Error #:" . $err;
            } else {
                $template_info = array();
                //error_log("-------PROCESS HISTORY is : ------- \n".$response);
                $response = json_decode($response);
                $json_graph = json_decode($template->json_graph);
                
                foreach($response->data as $process){
                   //$torch_node = (object)[];
                   
                   foreach($json_graph as $jnode){
                      $node = $jnode->node;
                      if( strcmp($node->name, $process->name) == 0){
                         $node_info = (object)[];
                         $node_info->nodeAttributes = (object)[];
                         ($node_info->nodeAttributes)->status = "WAITING";
                         //$node_info->cap_attrs = (object)[];
                         
                         foreach($process->variables as $variable){
                            if( strcmp($variable->name, "nodeInfo") == 0 ){
                               //error_log("\n-------NODE ".$node->name." INFO: ------- \n".json_encode($variable));
                               $var_value = json_decode($variable->value);
                               if( !(isset($var_value->nodeAttributes)) || !(isset( ($var_value->nodeAttributes)->status))){
                                  ($node_info->nodeAttributes)->status = "ERROR";
                                  ($node_info->nodeAttributes)->status_reason = "STATUS unavailable";
                               } else{
                                  $node_info->nodeAttributes = $var_value->nodeAttributes;
                                  if( isset($var_value->capAttributes)){
                                     $node_info->capAttributes = $var_value->capAttributes;
                                  }
                               }
                               break;
                            }
                         }
                         $template_info[$node->name] = $node_info;
                         break;
                      }
                   }
                }
                //error_log("\n-------TEMPLATE INFO is: ------- \n".json_encode($template_info));
            }
        }
        return view('template', [
            'template' => $template,
            'template_info' => $template_info
        ]);
    }

    /**
     * Show the form for editing the specified resource.
     *
     * @param  \App\Template  $template
     * @return \Illuminate\Http\Response
     */
    public function edit(Template $template)
    {
        //
    }

    /**
     * Update the specified resource in storage.
     *
     * @param  \Illuminate\Http\Request  $request
     * @param  \App\Template  $template
     * @return \Illuminate\Http\Response
     */
    public function update(Request $request, Template $template)
    {
        //
    }

    /**
     * Remove the specified resource from storage.
     *
     * @param  \App\Template  $template
     * @return \Illuminate\Http\Response
     */
    public function destroy(User $user, Template $template)
    {
        $template->delete();
        return redirect('home');
    }
}



