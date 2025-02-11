import sqlite3

connection = sqlite3.connect('distancematrix.db')

with open('schema.sql') as f:
    connection.executescript(f.read())



