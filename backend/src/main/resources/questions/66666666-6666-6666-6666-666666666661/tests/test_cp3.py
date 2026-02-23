"""Checkpoint 3: Edge cases — empty data, negative amounts, float precision."""
import sys
import unittest

sys.path.insert(0, '/tmp/workspace')
from csv_reader import parse_csv
from analyzer import total_sales, average_sales, max_sale, min_sale


class TestEdgeCases(unittest.TestCase):

    def test_single_record_max_equals_min(self):
        text = "product,amount\nOnlyOne,42.0"
        records = parse_csv(text)
        mx = max_sale(records)
        mn = min_sale(records)
        self.assertIsNotNone(mx)
        self.assertIsNotNone(mn)
        self.assertAlmostEqual(mx['amount'], mn['amount'])

    def test_negative_amounts(self):
        text = "product,amount\nRefund,-50.0\nSale,100.0"
        records = parse_csv(text)
        mn = min_sale(records)
        self.assertAlmostEqual(mn['amount'], -50.0,
            msg="min_sale should handle negative amounts")

    def test_total_with_negative(self):
        text = "product,amount\nRefund,-50.0\nSale,100.0"
        records = parse_csv(text)
        result = total_sales(records)
        self.assertAlmostEqual(result, 50.0)

    def test_average_precision(self):
        text = "product,amount\nA,1.0\nB,2.0\nC,3.0"
        records = parse_csv(text)
        result = average_sales(records)
        self.assertAlmostEqual(result, 2.0, places=2)

    def test_quoted_field_in_full_pipeline(self):
        text = 'seller,amount\n"Johnson, Mark",250.50\n"Lee, Amy",180.25'
        records = parse_csv(text)
        self.assertEqual(len(records), 2)
        result = total_sales(records)
        self.assertAlmostEqual(result, 430.75, places=2)

    def test_csv_header_only_empty_analysis(self):
        records = parse_csv("product,amount")
        self.assertEqual(total_sales(records), 0.0)
        self.assertEqual(average_sales(records), 0.0)
        self.assertIsNone(max_sale(records))
        self.assertIsNone(min_sale(records))


if __name__ == '__main__':
    suite = unittest.TestLoader().loadTestsFromTestCase(TestEdgeCases)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    sys.exit(0 if result.wasSuccessful() else 1)
