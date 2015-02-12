#!/usr/bin/env python
import sys
import utils

if __name__ == '__main__':
  host = sys.argv[1]
  port = int(sys.argv[2])
  collection = sys.argv[3]
  chunk_size = 10000
  added = 0
  for lines in utils.grouper(sys.stdin, chunk_size):
    lines = [x for x in lines if x != None]
    objects = [utils.parse_line(line) for line in lines]
    utils.index_objects(objects, host, port, collection)
    added += chunk_size
    print >>sys.stderr, added 
