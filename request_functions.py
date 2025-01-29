import json
import requests

def make_request(row):
    url = "urlToMakeRequestTo"
    
    payload = json.dumps({
        "cstmCDT": {
            "grHd": {
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
        return response.json()
    except requests.exceptions.RequestException as e:
        print(f"Error for row {row.name}: {str(e)}")
        return None
