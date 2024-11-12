import pandas as pd
from sklearn.metrics import classification_report
from main import get_model
import seaborn as sns
import matplotlib.pyplot as plt

model_name = 'ExtraTrees'
test_path = "E:\\DISK\\_workspace\\nathan\\python\\dadosteste.csv"

print(f"Loading model {model_name}")
model = get_model(model_name)
print()

print(f"Loading test data from {test_path}")
df = pd.read_csv(test_path)
df = df.head(5000)
print()

print(f"Predicting")
prediction_proba = model.predict_proba(df.drop(columns=['label']))
print()

prediction = prediction_proba.argmax(axis=1)

print("Prediction:")
print(prediction)

print("Classification Report:")
print(classification_report(df['label'], prediction))

print("Confusion Matrix:")
sns.heatmap(pd.crosstab(df['label'], prediction), fmt="d", annot=True)
plt.show()
