import argparse
import re

from pathlib import Path

from typing import List, Tuple, NamedTuple, Optional

RE_METRIC = re.compile(r'\[(.*)\] \[INFO\]: metrics: Name=(.*), Skill=(.*), Type=(.*), F1=(.*), Accuracy=(.*), Precision=(.*), Recall=(.*), Total=(.*), Asks=(.*), Timeouts=(.*), Points=(.*), TP=(.*), FP=(.*), TN=(.*), FN=(.*), API time=(.*), Ask time=(.*), Voting time=(.*)')

# [2024-04-28T19:19:07:11.868] [INFO]: metrics: Name=agent-007, Skill=LGBM, Type=all, F1=0.985, Accuracy=0.980, Precision=0.985, Recall=0.985, Total=100, Asks=0, Timeouts=0, Points=96, TP=65, FP=1, TN=33, FN=1, API time=0.000, Ask time=0.299, Voting time=0.000
class Metric(NamedTuple):
    datetime: str
    name: str
    skill: str
    type: str
    f1: float
    accuracy: float
    precision: float
    recall: float
    total: int
    asks: int
    timeouts: int
    points: int
    tp: int
    fp: int
    tn: int
    fn: int
    api_time: float
    ask_time: float
    voting_time: float


def parse_metrics(content: str) -> List[Metric]:
    metrics = []

    for line in content.split('\n'):
        match = RE_METRIC.match(line)
        if match:
            datetime, name, skill, type_, f1, accuracy, precision, recall, total, asks, timeouts, points, tp, fp, tn, fn, api_time, ask_time, voting_time = match.groups()
            metrics.append(Metric(
                datetime=datetime,
                name=name,
                skill=skill,
                type=type_,
                f1=float(f1),
                accuracy=float(accuracy),
                precision=float(precision),
                recall=float(recall),
                total=int(total),
                asks=int(asks),
                timeouts=int(timeouts),
                points=int(points),
                tp=int(tp),
                fp=int(fp),
                tn=int(tn),
                fn=int(fn),
                api_time=float(api_time),
                ask_time=float(ask_time),
                voting_time=float(voting_time)
            ))
    return metrics


def metrics_to_csv(metrics: List[Metric], output_path: Path) -> str:
    fields = Metric._fields
    with output_path.open('w') as f:
        print(*fields, sep=',', file=f)
        for metric in metrics:
            print(*metric, sep=',', file=f)
    print("Metrics saved to", output_path)


def main() -> None:
    parser = argparse.ArgumentParser(description='Parse and analyze metrics')
    parser.add_argument('log_file', type=Path, help='Path to log file')
    parser.add_argument('output_file', type=Path, help='Path to output CSV file')
    args = parser.parse_args()
    metrics = parse_metrics(args.log_file.read_text())
    metrics_to_csv(metrics, args.output_file)


if __name__ == '__main__':
    main()
