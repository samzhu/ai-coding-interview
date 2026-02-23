"""Checkpoint 2: Test BFS shortest path solver."""
import sys
import unittest

sys.path.insert(0, '/tmp/workspace')
from maze import Maze
from solver import find_shortest_path


class TestBFSSolver(unittest.TestCase):

    def test_simple_path(self):
        # 3x3 grid with center wall; shortest path goes around
        grid = [
            [0, 0, 0],
            [0, 1, 0],
            [0, 0, 0],
        ]
        maze = Maze(grid)
        result = find_shortest_path(maze)
        self.assertEqual(result, 4, f"Expected 4 steps, got {result}")

    def test_straight_path(self):
        # 1x4 grid; straight line from left to right
        grid = [[0, 0, 0, 0]]
        maze = Maze(grid)
        result = find_shortest_path(maze)
        self.assertEqual(result, 3, f"Expected 3 steps, got {result}")

    def test_no_path(self):
        # 2x2 grid with walls blocking all routes
        grid = [
            [0, 1],
            [1, 0],
        ]
        maze = Maze(grid)
        result = find_shortest_path(maze)
        self.assertEqual(result, -1, f"Expected -1 (no path), got {result}")

    def test_single_cell(self):
        # 1x1 grid; start equals end
        grid = [[0]]
        maze = Maze(grid)
        result = find_shortest_path(maze)
        self.assertEqual(result, 0, f"Expected 0 (start == end), got {result}")


if __name__ == '__main__':
    suite = unittest.TestLoader().loadTestsFromTestCase(TestBFSSolver)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    sys.exit(0 if result.wasSuccessful() else 1)
