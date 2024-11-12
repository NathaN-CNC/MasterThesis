import argparse
from pathlib import Path
import re
import sys

RE_UUID = re.compile(r'\w{8}-\w{4}-\w{4}-\w{4}-\w{12}')


def group_messages(log_file: str, output=sys.stdout):
    lines = Path(log_file).read_text(encoding="UTF-16").splitlines()
    groups = {}

    for line in lines:
        match = RE_UUID.search(line)
        if match:
            uuid = match.group()
            groups[uuid] = groups.get(uuid, []) + [line]

    print(f'Found {len(groups)} unique UUIDs')
    for uuid, lines in groups.items():
        for line in lines:
            print(line, file=output)
        print(file=output)

def count_sure_lines(log_file: str, index=1):
    RE_IS_ATTACK = re.compile(r'sure is attack\? (true|false) : prob\? [\d\.\-E]+ : label\? (true|false) : correct\? (true|false)')
    trues = 0
    falses = 0
    total = 0
    with Path(log_file).open(encoding="utf-16") as f:
        lines = f
        for line in lines:
            match = RE_IS_ATTACK.search(line)
            if match:
                if match.group(index) == 'true':
                    trues += 1
                else:
                    falses += 1
                total += 1

    if total > 0:
        print(f"Trues  : {trues: 10d} | {trues / total:.2f}")
        print(f"Falses : {falses: 10d} | {falses / total:.2f}")
        print(f"Total  : {total: 10d} | {total / total:.2f}")


def count_correct_lines(log_file: str):
    RE_IS_ATTACK = re.compile(r'result is attack\? (true|false) : prob\? [\d\.\-E]+ : lebel\? (true|false) : correct\? (true|false)')
    trues = 0
    falses = 0
    total = 0
    with Path(log_file).open(encoding="utf-16") as f:
        lines = f
        for line in lines:
            match = RE_IS_ATTACK.search(line)
            if match:
                if match.group(3) == 'true':
                    trues += 1
                else:
                    falses += 1
                total += 1

    if total > 0:
        print(f"Correct:   {trues: 10d} | {trues / total:.2f}")
        print(f"Incorrect: {falses: 10d} | {falses / total:.2f}")
        print(f"Total :    {total: 10d} | {total / total:.2f}")


def main():
    parser = argparse.ArgumentParser(description='Analyze logs')
    subparsers = parser.add_subparsers(dest='command')

    sb = subparsers.add_parser('group-messages')
    sb.add_argument('log', type=str, help='Log file to analyze')
    sb.add_argument('-o', '--output', type=argparse.FileType('w'),
                        default=sys.stdout, help='Output file')
    sb.set_defaults(func=lambda args: group_messages(args.log, args.output))

    sb = subparsers.add_parser('count-sure-lines')
    sb.add_argument('log', type=str, help='Log file to analyze')
    sb.add_argument('-i', '--index', type=int, default=1, help='Index of the value to count')
    sb.set_defaults(func=lambda args: count_sure_lines(args.log, args.index))

    sb = subparsers.add_parser('count-correct-lines')
    sb.add_argument('log', type=str, help='Log file to analyze')
    sb.set_defaults(func=lambda args: count_correct_lines(args.log))

    args = parser.parse_args()
    if not args.command:
        parser.print_help()
        exit(1)
    args.func(args)


if __name__ == '__main__':
    main()
