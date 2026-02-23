"""Checkpoint 2: Test statistical analysis functions."""
import sys
import math
import unittest

sys.path.insert(0, '/tmp/workspace')
from csv_reader import parse_csv
from analyzer import total_sales, average_sales, max_sale, min_sale


SAMPLE_CSV = """product,seller,amount
Apple,Alice,100.0
Banana,Bob,200.0
Cherry,Alice,50.0
Date,Bob,150.0"""


class TestAnalyzer(unittest.TestCase):

    def setUp(self):
        self.records = parse_csv(SAMPLE_CSV)

    def test_total_sales(self):
        result = total_sales(self.records)
        self.assertAlmostEqual(result, 500.0, places=2)

    def test_average_sales(self):
        result = average_sales(self.records)
        self.assertAlmostEqual(result, 125.0, places=2)

    def test_average_sales_rounding(self):
        text = "product,amount\nA,100.0\nB,200.0\nC,10.0"
        records = parse_csv(text)
        result = average_sales(records)
        # 310 / 3 = 103.333... -> rounds to 103.33
        self.assertEqual(result, round(310/3, 2))

    def test_max_sale(self):
        result = max_sale(self.records)
        self.assertIsNotNone(result)
        self.assertEqual(result['product'], 'Banana')
        self.assertAlmostEqual(result['amount'], 200.0)

    def test_min_sale(self):
        result = min_sale(self.records)
        self.assertIsNotNone(result)
        self.assertEqual(result['product'], 'Cherry')
        self.assertAlmostEqual(result['amount'], 50.0)

    def test_total_sales_empty(self):
        result = total_sales([])
        self.assertEqual(result, 0.0)

    def test_average_sales_empty(self):
        result = average_sales([])
        self.assertEqual(result, 0.0)

    def test_max_sale_empty(self):
        result = max_sale([])
        self.assertIsNone(result)

    def test_min_sale_empty(self):
        result = min_sale([])
        self.assertIsNone(result)


if __name__ == '__main__':
    suite = unittest.TestLoader().loadTestsFromTestCase(TestAnalyzer)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    sys.exit(0 if result.wasSuccessful() else 1)
