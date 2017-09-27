package metroinsight.citadel.rest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.serviceproxy.ProxyHelper;
import metroinsight.citadel.common.RestApiTemplate;
import metroinsight.citadel.data.DataService;
import metroinsight.citadel.data.impl.GeomesaService;
import metroinsight.citadel.datacache.DataCacheService;
import metroinsight.citadel.datacache.impl.RedisDataCacheService;
import metroinsight.citadel.metadata.MetadataService;
import metroinsight.citadel.model.BaseContent;


//TODO: This should be deprecated
public class DataRestApi extends RestApiTemplate{

  DataCacheService cacheService = null;
  MetadataService metadataService = null;

  private DataService dataService;
  Vertx vertx;
  
  public DataRestApi (Vertx vertx) {
    dataService = new GeomesaService(vertx);
    this.vertx = vertx;
    cacheService = new RedisDataCacheService(vertx);
    metadataService = ProxyHelper.createProxy(MetadataService.class, vertx, MetadataService.ADDRESS);
  }

  
  void upsertCache(String uuid, JsonObject data, Handler<AsyncResult<Void>> rh) {
    if (cacheService == null) {
      return;
    }
    JsonObject cache = new JsonObject();
    // This had better use CachedData structure, but because redis can't store null values, it does not make sense to use default values from CachedData
    JsonArray coordinate = data.getJsonArray("coordinates").getJsonArray(0);
    cache.put("lng", coordinate.getDouble(0));
    cache.put("lat", coordinate.getDouble(1));
    cache.put("value", data.getDouble("value"));
    cache.put("timestamp", data.getLong("timestamp"));
    List<String> indexKeys = new ArrayList<String>(2);
    indexKeys.add(0, "lng");
    indexKeys.add(1, "lat");
    cacheService.upsertData(uuid, cache, indexKeys, cacheRh -> {
      if (cacheRh.succeeded()) {
        rh.handle(Future.succeededFuture());
      } else {
        System.out.println(cacheRh.cause());
        rh.handle(Future.failedFuture(cacheRh.cause()));
      }
    });
  }
  
  public void queryData(RoutingContext rc) {
    JsonObject q = rc.getBodyAsJson().getJsonObject("query");
    HttpServerResponse resp = getDefaultResponse(rc);
    BaseContent content = new BaseContent();
    dataService.queryData(q, ar -> {
      String cStr;
      String cLen;
      if (ar.failed()) {
        content.setReason(ar.cause().getMessage());
        cStr = content.toString();
        cLen = Integer.toString(cStr.length());
        resp.setStatusCode(400);
      } else {
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
  }
  
  public void getData(RoutingContext rc) {
    
  }
  

  public void insertData(RoutingContext rc) {
    HttpServerResponse resp = getDefaultResponse(rc);
    JsonArray data = rc.getBodyAsJson().getJsonArray("data");

    //// Validate if the UUIDs are valid.
    // Extract unique uuids in the data.
    Set<String> uuids = new HashSet<String>();
    for (int i=0; i < data.size(); i++) {
      uuids.add(data.getJsonObject(i).getString("uuid"));
    }
    
    // Check if all uuids exist in metadata db.
    List<Future> uuidFutList = new ArrayList<Future>();
    for (String uuid: uuids) {
      Future<Boolean> uuidFut = Future.future();
      metadataService.getPoint(uuid, rh -> {
        if (rh.succeeded()) {
          uuidFut.complete(true);
        } else {
          uuidFut.fail(uuid + "does not exist");
        }
      });
      uuidFutList.add(uuidFut);
    }

    //Update Cache if available.
    Future<Void> cacheFuture = Future.future();
    if (cacheService != null) {
      String uuid;
      JsonObject cacheBuffers = new JsonObject();
      JsonObject datum;
      for (int i = 0; i < data.size(); i++) {
      //Buffering for cache
        datum = data.getJsonObject(i);
        uuid = datum.getString("uuid");
        JsonObject buf = null;
        if (cacheBuffers.containsKey(uuid)) {
          datum = data.getJsonObject(i);
          buf = cacheBuffers.getJsonObject(uuid);
          if (buf.getDouble("timestamp") < datum.getDouble("timestamp")) {
            cacheBuffers.put(uuid, datum);
          }
        } else {
          cacheBuffers.put(uuid, datum);
        }
      }
      Iterator<String> keyIter = cacheBuffers.fieldNames().iterator();
      while (keyIter.hasNext()) {
        uuid = keyIter.next();
        upsertCache(uuid, cacheBuffers.getJsonObject(uuid), ar -> {
          if (ar.failed()) {
            cacheFuture.fail(ar.cause());
          }
        });
      }
    }

    // Actual running of uuid checking and then run the insertion.
    CompositeFuture.join(uuidFutList).setHandler(uuidAr -> {
      BaseContent content = new BaseContent();
      if (uuidAr.failed()) {
        // If any of uuid does not exist.
        sendErrorResponse(resp, 400, uuidAr.cause().getMessage());
      } else {
        dataService.insertData(data, dataAr -> {
          String cStr = "";
          String cLen = "";
          if (dataAr.failed()) {
            // If failed to insert data
            content.setReason(dataAr.cause().getMessage());
            resp.setStatusCode(400);
          } else {
            // Succeeded to insert data
            resp.setStatusCode(201);
            content.setSucceess(true);
          }
          cStr = content.toString();
          cLen = Integer.toString(cStr.length());
          resp
            .putHeader("content-length", cLen)
            .write(cStr);
        });
        cacheFuture.complete();
      }
    });
  }


}
