import itertools
import json
import operator
import re
import sys

import subprocess

import pytablewriter

# try:
#     import pytablewriter
# except:
#     subprocess.check_call(['pip', 'install', 'pytablewriter'])
#     import pytablewriter

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
                {ver(version): list(o2)[0]['status']
                for version, o2 in itertools.groupby(sorted(o1, key=GET_VER), GET_VER)}
                for payload, o1 in itertools.groupby(sorted(data, key=GET_PAY), GET_PAY)}

by_version = {ver(version):
                {payload: list(o2)[0]['status']
                 for payload, o2 in itertools.groupby(sorted(o1, key=GET_PAY), GET_PAY)}
                 for version, o1 in itertools.groupby(sorted(data, key=GET_VER), GET_VER)}

sorted_ver = sorted(by_version.items(), key=lambda v: list(map(int, re.split(r'[._]', v[0]))))

ver_groups = [list(v for v, r in vers) for res, vers in itertools.groupby(sorted_ver, operator.itemgetter(1))]

ver_ranges = [[vs[0], vs[-1]] for vs in ver_groups]


# print('\n'.join(repr(v) for v in ver_groups))
#
# print(repr(by_payload))
# print(repr(by_version))

vers = list(list(by_payload.items())[0][1].keys())

# print(vers)

md = pytablewriter.MarkdownTableWriter()
md.table_name = ''
md.header_list = [''] + ['{} - {}'.format(vs, ve) if vs != ve else vs for vs, ve in ver_ranges]
md.value_matrix = [[p] + [status(o[vs]) for vs, ve in ver_ranges] for p, o in sorted(by_payload.items())]
md.margin = 1

md.write_table()
