FROM python:3.10
COPY requirements.txt /tmp
RUN pip install -r /tmp/requirements.txt
WORKDIR /app
COPY . /app
RUN mkdir -p /app/georeference/xls
CMD [ "python3", "main.py" ]
