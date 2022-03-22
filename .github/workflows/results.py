import itertools
import json
import operator
import sys

import subprocess

try:
    import pytablewriter
except:
    subprocess.check_call(['pip', 'install', 'pytablewriter'])
    import pytablewriter

def ver(v):
    return v.split(".", 1)[1] if v.startswith("1.") else v

def status(s):
    return ':green_circle:' if s == 'SUCCESS' else ':red_square:'


if len(sys.argv) > 1:
    with open(sys.argv[1]) as f:
        data = [json.loads(line) for line in f.readlines()]
else:
    data = [json.loads(line) for line in sys.stdin.readlines()]

for da in data:
    del da['out']

# print(repr(data))

GET_VER = operator.itemgetter('java.version')
GET_PAY = operator.itemgetter('payload')

by_payload = {payload:
                {ver(version): status(list(o2)[0]['status'])
                for version, o2 in itertools.groupby(sorted(o1, key=GET_VER), GET_VER)}
                for payload, o1 in itertools.groupby(sorted(data, key=GET_PAY), GET_PAY)}

# print(repr(by_payload))

vers = list(list(by_payload.items())[0][1].keys())

# print(vers)

md = pytablewriter.MarkdownTableWriter()
md.table_name = ''
md.header_list = [''] + vers
md.value_matrix = [[p] + [o[v] for v in vers] for p, o in sorted(by_payload.items())]

md.write_table()
