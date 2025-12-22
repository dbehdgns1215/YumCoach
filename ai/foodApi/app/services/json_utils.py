import json


def extract_json_object(text: str) -> dict:
    """
    Extract first top-level JSON object from a string.
    """
    start = text.find("{")
    end = text.rfind("}")
    if start == -1 or end == -1 or end <= start:
        raise ValueError("No JSON object found in model output")
    candidate = text[start: end + 1]
    return json.loads(candidate)
