class Maze:
    """A grid-based maze with walls and open paths.

    Grid cells:
        0 = open path
        1 = wall

    Movement: up, down, left, right (no diagonal)
    """

    DIRECTIONS = [(-1, 0), (1, 0), (0, -1), (0, 1)]

    def __init__(self, grid: list):
        self.grid = grid
        self.rows = len(grid)
        self.cols = len(grid[0]) if grid else 0

    def get_start(self) -> tuple:
        """Return the start position (top-left corner)."""
        return (0, 0)

    def get_end(self) -> tuple:
        """Return the end position (bottom-right corner)."""
        # BUG: off-by-one error -- should be (self.rows - 1, self.cols - 1)
        return (self.rows, self.cols)

    def is_valid(self, row: int, col: int) -> bool:
        """Return True if (row, col) is within bounds and not a wall."""
        return (0 <= row < self.rows and
                0 <= col < self.cols and
                self.grid[row][col] == 0)

    def get_neighbors(self, row: int, col: int) -> list:
        """Return all valid neighboring cells from (row, col)."""
        neighbors = []
        for dr, dc in self.DIRECTIONS:
            nr, nc = row + dr, col + dc
            if self.is_valid(nr, nc):
                neighbors.append((nr, nc))
        return neighbors
