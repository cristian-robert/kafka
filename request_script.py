import json
import requests
import pandas as pd
import sys
import os
from pathlib import Path
from request_functions import make_request
import openpyxl

def print_flush(*args, **kwargs):
    print(*args, **kwargs)
    sys.stdout.flush()

def main():
    if getattr(sys, 'frozen', False):
        excel_path = Path(os.path.dirname(sys.executable)) / 'transactions.xlsx'
    else:
        excel_path = Path(__file__).parent / 'transactions.xlsx'
    
    try:
        # Read Excel file using openpyxl engine
        df = pd.read_excel(
            excel_path,
            engine='openpyxl',
            dtype={'account': str}
        )
        
        # Convert any numeric account numbers to string with proper formatting
        df['account'] = df['account'].apply(lambda x: f"{x:0>10}" if pd.notnull(x) else '')
        
        print_flush("Excel columns:", df.columns.tolist())
        print_flush("First row:", df.iloc[0].to_dict())
        
        for index, row in df.iterrows():
            print_flush(f"\nProcessing row {index}:")
            print_flush(row.to_dict())
            make_request(row)
            
    except FileNotFoundError:
        print_flush(f"transactions.xlsx not found at: {excel_path}")
    except Exception as e:
        print_flush(f"Error type: {type(e)}")
        print_flush(f"Error: {str(e)}")
        print_flush("Full row data:", row if 'row' in locals() else "No row data")
    
    input("Press Enter to exit...")
