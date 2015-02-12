#!/usr/bin/env python
 # -*- coding: utf-8 -*-
import web
import sys
import kenlm
import json
import os
dir = os.path.dirname(os.path.realpath(__file__))

lm_path = '%s/../../../questions.binary' % dir
port = 9090

sys.stderr.write("Loading language model from %s..." % lm_path)
lm = kenlm.LanguageModel(lm_path)
sys.stderr.write("Done.\n")

urls = ('/score', 'score')

class MyApplication(web.application): 
  def run(self, port=8080, *middleware): 
    func = self.wsgifunc(*middleware) 
    return web.httpserver.runsimple(func, ('0.0.0.0', port)) 

app = MyApplication(urls, globals())

class score:

    def get_scores(self, queries):
        return [lm.score(q) for q in queries]

    def GET(self):
        i = web.input(_unicode=False)
        queries = [q.strip() for q in i.q.split('|')]
        print >>sys.stderr, "queries:\n%s" % str('\n'.join(queries))
        return '\n'.join('%0.4f' % s for s in self.get_scores(queries))

    def POST(self):
        return self.GET()

if __name__ == '__main__':
    app.run(port=port)
