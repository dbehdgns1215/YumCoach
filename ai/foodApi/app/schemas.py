from pydantic import BaseModel, Field
from typing import Optional, List


class FoodImageAnalysisRequest(BaseModel):
    image_url: str
    extra_text: Optional[str] = None


class SimpleFoodItem(BaseModel):
    name: str = Field(..., description="food name in Korean")
    portion: Optional[str] = Field(None, description="e.g., 1 그릇, 2 조각, 반접시")


class SimpleFoodListResult(BaseModel):
    items: List[SimpleFoodItem]


class FoodItem(BaseModel):
    name: str = Field(..., description="food name in Korean")
    portion: Optional[str] = Field(
        None, description="e.g., 1 그릇, 2 조각, 반접시")
    calories_kcal: Optional[float] = None
    protein_g: Optional[float] = None
    fat_g: Optional[float] = None
    carbs_g: Optional[float] = None


class FoodAnalysisResult(BaseModel):
    """Single food analysis result"""
    name: str = Field(..., description="food name in Korean")
    portion: Optional[str] = Field(None, description="e.g., 1 그릇, 2 조각, 반접시")
    calories_kcal: Optional[float] = None
    protein_g: Optional[float] = None
    fat_g: Optional[float] = None
    carbs_g: Optional[float] = None


class MultiFoodAnalysisResult(BaseModel):
    items: List[FoodItem]
    total_calories_kcal: Optional[float] = None
    total_protein_g: Optional[float] = None
    total_fat_g: Optional[float] = None
    total_carbs_g: Optional[float] = None
