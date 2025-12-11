package com.ssafy.yumcoach.food.model;

import lombok.Data;

@Data
public class NutritionFactsPrimaryDto {

    private Long nutritionId;
    private String foodId;

    private Double energyKcal;
    private Double waterG;
    private Double proteinG;
    private Double fatG;
    private Double ashG;
    private Double carbohydrateG;
    private Double sugarsG;
    private Double dietaryFiberG;

    private Double calciumMg;
    private Double ironMg;
    private Double phosphorusMg;
    private Double potassiumMg;
    private Double sodiumMg;

    private Double vitaminARae;
    private Double retinolUg;
    private Double betaCaroteneUg;

    private Double thiaminMg;
    private Double riboflavinMg;
    private Double niacinMg;
    private Double vitaminCMg;
    private Double vitaminDUg;

    private Double cholesterolMg;
    private Double saturatedFatG;
    private Double transFatG;
    private Double unsaturatedFatG;

    private Double caffeineMg;
    private Double vitaminEMg;
    private Double vitaminETocotrienolMg;

}
