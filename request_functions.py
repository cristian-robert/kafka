import json
import requests
import sys
import uuid
from datetime import datetime, timezone

def print_flush(*args, **kwargs):
    print(*args, **kwargs)
    sys.stdout.flush()

def get_uetr():
    return str(uuid.uuid4())

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
                "aabb": str(row['account']),
                "ctlSum": float(row['sum']),
                "msg": row['message']
            }
        }
    })
    
    headers = {'Content-Type': 'application/json'}
    
    try:
        print_flush(f"\nSending request for row {row.name}...")
        print_flush(f"Payload: {payload}")
        response = requests.post(url, headers=headers, data=payload)
        print_flush(f"Status Code: {response.status_code}")
        print_flush(f"Response: {response.text}\n")
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        print_flush(f"Error for row {row.name}: {str(e)}")
        return None
