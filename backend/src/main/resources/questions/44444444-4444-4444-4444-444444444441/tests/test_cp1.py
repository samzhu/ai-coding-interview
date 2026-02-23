"""Checkpoint 1: Test Maze class parsing and boundary detection."""
import sys
import unittest

sys.path.insert(0, '/tmp/workspace')
from maze import Maze


class TestMazeParsing(unittest.TestCase):

    def setUp(self):
        self.grid_3x3 = [
            [0, 0, 0],
            [0, 1, 0],
            [0, 0, 0],
        ]
        self.maze = Maze(self.grid_3x3)

    def test_maze_dimensions(self):
        self.assertEqual(self.maze.rows, 3)
        self.assertEqual(self.maze.cols, 3)

    def test_get_start(self):
        start = self.maze.get_start()
        self.assertEqual(start, (0, 0))

    def test_get_end(self):
        end = self.maze.get_end()
        self.assertEqual(end, (2, 2), f"Expected (2, 2) but got {end}")

    def test_is_valid_open_cell(self):
        self.assertTrue(self.maze.is_valid(0, 0))

    def test_is_valid_wall(self):
        self.assertFalse(self.maze.is_valid(1, 1))

    def test_is_valid_out_of_bounds(self):
        self.assertFalse(self.maze.is_valid(-1, 0))
        self.assertFalse(self.maze.is_valid(0, -1))
        self.assertFalse(self.maze.is_valid(3, 0))
        self.assertFalse(self.maze.is_valid(0, 3))

    def test_get_neighbors_avoids_walls(self):
        neighbors = self.maze.get_neighbors(1, 0)
        self.assertIn((0, 0), neighbors)
        self.assertIn((2, 0), neighbors)
        self.assertNotIn((1, 1), neighbors)


if __name__ == '__main__':
    suite = unittest.TestLoader().loadTestsFromTestCase(TestMazeParsing)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    sys.exit(0 if result.wasSuccessful() else 1)
