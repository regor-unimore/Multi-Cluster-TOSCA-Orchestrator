FROM python:3.10-slim-bookworm
COPY app/my_solver_flask.py /app/my_solver.py
COPY app/requirements.txt requirements.txt
RUN pip install -r requirements.txt

WORKDIR /app
CMD ["python", "my_solver.py"]
