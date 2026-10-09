import csv
import math
import unittest
from pathlib import Path


class TestTrainingData(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        data_path = (
            Path(__file__).resolve().parents[1]
            / "data"
            / "processed"
            / "training_data.csv"
        )
        with data_path.open(newline="", encoding="utf-8") as data_file:
            reader = csv.DictReader(data_file)
            cls.fieldnames = reader.fieldnames
            cls.rows = list(reader)

    def test_training_data_has_expected_columns_and_rows(self):
        self.assertEqual(
            self.fieldnames,
            [
                "solarForecastKwh",
                "outageProbability",
                "currentTariffRate",
                "peakTariffRate",
                "currentSoc",
                "estimatedLoad",
                "loadSheddingStage",
                "action",
            ],
        )
        self.assertGreater(len(self.rows), 0)

    def test_training_data_values_are_valid(self):
        for row in self.rows:
            numeric_values = {
                column: float(row[column])
                for column in self.fieldnames
                if column != "action"
            }
            self.assertTrue(
                all(math.isfinite(value) for value in numeric_values.values())
            )
            self.assertGreaterEqual(numeric_values["solarForecastKwh"], 0)
            self.assertGreaterEqual(numeric_values["outageProbability"], 0)
            self.assertLessEqual(numeric_values["outageProbability"], 1)
            self.assertGreaterEqual(numeric_values["currentSoc"], 0)
            self.assertLessEqual(numeric_values["currentSoc"], 100)
            self.assertTrue(row["action"].strip())


if __name__ == "__main__":
    unittest.main()
