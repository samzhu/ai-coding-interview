"""CSV reader module for parsing product sales data."""


def parse_csv(text: str) -> list:
    """Parse a CSV string into a list of dicts.

    The first row is treated as the header.
    Each subsequent row becomes a dict with header keys.
    The 'amount' field is converted to float.

    BUG: Using str.split(',') does not handle quoted fields correctly.
    Example: "Smith, John",100.0 would be split into 3 parts instead of 2.

    Args:
        text: A CSV-formatted string with a header row.

    Returns:
        A list of dicts, one per data row.
    """
    lines = text.strip().splitlines()
    if not lines:
        return []

    # BUG: split(',') breaks on quoted fields like "Smith, John"
    headers = [h.strip() for h in lines[0].split(',')]
    records = []
    for line in lines[1:]:
        if not line.strip():
            continue
        # BUG: same split(',') problem here
        values = [v.strip() for v in line.split(',')]
        row = dict(zip(headers, values))
        if 'amount' in row:
            row['amount'] = float(row['amount'])
        records.append(row)
    return records
