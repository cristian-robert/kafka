import json
import requests
from datetime import datetime, timezone

def get_iso_timestamp():
    return datetime.now(timezone.utc).isoformat(timespec='microseconds') + 'Z'

def generate_random_string(length):
    import random
    characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
    result = ''
    for i in range(length):
        result += characters[int(random.random() * len(characters))]
    return result

def make_request(row):
    url = "urlToMakeRequestTo"
    
    # Generate the required variables
    current_date = datetime.now().strftime("%Y-%m-%d")
    message_id = generate_random_string(36)
    end_to_end_id = generate_random_string(35)
    creditor_account_id = generate_random_string(34)
    
    payload = json.dumps({
        "cstmCDT": {
            "grHd": {
                "current_date": current_date,
                "message_id": message_id,
                "end_to_end_id": end_to_end_id,
                "creditor_account_id": creditor_account_id,
                # Add other fields from your CSV row as needed
                "aabb": row['a'],
                "ctlSum": row['b'],
                "msg": row['c']
            }
        }
    })
    
    headers = {'Content-Type': 'application/json'}
    
    try:
        response = requests.post(url, headers=headers, data=payload)
        response.raise_for_status()
        print(f"Success for row {row.name}")
        print(f"Used message_id: {message_id}")
        print(f"Used end_to_end_id: {end_to_end_id}")
        print(f"Used creditor_account_id: {creditor_account_id}")
        return response.json()
    except requests.exceptions.RequestException as e:
        print(f"Error for row {row.name}: {str(e)}")
        return None
