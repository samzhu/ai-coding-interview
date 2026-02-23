"""Checkpoint 2: Test mark_done() and get_pending() methods."""
import sys
import unittest

sys.path.insert(0, '/tmp/workspace')
from solution import TodoStore


class TestMarkDoneAndGetPending(unittest.TestCase):

    def setUp(self):
        self.store = TodoStore()

    def test_mark_done_returns_item(self):
        item = self.store.add("Write tests")
        updated = self.store.mark_done(item['id'])
        self.assertIsNotNone(updated, "mark_done() should return the updated item")

    def test_mark_done_sets_done_true(self):
        item = self.store.add("Write tests")
        updated = self.store.mark_done(item['id'])
        self.assertTrue(updated['done'], "mark_done() should set done=True")

    def test_mark_done_not_found_raises_value_error(self):
        with self.assertRaises(ValueError):
            self.store.mark_done(999)

    def test_get_pending_excludes_done(self):
        item1 = self.store.add("Task 1")
        self.store.add("Task 2")
        self.store.mark_done(item1['id'])
        pending = self.store.get_pending()
        self.assertEqual(len(pending), 1, f"Expected 1 pending item, got {len(pending)}")
        self.assertEqual(pending[0]['title'], "Task 2")

    def test_get_pending_all_pending(self):
        self.store.add("Task 1")
        self.store.add("Task 2")
        pending = self.store.get_pending()
        self.assertEqual(len(pending), 2)


if __name__ == '__main__':
    suite = unittest.TestLoader().loadTestsFromTestCase(TestMarkDoneAndGetPending)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    sys.exit(0 if result.wasSuccessful() else 1)
