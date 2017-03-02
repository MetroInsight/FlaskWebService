import time
import sys
import pdb

import influxdb
from flask import request, jsonify
from flask_restplus import Api, Resource, Namespace, fields
from flask_restplus import reqparse

from . import responses
from .. import timeseriesdb as ts_db

ns = Namespace('timeseries', description='Operations related to timeseries')

@ns.param('uuid', 'Unique identifier of point')
@ns.route('/<string:uuid>')
class TimeSeriesAPI(Resource):
    def get(self, uuid):
        """
        Reads the time series data of a point for the requested range

        Parameters:
        "uuid": <point uuid>
        "start_time": <unix timestamp of start time in seconds>
        "end_time": <unix timestamp of end time in seconds>

        Returns (JSON):
        {
            "data":{
                "name": <point uuidi>
                "series": [
                    "columns": [column definitions]
                ]
                "values":[list of point values]
            }
            "success": <True or False>
        }

        """

        query_string = 'select * from "{0}"'.format(uuid)
                        #influxdb only take double quotes in query
        print(query_string) 
        start_time = request.args.get('start_time')
        end_time = request.args.get('end_time')
        if not start_time:
            query_string += "order by time desc limit 1"
        elif not end_time:
            query_string += "where time >= {0}".format(str(start_time))
        else:
            query_string += "where time >= {0} and time < {1}"\
                            .format(str(start_time), str(end_time))
        data = ts_db.query(query_string)
        response = dict(responses.success_true)
        response.update({'data': data.raw})
        return response


    def post(self, uuid):
        """
        Parameters:
        {
            "samples": [
                {
                    "time": unix timestamp in seconds
                    "value": value
                },
                { more times and values }
            ]
        }
        Returns:
        {
            "success": <True or False>
            "error": error message
        }
        """
        points = []
        data = request.get_json(force=True)
        for sample in data['samples']:
            data_dict = {
                    'measurement': uuid,
                    'time': sample['time'],
                    'fields':{
                            'value': sample['value']         
                        }
                }
            points.append(data_dict)
        
            
            result = ts_db.write_points(points, time_precision='s')
            response = dict(responses.success_true)
        else:
            response = dict(responses.success_false)
            response.update({'error': 'Error occurred when writing to InfluxDB'})
        print("BYE")

        return response
