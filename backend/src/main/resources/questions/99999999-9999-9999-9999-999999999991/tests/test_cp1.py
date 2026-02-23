"""Checkpoint 1: Test dependency graph builder."""
import sys
import unittest

sys.path.insert(0, '/tmp/workspace')
from graph import build_graph


class TestBuildGraph(unittest.TestCase):

    def test_simple_dependency(self):
        # A depends on B: B -> A (B must run before A)
        tasks = [{'id': 'A', 'priority': 1}, {'id': 'B', 'priority': 2}]
        deps = [('A', 'B')]  # A depends on B
        g = build_graph(tasks, deps)

        # B -> A means adj[B] should contain A
        self.assertIn('A', g['adj']['B'],
            "adj[B] should contain A (B -> A means B runs before A)")

        # A has in_degree 1 (depends on B)
        self.assertEqual(g['in_degree']['A'], 1,
            "in_degree[A] should be 1 (A depends on B)")

        # B has in_degree 0 (no dependencies)
        self.assertEqual(g['in_degree']['B'], 0,
            "in_degree[B] should be 0 (B has no dependencies)")

    def test_chain_dependency(self):
        # C depends on B, B depends on A: order must be A -> B -> C
        tasks = [
            {'id': 'A', 'priority': 1},
            {'id': 'B', 'priority': 2},
            {'id': 'C', 'priority': 3},
        ]
        deps = [('B', 'A'), ('C', 'B')]  # B depends on A, C depends on B
        g = build_graph(tasks, deps)

        self.assertIn('B', g['adj']['A'], "adj[A] should contain B")
        self.assertIn('C', g['adj']['B'], "adj[B] should contain C")
        self.assertEqual(g['in_degree']['A'], 0, "A has no dependencies")
        self.assertEqual(g['in_degree']['B'], 1, "B depends on A")
        self.assertEqual(g['in_degree']['C'], 1, "C depends on B")

    def test_no_dependencies(self):
        tasks = [{'id': 'X', 'priority': 1}, {'id': 'Y', 'priority': 2}]
        g = build_graph(tasks, [])
        self.assertEqual(g['in_degree']['X'], 0)
        self.assertEqual(g['in_degree']['Y'], 0)
        self.assertEqual(g['adj']['X'], [])
        self.assertEqual(g['adj']['Y'], [])


if __name__ == '__main__':
    suite = unittest.TestLoader().loadTestsFromTestCase(TestBuildGraph)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    sys.exit(0 if result.wasSuccessful() else 1)
