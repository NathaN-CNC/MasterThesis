from pathlib import Path
from pprint import pprint
import pandas as pd
import joblib


print('versao', joblib.__version__)

DIRPATH = Path(__file__).parent


print("Carregando dados...")
df = pd.read_csv(DIRPATH / 'dadostreino.csv')
print("Dados carregados")

df = df.drop(columns=['label'])
print(df)

pprint(list(df.columns))
# modelo = joblib.load(DIRPATH / 'RandomForest.pkl')
