package metroinsight.citadel;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import metroinsight.citadel.datacache.impl.RedisDataCacheService;
import metroinsight.citadel.model.CachedData;;

@RunWith(VertxUnitRunner.class)
public class ServerTest {
  
  private Vertx vertx;
  private Integer port;
  String serverip="localhost";
  
  @Before
  public void setUp(TestContext context) throws IOException{
    ServerSocket socket = null;
    socket = new ServerSocket(0);
    port =8080; //socket.getLocalPort();
    socket.close();
    
    DeploymentOptions options = new DeploymentOptions()
        .setConfig(new JsonObject().put("http.port", port)
            );
    vertx = Vertx.vertx();
    vertx.deployVerticle(RestApiVerticle.class.getName(),
        options,
        context.asyncAssertSuccess());
    
    }
  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  //@Test
  public void testMyApplication(TestContext context){
    final Async async = context.async();
    
    vertx.createHttpClient().getNow(port,  serverip, "/",
        response -> {
          response.handler(body -> {
            context.assertTrue(body.toString().contains("Hello"));
            async.complete();
          });
        });
  }
  
  //@Test
  public void testQueryPoint(TestContext context) {
    final Async async = context.async();
  	JsonObject query = new JsonObject();
  	query.put("query", (new JsonObject()).put("pointType", "temp"));
    String queryStr = Json.encodePrettily(query);
    String length = Integer.toString(queryStr.length());
    vertx.createHttpClient().post(port, "localhost", "/api/query")
    	.putHeader("content-type", "application/json")
    	.putHeader("content-length",  length)
    	.handler(response -> {
    		context.assertEquals(response.statusCode(), 200);
    		response.bodyHandler(body -> {
    			context.assertTrue(body.toJsonObject().getJsonArray("results").size() > 0);
    			async.complete();
    		});
    	})
    	.write(queryStr);
  }
  
  //@Test
  public void testCreateSensor(TestContext context){
    final Async async = context.async();
    JsonObject metadataJo = new JsonObject();
    metadataJo.put("pointType",  "temp");
    metadataJo.put("unit",  "F");
    final String json = Json.encodePrettily(metadataJo);
    final String length = Integer.toString(json.length());
    vertx.createHttpClient().post(port, serverip, "/api/point")
    	.putHeader("content-type", "application/json")
    	.putHeader("content-length",  length)
    	.handler(response -> {
    		context.assertEquals(response.statusCode(), 201);
    		response.bodyHandler(body -> {
    			context.assertTrue(body.toJsonObject().getBoolean("success"));
    			async.complete();
    		});
    	})
    	.write(json);
  }
/*
  @Test
  public void testGetSensor(TestContext context) {
    final Async async = context.async();
    String uuid = "90fb26f6-4449-482b-87be-83e5741d213e"; //TODO: This needs to be auto-gen later.
  	JsonObject query = new JsonObject();
  	query.put("query", (new JsonObject()).put("uuid", uuid));
    String queryStr = Json.encodePrettily(query);
    String length = Integer.toString(queryStr.length());
    vertx.createHttpClient().post(port, "localhost", "/api/query")
    	.putHeader("content-type", "application/json")
    	.putHeader("content-length",  length)
    	.handler(response -> {
    		context.assertEquals(response.statusCode(), 200);
    		response.bodyHandler(body -> {
    			System.out.println("response is:"+body);
    			context.assertTrue(body.toJsonArray().size() > 0);
    			async.complete();
    		});
    	})
    	.write(queryStr)
  }
*/
  //@Test
  public void testInsertData(TestContext context) {
    System.out.println("START TESTING INSERT DATA");
    final Async async = context.async();
    String uuid = "90fb26f6-4449-482b-87be-83e5741d213e"; 
    String uuid2 = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
  	JsonObject query = new JsonObject();

  	JsonArray data = new JsonArray();

  	JsonObject datum1 = new JsonObject();
  	Double lng = -117.231221;
  	Double lat = 32.881454;
  	datum1.put("uuid", uuid);
  	datum1.put("timestamp", 1499813708623L);
  	datum1.put("value", 15);
  	datum1.put("geometryType", "point");
  	ArrayList<ArrayList<Double>> coordinates = new ArrayList<ArrayList<Double>>();
  	ArrayList<Double> coordinate = new ArrayList<Double>();
  	coordinate.add(lng);
  	coordinate.add(lat);
  	coordinates.add(coordinate);
  	datum1.put("coordinates", coordinates);
  	data.add(datum1);

  	JsonObject datum2 = new JsonObject();
  	lng = -117.231230;
  	lat = 32.881450;
  	datum2.put("uuid", uuid2);
  	datum2.put("timestamp", 1499813708600L);
  	datum2.put("value", 20);
  	datum2.put("geometryType", "point");
  	ArrayList<ArrayList<Double>> coordinates2 = new ArrayList<ArrayList<Double>>();
  	ArrayList<Double> coordinate2 = new ArrayList<Double>();
  	coordinate2.add(lng);
  	coordinate2.add(lat);
  	coordinates2.add(coordinate2);
  	datum2.put("coordinates", coordinates2);
  	data.add(datum2);
  	
  	query.put("data",data);
    String queryStr = Json.encodePrettily(query);
    String length = Integer.toString(queryStr.length());
    vertx.createHttpClient().post(port, serverip, "/api/data")
    	.putHeader("content-type", "application/json")
    	.putHeader("content-length",  length)
    	.handler(response -> {
    		context.assertEquals(response.statusCode(), 201);
    			async.complete();
    	})
    	.write(queryStr);
    System.out.println("FINISHED TESTING INSERT DATA");
  }
  
