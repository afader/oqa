#!/usr/bin/env python
import sys
import utils

def add_id(input):
  for (i, line) in enumerate(input):
    yield 'id\t%s\t%s' % (i, line)

if __name__ == '__main__':
  host = sys.argv[1]
  port = int(sys.argv[2])
  coll = 'relsyn'
  utils.index_stream(add_id(sys.stdin), port=port, coll=coll)
