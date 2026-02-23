"""Checkpoint 1: Test add() and get_all() methods."""
import sys
import unittest

sys.path.insert(0, '/tmp/workspace')
from solution import TodoStore


class TestAddAndGetAll(unittest.TestCase):

    def setUp(self):
        self.store = TodoStore()

    def test_add_returns_item_with_id(self):
        item = self.store.add("Buy groceries")
        self.assertIsNotNone(item, "add() should not return None")
        self.assertIn('id', item, "Item should have 'id' field")
        self.assertIsInstance(item['id'], int, "id should be an integer")

    def test_add_sets_title(self):
        item = self.store.add("Buy groceries")
        self.assertEqual(item['title'], "Buy groceries",
                         f"Expected title 'Buy groceries', got '{item['title']}'")

    def test_add_sets_done_false(self):
        item = self.store.add("Buy groceries")
        self.assertFalse(item['done'], "New items should have done=False")

    def test_ids_are_unique(self):
        item1 = self.store.add("Task 1")
        item2 = self.store.add("Task 2")
        self.assertNotEqual(item1['id'], item2['id'], "Each item should have a unique id")

    def test_get_all_returns_list(self):
        self.store.add("Task 1")
        self.store.add("Task 2")
        items = self.store.get_all()
        self.assertIsInstance(items, list, "get_all() should return a list")
        self.assertEqual(len(items), 2, f"Expected 2 items, got {len(items)}")

    def test_get_all_empty(self):
        items = self.store.get_all()
        self.assertEqual(items, [], "get_all() on empty store should return []")


if __name__ == '__main__':
    suite = unittest.TestLoader().loadTestsFromTestCase(TestAddAndGetAll)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    sys.exit(0 if result.wasSuccessful() else 1)
