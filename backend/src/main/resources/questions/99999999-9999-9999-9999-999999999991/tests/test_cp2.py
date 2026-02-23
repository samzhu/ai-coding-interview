"""Checkpoint 2: Test topological sort with priority."""
import sys
import unittest

sys.path.insert(0, '/tmp/workspace')
from scheduler import topological_sort


class TestTopologicalSort(unittest.TestCase):

    def test_simple_chain(self):
        # A -> B -> C (C depends on B, B depends on A)
        tasks = [
            {'id': 'A', 'priority': 1},
            {'id': 'B', 'priority': 2},
            {'id': 'C', 'priority': 3},
        ]
        deps = [('B', 'A'), ('C', 'B')]
        result = topological_sort(tasks, deps)
        self.assertEqual(result, ['A', 'B', 'C'])

    def test_priority_breaks_ties(self):
        # A and B are independent; A has priority 1, B has priority 2
        # A should come first (lower priority number = higher priority)
        tasks = [
            {'id': 'A', 'priority': 1},
            {'id': 'B', 'priority': 2},
        ]
        result = topological_sort(tasks, [])
        self.assertEqual(result[0], 'A',
            "Task with lower priority number should be scheduled first")

    def test_dependency_order_respected(self):
        # C depends on A; D depends on B; A=priority 1, B=priority 2
        tasks = [
            {'id': 'A', 'priority': 1},
            {'id': 'B', 'priority': 2},
            {'id': 'C', 'priority': 1},
            {'id': 'D', 'priority': 2},
        ]
        deps = [('C', 'A'), ('D', 'B')]
        result = topological_sort(tasks, deps)

        # A must come before C, B must come before D
        self.assertLess(result.index('A'), result.index('C'),
            "A must appear before C")
        self.assertLess(result.index('B'), result.index('D'),
            "B must appear before D")

    def test_empty_tasks(self):
        result = topological_sort([], [])
        self.assertEqual(result, [])

    def test_single_task(self):
        tasks = [{'id': 'solo', 'priority': 1}]
        result = topological_sort(tasks, [])
        self.assertEqual(result, ['solo'])

    def test_all_tasks_included(self):
        tasks = [
            {'id': 'A', 'priority': 3},
            {'id': 'B', 'priority': 1},
            {'id': 'C', 'priority': 2},
        ]
        result = topological_sort(tasks, [])
        self.assertEqual(sorted(result), ['A', 'B', 'C'],
            "All tasks must appear in result")


if __name__ == '__main__':
    suite = unittest.TestLoader().loadTestsFromTestCase(TestTopologicalSort)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    sys.exit(0 if result.wasSuccessful() else 1)
