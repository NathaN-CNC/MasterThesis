FROM python:3.10-slim

RUN apt-get update
RUN apt-get install -y libgomp1

WORKDIR /app

COPY ./requirements.txt .

RUN pip install -r requirements.txt

COPY ./main.py .
COPY ./main_env.py .

CMD ["python", "main_env.py"]
