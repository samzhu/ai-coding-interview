"""Sales data analyzer module."""


def total_sales(records: list) -> float:
    """Return the sum of all 'amount' values.

    Args:
        records: List of dicts with an 'amount' key (float).

    Returns:
        Sum of all amounts, or 0.0 if records is empty.
    """
    # TODO: Implement total_sales()
    # Sum all record['amount'] values
    pass


def average_sales(records: list) -> float:
    """Return the average 'amount' rounded to 2 decimal places.

    Args:
        records: List of dicts with an 'amount' key (float).

    Returns:
        Average amount rounded to 2 decimal places, or 0.0 if empty.
    """
    # TODO: Implement average_sales()
    # Return round(total / count, 2)
    # Return 0.0 if records is empty
    pass


def max_sale(records: list):
    """Return the record with the highest 'amount'.

    Args:
        records: List of dicts with an 'amount' key (float).

    Returns:
        The dict with the maximum amount, or None if records is empty.
    """
    # TODO: Implement max_sale()
    # Use max(records, key=lambda r: r['amount'])
    # Return None if records is empty
    pass


def min_sale(records: list):
    """Return the record with the lowest 'amount'.

    Args:
        records: List of dicts with an 'amount' key (float).

    Returns:
        The dict with the minimum amount, or None if records is empty.
    """
    # TODO: Implement min_sale()
    # Use min(records, key=lambda r: r['amount'])
    # Return None if records is empty
    pass
