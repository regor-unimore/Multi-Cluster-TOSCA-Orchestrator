import sqlite3

connection = sqlite3.connect('georeference.db')

with open('schema.sql') as f:
    connection.executescript(f.read())



