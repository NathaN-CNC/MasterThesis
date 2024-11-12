from pathlib import Path
import pandas as pd
import numpy as np

DIRPATH = Path(__file__).parent

df = pd.read_csv(DIRPATH / 'dadostreino.csv')


def bool_to_int(b):
    return 1 if b else 0


sample = df.sample(1)

print([bool_to_int(i) if np.issubdtype(type(i), np.bool_) else i
       for i in list(sample.iloc[0])])
