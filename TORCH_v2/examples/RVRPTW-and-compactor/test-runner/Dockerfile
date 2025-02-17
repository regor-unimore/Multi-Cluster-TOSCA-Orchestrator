FROM python:3.11-slim
COPY app/test_runner.py /app/test_runner.py
COPY app/requirements.txt requirements.txt
RUN pip install -r requirements.txt

WORKDIR /app
CMD ["python", "test_runner.py"]
