# AI Runner

## Get Started

### Setup

```bash
# Create a virtual environment
python -m venv venv

# Activate the virtual environment
source venv/bin/activate
./venv/Scripts/activate.ps1 # Windows

# Install the required packages
pip install -r requirements.txt
```

### Running

```bash
python main.py randomforest --api --port 5000
python main.py MLP --api --port 5001
python main.py LogisticReg --api --port 5002
python main.py KNN --api --port 5003
python main.py decisiontree --api --port 5004
python main.py SVM --api --port 5005
```

## Running the project

Start with docker-compose:

```bash
docker-compose --project-name ai-agent up --build --remove-orphans --detach
docker-compose --project-name ai-agent up --detach
```

Stop with docker-compose:

```bash
docker-compose --project-name ai-agent down --remove-orphans
```
