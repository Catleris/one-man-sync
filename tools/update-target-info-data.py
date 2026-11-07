"""Convert a locally reviewed Koitere/monster-stats monsterdata.csv to our static TSV.
Usage: python tools/update-target-info-data.py /path/to/monsterdata.csv
Unknown/non-scalar numbers stay unknown; conflicting NPC IDs are omitted.
"""
import csv
import re
import sys
from pathlib import Path

rows, conflicts = {}, set()
for source in csv.reader(Path(sys.argv[1]).open(encoding='utf-8-sig')):
    if len(source) < 21 or 'deadman' in source[0].lower():
        continue
    def number(i):
        value = source[i].strip()
        return value if re.fullmatch(r'-?\d+', value) else '?'
    element = source[1].strip().title()
    if element not in ('Air', 'Water', 'Earth', 'Fire'):
        element = '?'
    # Exact source NPC IDs, not name guesses. Store only display/reference fields.
    values = [source[0].strip(), element, number(2), number(5), number(6),
              number(4), number(3), number(7), number(8), number(9),
              number(14), number(17), number(18), number(20)]
    for token in source[10].split(','):
        if not token.strip().isdigit():
            continue
        npc_id = int(token)
        if npc_id in rows and rows[npc_id] != values:
            conflicts.add(npc_id)
        rows[npc_id] = values
out = Path('src/main/resources/lt/oneman/sync/target-info.tsv')
with out.open('w', encoding='utf-8') as f:
    f.write('# id\tname/variant\telement\tpercent\tstab\tslash\tcrush\tmagic\tstandard\theavy\tlight\thp\tdefence\tmagicLevel\tspeed\n')
    for npc_id, values in sorted(rows.items()):
        if npc_id not in conflicts:
            f.write('\t'.join([str(npc_id)] + values) + '\n')
print(f'{len(rows) - len(conflicts)} NPC IDs; {len(conflicts)} conflicting IDs omitted')
