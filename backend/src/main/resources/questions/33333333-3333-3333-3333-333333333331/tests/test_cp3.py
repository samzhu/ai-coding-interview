"""Checkpoint 3: Test edge cases and data integrity."""
import sys
import unittest

sys.path.insert(0, '/tmp/workspace')
from solution import TodoStore


class TestEdgeCases(unittest.TestCase):

    def setUp(self):
        self.store = TodoStore()

    def test_add_multiple_items_sequential_ids(self):
        items = [self.store.add(f"Task {i}") for i in range(5)]
        ids = [item['id'] for item in items]
        self.assertEqual(ids, sorted(ids), "IDs should be in increasing order")

    def test_get_all_preserves_order(self):
        titles = ["Alpha", "Beta", "Gamma"]
        for t in titles:
            self.store.add(t)
        result_titles = [item['title'] for item in self.store.get_all()]
        self.assertEqual(result_titles, titles, "Items should be returned in insertion order")

    def test_mark_done_does_not_affect_other_items(self):
        item1 = self.store.add("Task 1")
        item2 = self.store.add("Task 2")
        self.store.mark_done(item1['id'])
        all_items = self.store.get_all()
        item2_state = next(i for i in all_items if i['id'] == item2['id'])
        self.assertFalse(item2_state['done'], "Marking one item done should not affect others")

    def test_mark_done_twice_is_idempotent(self):
        item = self.store.add("Task 1")
        self.store.mark_done(item['id'])
        result = self.store.mark_done(item['id'])
        self.assertTrue(result['done'], "Marking done twice should still be done=True")

    def test_add_empty_title(self):
        item = self.store.add("")
        self.assertEqual(item['title'], "")


if __name__ == '__main__':
    suite = unittest.TestLoader().loadTestsFromTestCase(TestEdgeCases)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    sys.exit(0 if result.wasSuccessful() else 1)
