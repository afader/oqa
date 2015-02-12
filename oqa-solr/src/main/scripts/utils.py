import sys
import json
import urllib2
import itertools

def grouper(iterable, n, fillvalue=None):
  args = [iter(iterable)] * n
  return itertools.izip_longest(fillvalue=fillvalue, *args)

def solr_url(host, port, coll):
  return 'http://%s:%s/solr/%s/update/json?commit=true' % (host, port, coll)

def index_objects(objects, host, port, collection):
  url = solr_url(host, port, collection)
  encoded = json.dumps(objects)
  req = urllib2.Request(url)
  req.add_header('Content-Type', 'application/json')
  urllib2.urlopen(req, encoded)

def parse_line(line):
  fields = line.rstrip('\n').split('\t')
  keys = fields[0::2]
  values = fields[1::2]
  object = dict(zip(keys, values))
  return object

def index_stream(input, port, coll, host='localhost', chunk_size=10000):
  grouped = [x for x in grouped if x != None]
  grouped = grouper(input, chunk_size)
  added = 0
  for group in grouped:
    parsed = [parse_line(line) for line in group if line]
    index_objects(parsed, host, port, coll)
    added += len(parsed)
    print >>sys.stderr, added
