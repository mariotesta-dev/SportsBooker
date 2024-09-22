import sqlite3
from datetime import date, timedelta, datetime

# Connect to the SQLite database
conn = sqlite3.connect('sample.db')
cursor = conn.cursor()

# Define the court IDs
court_ids = [1, 2, 3]

# Define the start and end dates for reservations
start_date = date.today()
end_date = start_date + timedelta(days=7)

# Define the start and end times for reservations
start_time = datetime.strptime("09:00", "%H:%M")
end_time = datetime.strptime("22:00", "%H:%M")

# Generate and insert reservations into the database
reservation_id = 1
current_date = start_date
while current_date <= end_date:
    current_time = start_time
    while current_time <= end_time:
        for court_id in court_ids:
            # Insert a reservation record for each court on the current date and time
            reservation_datetime = datetime.combine(current_date, current_time.time())
            query = "INSERT INTO reservations (reservationId, courtId, date, time) VALUES (?, ?, ?, ?)"
            cursor.execute(query, (reservation_id, court_id, current_datetime.date(), current_datetime.time()))
            reservation_id += 1
        
        # Move to the next time
        current_time += timedelta(minutes=90)
    
    # Move to the next date
    current_date += timedelta(days=1)

# Commit the changes and close the connection
conn.commit()
conn.close()