  //@Test
  public void testQueryData(TestContext context) {
    final Async async = context.async();
  	JsonObject query = new JsonObject();
  	JsonObject data = new JsonObject();
  	data.put("lat_min", 32.868623);
  	data.put("lat_max", 32.893202);
  	data.put("lng_min", -117.244438);
  	data.put("lng_max", -117.214398);
  	data.put("timestamp_min", 1499813707623L);
  	data.put("timestamp_max", 1499813709623L);
  	query.put("query",data);
    String queryStr = Json.encodePrettily(query);
    String length = Integer.toString(queryStr.length());
    vertx.createHttpClient().post(port, serverip, "/api/querydata")
      .putHeader("content-type", "application/json")
      .putHeader("content-length",  length)
      .handler(response -> {
        context.assertEquals(response.statusCode(), 200);
        response.bodyHandler(body -> {
          System.out.println("Data Query response is:"+body);
          context.assertTrue(body.toJsonObject().getJsonArray("results").size() > 0);
          async.complete();
          });
    	})
      .write(queryStr);
  }
  
  private Boolean check_only_one_uuid(JsonArray data, String uuid) {
    JsonObject datum;
    System.out.println(data);
    for (int i=0; i<data.size(); i++) {
      datum = data.getJsonObject(i);
      if (!datum.getString("uuid").equals(uuid)) {
        return false;
      }
    }
    return true;
  }

  //@Test
  public void testQueryDataOnlyUUID(TestContext context) {
    final Async async = context.async();
  	JsonObject query = new JsonObject();
  	JsonObject data = new JsonObject();
    String uuid = "90fb26f6-4449-482b-87be-83e5741d213e"; 
    JsonArray uuids = new JsonArray();
    uuids.add(uuid);
  	data.put("uuids", uuids);
  	query.put("query",data);
    String queryStr = Json.encodePrettily(query);
    String length = Integer.toString(queryStr.length());
    vertx.createHttpClient().post(port, serverip, "/api/querydata")
      .putHeader("content-type", "application/json")
      .putHeader("content-length",  length)
      .handler(response -> {
        context.assertEquals(response.statusCode(), 200);
        response.bodyHandler(body -> {
          System.out.println("Data Query response is:"+body);
          JsonArray results = body.toJsonObject().getJsonArray("results");
          context.assertTrue(results.size() > 0);
          context.assertTrue(check_only_one_uuid(results, uuid));
          async.complete();
          });
    	})
      .write(queryStr);
  }
  
  public JsonObject getRedisTestConfig() {
    String filename = "configs/cache_test_config.json";
    Buffer configBuf = vertx.fileSystem().readFileBlocking(filename);
    JsonObject configJson = configBuf.toJsonObject();
    return configJson;
  }
  
  public JsonArray getDataTestConfig() {
    String filename = "configs/data_test_config.json";
    Buffer configBuf = vertx.fileSystem().readFileBlocking(filename);
    JsonArray configJson = configBuf.toJsonArray();
    return configJson;
  }
  
