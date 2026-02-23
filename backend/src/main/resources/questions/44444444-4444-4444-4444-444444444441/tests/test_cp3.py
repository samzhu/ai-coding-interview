"""Checkpoint 3: Edge cases and larger grid verification."""
import sys
import unittest

sys.path.insert(0, '/tmp/workspace')
from maze import Maze
from solver import find_shortest_path


class TestEdgeCases(unittest.TestCase):

    def test_start_equals_end(self):
        # 1x1 grid: start and end are the same cell
        grid = [[0]]
        maze = Maze(grid)
        result = find_shortest_path(maze)
        self.assertEqual(result, 0)

    def test_fully_blocked(self):
        # No path exists because center cross is all walls
        grid = [
            [0, 1, 0],
            [1, 1, 1],
            [0, 1, 0],
        ]
        maze = Maze(grid)
        result = find_shortest_path(maze)
        self.assertEqual(result, -1)

    def test_multiple_paths_returns_shortest(self):
        # Open 3x3 grid: two possible paths, both 4 steps (no diagonal)
        grid = [
            [0, 0, 0],
            [0, 0, 0],
            [0, 0, 0],
        ]
        maze = Maze(grid)
        result = find_shortest_path(maze)
        self.assertEqual(result, 4, "Shortest path in open 3x3 grid should be 4 steps (no diagonal)")

    def test_large_open_grid(self):
        # 10x10 open grid: minimum path = 9 + 9 = 18 steps
        size = 10
        grid = [[0] * size for _ in range(size)]
        maze = Maze(grid)
        result = find_shortest_path(maze)
        self.assertEqual(result, 18, f"Expected 18 steps in 10x10 open grid, got {result}")

    def test_winding_path(self):
        # Snake-like maze with only one winding route (16 steps)
        grid = [
            [0, 0, 0, 0, 0],
            [1, 1, 1, 1, 0],
            [0, 0, 0, 0, 0],
            [0, 1, 1, 1, 1],
            [0, 0, 0, 0, 0],
        ]
        maze = Maze(grid)
        result = find_shortest_path(maze)
        self.assertEqual(result, 16, f"Expected 16 steps in winding maze, got {result}")


if __name__ == '__main__':
    suite = unittest.TestLoader().loadTestsFromTestCase(TestEdgeCases)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    sys.exit(0 if result.wasSuccessful() else 1)
