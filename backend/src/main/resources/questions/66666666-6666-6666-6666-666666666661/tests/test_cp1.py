"""Checkpoint 1: Test CSV parsing correctness, especially quoted fields."""
import sys
import unittest

sys.path.insert(0, '/tmp/workspace')
from csv_reader import parse_csv


class TestCsvParser(unittest.TestCase):

    def test_simple_csv(self):
        text = "name,amount\nAlice,100.0\nBob,200.0"
        records = parse_csv(text)
        self.assertEqual(len(records), 2)
        self.assertEqual(records[0]['name'], 'Alice')
        self.assertAlmostEqual(records[0]['amount'], 100.0)

    def test_amount_is_float(self):
        text = "name,amount\nAlice,99.50"
        records = parse_csv(text)
        self.assertIsInstance(records[0]['amount'], float)

    def test_quoted_field_with_comma(self):
        # "Smith, John" is one field — split(',') breaks this!
        text = 'name,amount\n"Smith, John",150.0\nBob,200.0'
        records = parse_csv(text)
        self.assertEqual(len(records), 2,
            f"Expected 2 records, got {len(records)} — did you use csv module instead of split(',')?")
        self.assertEqual(records[0]['name'], 'Smith, John',
            f"Expected 'Smith, John', got '{records[0]['name']}' — quoted fields must not be split")

    def test_empty_csv_returns_empty_list(self):
        records = parse_csv("name,amount")
        self.assertEqual(records, [])

    def test_multiple_rows(self):
        text = "product,amount\nApple,10.0\nBanana,20.0\nCherry,30.0"
        records = parse_csv(text)
        self.assertEqual(len(records), 3)
        self.assertEqual(records[2]['product'], 'Cherry')


if __name__ == '__main__':
    suite = unittest.TestLoader().loadTestsFromTestCase(TestCsvParser)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    sys.exit(0 if result.wasSuccessful() else 1)