  //@Test
  public void testRedisWrite(TestContext context) {
    System.out.println("START TESTING CACHE WRITING");
    // Read config
    JsonObject cacheTestConfig = getRedisTestConfig();
    String uuid = cacheTestConfig.getString("uuid");
    JsonObject data = cacheTestConfig.getJsonObject("data");
    final Async async = context.async();
    RedisDataCacheService redisCache = new RedisDataCacheService(vertx);
    List<String> indexKeys = new ArrayList<String>(2);
    indexKeys.add(0, "lat");
    indexKeys.add(1, "lng");
    redisCache.upsertData(uuid, data, indexKeys, rh -> {
      context.assertTrue(rh.succeeded());
      System.out.println("FINISH TESTING CACHE WRITING");
      async.complete();
    }); 
  }
  
  //@Test
  public void testRedisRead(TestContext context) {
    System.out.println("CACHE READ START");
    // Read config
    JsonObject cacheTestConfig = getRedisTestConfig();
    String uuid = cacheTestConfig.getString("uuid");
    JsonObject targetData = cacheTestConfig.getJsonObject("data").mapTo(CachedData.class).toJson();
    final Async async = context.async();
    RedisDataCacheService redisCache = new RedisDataCacheService(vertx);
    List<String> fields = new ArrayList<>(Arrays.asList("pointType", "unit", "lng", "lat", "timestamp", "value", "name"));
    redisCache.getData(uuid, fields, rh -> {
      context.assertTrue(rh.succeeded());
      JsonObject data = rh.result();
      //TODO: get keys and compare values
      CachedData cachedData = data.mapTo(CachedData.class);
      data = cachedData.toJson();
      context.assertEquals(data,  targetData);
      System.out.println("CACHE READ SUCCESS");
      async.complete();
    });
  }
  
  @Test
  public void testQuerySimpleBbox(TestContext context) {
    //router.post("/api/querydata/simplebbox").handler(dataRestApi::querySimpleBbox);
    System.out.println("START TESTING Simple BBox Query");
    final Async async = context.async();
    JsonObject query = new JsonObject();
    JsonArray data = getDataTestConfig();
    JsonObject targetDatum = data.getJsonObject(0);
    JsonObject refDatum = data.getJsonObject(1);
    Double lng1 = targetDatum.getJsonArray("coordinates").getJsonArray(0).getDouble(0);
    Double lat1 = targetDatum.getJsonArray("coordinates").getJsonArray(0).getDouble(1);
    Double lng2 = refDatum.getJsonArray("coordinates").getJsonArray(0).getDouble(0);
    Double lat2 = refDatum.getJsonArray("coordinates").getJsonArray(0).getDouble(1);
    Double deltaLng = Math.abs(lng1-lng2)/2;
    Double deltaLat = Math.abs(lat1-lat2)/2;
    Double minLng = lng1 - deltaLng;
    Double maxLng = lng1 + deltaLng;
    Double minLat = lat1 - deltaLat;
    Double maxLat = lat1 + deltaLat;
    query.put("min_lat", minLat);
    query.put("min_lng", minLng);
    query.put("max_lat", maxLat);
    query.put("max_lng", maxLng);
    
    JsonObject queryLoad = new JsonObject();
    queryLoad.put("query", query);

    String queryStr = Json.encodePrettily(queryLoad);
    String length = Integer.toString(queryStr.length());

    vertx.createHttpClient().post(port, serverip, "/api/querydata/simplebbox")
      .putHeader("content-type", "application/json")
      .putHeader("content-length",  length)
      .handler(response -> {
        context.assertEquals(response.statusCode(), 200);
        response.bodyHandler(body -> {
          System.out.println("Data Query response is:"+body);
          JsonArray res = body.toJsonObject().getJsonArray("results");
          boolean foundFlag = false;
          JsonObject datum;
          for (int i=0; i< res.size(); i++) {
            datum = res.getJsonObject(i);
            if (datum.getString("uuid").equals(targetDatum.getString("uuid"))) {
              foundFlag = true;
            }
          }
          context.assertTrue(foundFlag);
          async.complete();
          });
    	})
      .write(queryStr);
    System.out.println("FINISHED TESTING Simple BBox Query");
  }
  
}
