"""Checkpoint 3: Edge cases — cycles, isolated tasks, empty graph."""
import sys
import unittest

sys.path.insert(0, '/tmp/workspace')
from scheduler import topological_sort


class TestEdgeCases(unittest.TestCase):

    def test_cycle_detection(self):
        # A -> B -> C -> A (circular dependency)
        tasks = [
            {'id': 'A', 'priority': 1},
            {'id': 'B', 'priority': 2},
            {'id': 'C', 'priority': 3},
        ]
        # A depends on C, B depends on A, C depends on B -> cycle
        deps = [('A', 'C'), ('B', 'A'), ('C', 'B')]
        with self.assertRaises(ValueError) as ctx:
            topological_sort(tasks, deps)
        self.assertIn('cycle', str(ctx.exception).lower(),
            "ValueError message should mention 'cycle'")

    def test_self_dependency_cycle(self):
        # A depends on itself
        tasks = [{'id': 'A', 'priority': 1}]
        deps = [('A', 'A')]
        with self.assertRaises(ValueError):
            topological_sort(tasks, deps)

    def test_isolated_tasks_included(self):
        # Tasks with no dependencies should all appear in result
        tasks = [
            {'id': 'X', 'priority': 5},
            {'id': 'Y', 'priority': 3},
            {'id': 'Z', 'priority': 1},
        ]
        result = topological_sort(tasks, [])
        self.assertEqual(sorted(result), ['X', 'Y', 'Z'])
        # Priority order: Z(1) first, Y(3) second, X(5) last
        self.assertEqual(result, ['Z', 'Y', 'X'])

    def test_empty_graph(self):
        result = topological_sort([], [])
        self.assertEqual(result, [])

    def test_mixed_connected_and_isolated(self):
        # A -> B (chain) and C is isolated
        tasks = [
            {'id': 'A', 'priority': 1},
            {'id': 'B', 'priority': 2},
            {'id': 'C', 'priority': 1},  # isolated, same priority as A
        ]
        deps = [('B', 'A')]  # B depends on A
        result = topological_sort(tasks, deps)
        self.assertEqual(len(result), 3)
        self.assertLess(result.index('A'), result.index('B'),
            "A must come before B")
        self.assertIn('C', result, "Isolated task C must be in result")


if __name__ == '__main__':
    suite = unittest.TestLoader().loadTestsFromTestCase(TestEdgeCases)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    sys.exit(0 if result.wasSuccessful() else 1)
