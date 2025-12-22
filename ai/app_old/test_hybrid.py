import argparse
import json
import os

from hybrid_food_analyzer import HybridFoodAnalyzer
from food_classification_api import FoodClassificationAPI
from food_quantity_api import FoodQuantityPredictor


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--image', required=True, help='Image path')
    parser.add_argument('--include-quantity',
                        action='store_true', help='Estimate quantity per item')
    args = parser.parse_args()

    # Init models
    classifier = FoodClassificationAPI()

    # Quantity model optional
    qp = None
    if args.include_quantity:
        # guess common weights path
        candidates = [
            os.path.join(os.path.dirname(__file__),
                         'quantity_est/weights/new_opencv_ckpt_b84_e200.pth'),
            './weights/new_opencv_ckpt_b84_e200.pth'
        ]
        for p in candidates:
            if os.path.exists(p):
                qp = FoodQuantityPredictor(p)
                break

    analyzer = HybridFoodAnalyzer(classifier, qp)

    with open(args.image, 'rb') as f:
        image_bytes = f.read()

    res = analyzer.analyze(image_bytes, include_quantity=args.include_quantity)
    print(json.dumps(res, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
