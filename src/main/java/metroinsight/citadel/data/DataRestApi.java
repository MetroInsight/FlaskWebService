package metroinsight.citadel.data;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import metroinsight.citadel.authorization.Authorization_MetaData;
import metroinsight.citadel.common.RestApiTemplate;
import metroinsight.citadel.data.impl.GeomesaService;
import metroinsight.citadel.model.BaseContent;


public class DataRestApi extends RestApiTemplate {

	private DataService dataService;
	Authorization_MetaData Auth_meta_data;
	
  public DataRestApi () {
	  dataService=new GeomesaService();
	  Auth_meta_data=new Authorization_MetaData();
  }
  
  public void queryData(RoutingContext rc) {
	  
	System.out.println("In queryData DataRestApi.java");  
	JsonObject body = rc.getBodyAsJson();
	
	HttpServerResponse resp = getDefaultResponse(rc);
	BaseContent content = new BaseContent();
	/*
     * verify the user Token is valid and used has authority to query Data on provided uuid into the System
     */
    //get_ds_owner_token(String dsId), we also need to check query parameters
    if(body.containsKey("userToken")&&body.containsKey("uuid")&&body.containsKey("query"))
    {
    	String token=body.getString("userToken");
    	String uuid=body.getString("uuid");//uuid is the ds_id
    	JsonObject query=body.getJsonObject("query");
    	
    	if(token.equals("")||uuid.equals(""))//TODO: also check that query is not empty here!!
    	{
    		//System.out.println("The parameters are missing");
    		//return;
    		System.out.println("In DataRestApi: Query parameters are missing");
        	sendErrorResponse(resp, 400, "Query parameters are missing");
    		
    	}
    	
    	//from token extract the userId.
    	String userId=Auth_meta_data.get_userID(token);
    	
    	if(userId.equals(""))
    	{
    		//System.out.println("The Token is invalid or you don't have required priveleges");
    		//return;
    		System.out.println("In DataRestApi: Policy for user doesn't exist");
    		sendErrorResponse(resp, 400, "Api-Token doesn't exist or it doesn't have required priveleges");	
    	}
    	
    	//for this userId extract the policy
    	String policy = Auth_meta_data.get_policy(uuid, userId);
    	
    	/*
    	 * old model updated considering HBase consistency constraint
    	 */
    	//String token_owner=Auth_meta_data.get_ds_owner_token(uuid);//this means uuid exists and token also exists
    		
    	if(policy.equals("true")) { //true is default policy with no-space time constraints
    		JsonObject q = body.getJsonObject("query");
    		
    		q.put("uuid", uuid);//uuid on which we want to query, extend it to the set of uuids
    		
            System.out.println("Query is:"+q);
            dataService.queryData(q, ar -> {
            	String cStr;
                String cLen;
                
            	if (ar.failed()) {
              	System.out.println(ar.cause().getMessage());
              	content.setReason(ar.cause().getMessage());
                cStr = content.toString();
                cLen = Integer.toString(cStr.length());
                resp.setStatusCode(400);
                
            	} else {
            		/*
            		System.out.println("Suceeded in DataRestAPI Query Data");
            		String resultStr = ar.result().toString();
            		//System.out.println("Query Results are:"+resultStr);
            		String length = Integer.toString(resultStr.length());
            		rc.response()
            		.putHeader("content-TYPE", "application/json; charset=utf=8")
            		.putHeader("content-length",  length)
              	.setStatusCode(200)
              	.write(resultStr);
             */	
            		content.setSucceess(true);
                    content.setResults(ar.result());
                    cStr = content.toString();
                    cLen = Integer.toString(cStr.length());
                    resp
                    .setStatusCode(200);
            		
            }	
            	resp
                .putHeader("content-length", cLen)
                .write(cStr);
            	
            	});
            	
        	
    	}//end if(policy.equals("true")) 
    	else
    	{
    		System.out.println("In DataRestApi: Policy for user doesn't exist");
    		sendErrorResponse(resp, 400, "Api-Token doesn't exist or it doesn't have required priveleges");	
    	}
    	
    	
    }//end  if(body.containsKey("userToken")&&body.containsKey("uuid"))
    else
    {
    	System.out.println("In DataRestApi: Query parameters are missing");
    	sendErrorResponse(resp, 400, "Query parameters are missing");	
    }
	
    
  }//end queryData(RoutingContext rc)
  
  
  public void insertData(RoutingContext rc) {
	 
	System.out.println("In insertData DataRestApi.java");
    JsonObject body = rc.getBodyAsJson();
    // Get the query as JSON.
    
    /*
     * verify the user Token is valid and he as authority to insert Data into the System
     */
    //get_ds_owner_token(String dsId)
    if(body.containsKey("userToken")&&body.containsKey("uuid")&&body.containsKey("data"))
    {
    
    	String token=body.getString("userToken");
    	String uuid=body.getString("uuid");
    	
    	if(token.equals("")||uuid.equals(""))
    	{
    		System.out.println("Parameters are missing");
    		return;
    	}
    	
    	//for the token extract the userId
    	String userId=Auth_meta_data.get_userID(token);
    	
    	//extract the DS ownerID for uuid
    	String ownerId=Auth_meta_data.get_ds_owner_id(uuid);
    	
    	if(userId.equals(ownerId)) {
    		
		    	JsonArray q = body.getJsonArray("data");
		   	   //JsonObject body = (JsonObject) rc.getBodyAsJson().getValue("query");
		   	    System.out.println("body is:"+body);
		   	   //  JsonArray q = body.getJsonArray("data");
		   	  
		   	    //have to add UUID to the dataService.insertData function
		        // Call createPoint in metadataService asynchronously.
		        dataService.insertData(uuid, q, ar -> { 
		         // ar is a result object created in metadataService.createPoint
		         // We pass what to do with the result in this format.
		       	if (ar.failed()) {
		       	  // if the service is failed
		       	  // TODO: add response here.
		         	System.out.println(ar.cause().getMessage());
		       	} else {
		       	  System.out.println("Suceeded in DataRestAPI insertData");
		       	  JsonObject result = new JsonObject();
		       	  result.put("result", "SUCCESS");
		       	  String length = Integer.toString(result.toString().length());
		       		rc.response()
		       		  .putHeader("content-TYPE", "application/text; charset=utf=8")
		       		  .putHeader("content-length",  length)
		       		  .setStatusCode(201)
		       		  .write(result.toString());
		       	}
		       	});
       
    	}//if(token_owner.equals(token))
    	else{
    		System.out.println("Token doesn't have required preveleges");	
    	}
    		
    }//end  if(body.containsKey("userToken"))
    else
    {
    	System.out.println("parameters are missing");	
		return;
    }
    
    /*
     * 
     */
    
  
  }//end insertData(RoutingContext rc) 
  

}
