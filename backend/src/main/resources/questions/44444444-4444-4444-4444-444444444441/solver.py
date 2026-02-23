from collections import deque
from maze import Maze


def find_shortest_path(maze: Maze) -> int:
    """Find the length of the shortest path from start to end.

    Uses BFS (Breadth-First Search) to guarantee the shortest path.

    Args:
        maze: A Maze object with grid, start, and end positions.

    Returns:
        The number of moves in the shortest path, or -1 if no path exists.
        Returns 0 if start equals end.
    """
    # TODO: Implement BFS shortest path algorithm
    # Hint: Use collections.deque for the BFS queue
    # Hint: Track visited cells to avoid infinite loops
    # Hint: Return 0 immediately if start == end
    pass
