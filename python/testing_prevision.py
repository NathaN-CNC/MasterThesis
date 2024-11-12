from pathlib import Path
import pandas as pd
import numpy as nu
import joblib
import sklearn
import time

DIRPATH = Path(__file__).parent

print("Carregando dados...")
df = pd.read_csv(DIRPATH / 'dadostreino.csv')
print("Dados carregados")

df = df.drop(columns=['label'])
print(df)

print("Carregando modelo...")
modelo = joblib.load(DIRPATH / 'XGBoost.pkl')
print("Modelo carregado")


for i in range(min(len(df), 50)):
    amostra = df.iloc[i]
    previsao = modelo.predict_proba([amostra])
    print(f"Previsão para linha {i+1}: {previsao}")
    if previsao[0][0] < 0.8 and previsao[0][1] < 0.8:
        print("Ajuda: Incerteza na análise")
    if i < min(len(df), 50) - 1:
        time.sleep(10)
