import pdfplumber
import pandas as pd
import sys
import re
from datetime import datetime

# Regex para detectar linhas de transação
TRANSACTION_REGEX = re.compile(
    r"(\d{2}/\d{2}/\d{4})\s+(.+?)\s+(-?\d{1,3}(?:\.\d{3})*,\d{2})$"
)

def parse_brl(value):
    """Converte '1.234,56' para float"""
    value = value.replace(".", "").replace(",", ".")
    return float(value)

def extract_transactions(pdf_path):
    transactions = []

    with pdfplumber.open(pdf_path) as pdf:
        for page_number, page in enumerate(pdf.pages):
            text = page.extract_text()

            if not text:
                continue

            lines = text.split("\n")

            for line in lines:
                line = line.strip()

                match = TRANSACTION_REGEX.match(line)

                if match:
                    date_str, description, amount_str = match.groups()

                    try:
                        date = datetime.strptime(date_str, "%d/%m/%Y")
                        amount = parse_brl(amount_str)

                        transactions.append({
                            "date": date,
                            "description": description.strip(),
                            "amount": amount,
                            "type": "credit" if amount > 0 else "debit",
                            "page": page_number + 1
                        })

                    except Exception as e:
                        print(f"Erro ao processar linha: {line}")
                        print(e)

    return pd.DataFrame(transactions)

def save_to_csv(df, output_path):
    df = df.sort_values("date")
    df.to_csv(output_path, index=False, encoding="utf-8")

def main():
    pdf_path = sys.argv[1] if len(sys.argv) > 1 else "itau_extrato.pdf"
    output_path = sys.argv[2] if len(sys.argv) > 2 else "extrato.csv"

    df = extract_transactions(pdf_path)

    print(f"{len(df)} transações encontradas")

    print("\nPrimeiras transações:")
    print(df.head())

    save_to_csv(df, output_path)

    print(f"\nSalvo em {output_path}")

if __name__ == "__main__":
    main()
