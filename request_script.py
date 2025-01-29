import json
import requests
import pandas as pd
import sys
import os
from pathlib import Path
from request_functions import make_request

def main():
    if getattr(sys, 'frozen', False):
        csv_path = Path(os.path.dirname(sys.executable)) / 'transactions.csv'
    else:
        csv_path = Path(__file__).parent / 'transactions.csv'
    
    try:
        df = pd.read_csv(csv_path, dtype={'account': str})
        print("CSV columns:", df.columns.tolist())  # Debug: show columns
        print("First row:", df.iloc[0].to_dict())   # Debug: show first row
        
        for index, row in df.iterrows():
            print(f"\nProcessing row {index}:")
            print(row.to_dict())  # Debug: show current row
            make_request(row)
            
    except FileNotFoundError:
        print(f"transactions.csv not found at: {csv_path}")
    except Exception as e:
        print(f"Error type: {type(e)}")
        print(f"Error: {str(e)}")
        print("Full row data:", row if 'row' in locals() else "No row data")
    
    input("Press Enter to exit...")
