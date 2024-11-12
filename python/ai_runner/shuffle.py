import pandas as pd

test_path = r"C:\Users\natha\Documents\AgentesML\python\dadosteste.csv"

df = pd.read_csv(test_path)

# Save as dadosteste3.csv the shuffle of dadosteste2.csv
df = df.sample(frac=1).reset_index(drop=True)
df.to_csv(r"C:\Users\natha\Documents\AgentesML\python\dadosteste2.csv", index=False)