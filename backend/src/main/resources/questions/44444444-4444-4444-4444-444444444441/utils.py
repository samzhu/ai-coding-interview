def parse_grid(lines: list) -> list:
    """Parse a list of whitespace-separated strings into a 2D grid of integers.

    Example:
        lines = ["0 0 1", "0 1 0", "0 0 0"]
        returns [[0, 0, 1], [0, 1, 0], [0, 0, 0]]
    """
    return [[int(c) for c in line.split()] for line in lines if line.strip()]
