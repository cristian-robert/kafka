import json
import requests
import pandas as pd
from pathlib import Path
from request_functions import make_request

def main():
    csv_path = Path(__file__).parent / 'transactions.csv'
    
    try:
        df = pd.read_csv(csv_path)
        for _, row in df.iterrows():
            make_request(row)
    except FileNotFoundError:
        print("transactions.csv not found")
    except Exception as e:
        print(f"Error: {str(e)}")
    
    input("Press Enter to exit...")

if __name__ == "__main__":
    main()
