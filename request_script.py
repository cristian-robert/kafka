import json
import requests
import pandas as pd
import sys
import os
from pathlib import Path
from request_functions import make_request

def print_flush(*args, **kwargs):
    print(*args, **kwargs)
    sys.stdout.flush()

def main():
    if getattr(sys, 'frozen', False):
        csv_path = Path(os.path.dirname(sys.executable)) / 'transactions.csv'
    else:
        csv_path = Path(__file__).parent / 'transactions.csv'
    
    try:
        df = pd.read_csv(csv_path, dtype={'account': str})
        print_flush("CSV columns:", df.columns.tolist())
        print_flush("First row:", df.iloc[0].to_dict())
        
        for index, row in df.iterrows():
            print_flush(f"\nProcessing row {index}:")
            print_flush(row.to_dict())
            make_request(row)
            
    except FileNotFoundError:
        print_flush(f"transactions.csv not found at: {csv_path}")
    except Exception as e:
        print_flush(f"Error type: {type(e)}")
        print_flush(f"Error: {str(e)}")
        print_flush("Full row data:", row if 'row' in locals() else "No row data")
    
    input("Press Enter to exit...")
