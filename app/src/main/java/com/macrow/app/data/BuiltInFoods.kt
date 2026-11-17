package com.macrow.app.data

/**
 * Common Filipino foods, loaded once on first run. The numbers are typical values rounded
 * for one serving, not lab results: edit any food, or add your own, when yours differs.
 */
internal fun f(
    name: String, serving: String, grams: Int, kcal: Int, p: Double, c: Double, fat: Double,
    fiber: Double, sugar: Double, na: Int, fe: Double, ca: Int, vc: Int, k: Int,
) = Food(
    name = name, servingLabel = serving, servingGrams = grams.toDouble(), kcal = kcal.toDouble(), protein = p, carbs = c, fat = fat,
    fiber = fiber, sugar = sugar, sodiumMg = na.toDouble(), ironMg = fe, calciumMg = ca.toDouble(), vitCMg = vc.toDouble(), potassiumMg = k.toDouble(),
    source = Source.BUILTIN,
)

val FilipinoFoods: List<Food> = listOf(
    // Rice, bread and noodles
    f("Steamed white rice", "1 cup", 160, 205, 4.3, 44.5, 0.4, 0.6, 0.1, 2, 0.2, 16, 0, 55),
    f("Brown rice, cooked", "1 cup", 195, 216, 5.0, 45.0, 1.8, 3.5, 0.7, 10, 0.8, 20, 0, 84),
    f("Sinangag (garlic fried rice)", "1 cup", 180, 290, 5.0, 46.0, 9.0, 1.0, 0.5, 520, 0.8, 20, 1, 80),
    f("Arroz caldo", "1 bowl", 350, 330, 18.0, 45.0, 9.0, 1.0, 1.0, 800, 1.8, 30, 4, 300),
    f("Pandesal", "1 piece", 30, 90, 2.7, 17.0, 1.3, 0.7, 2.0, 150, 0.9, 12, 0, 40),
    f("White bread", "1 slice", 28, 75, 2.5, 14.0, 1.0, 0.8, 1.5, 140, 0.9, 40, 0, 30),
    f("Oatmeal, cooked", "1 cup", 234, 160, 6.0, 28.0, 3.2, 4.0, 0.6, 115, 2.0, 20, 0, 164),
    f("Pancit canton", "1 cup", 140, 260, 7.0, 38.0, 9.0, 2.0, 2.0, 650, 1.5, 20, 1, 110),
    f("Pancit bihon", "1 cup", 150, 240, 8.0, 38.0, 6.0, 2.0, 2.0, 600, 1.2, 25, 4, 150),
    f("Instant noodles", "1 pack", 60, 270, 6.0, 38.0, 11.0, 1.5, 2.0, 1100, 1.5, 20, 0, 100),
    // Meat, fish and eggs
    f("Chicken adobo", "1 cup", 220, 330, 30.0, 6.0, 20.0, 0.5, 2.0, 900, 1.5, 30, 3, 380),
    f("Chicken tinola", "1 bowl", 300, 190, 20.0, 7.0, 9.0, 1.5, 3.0, 650, 1.5, 40, 12, 520),
    f("Pork sinigang", "1 bowl", 350, 320, 24.0, 14.0, 18.0, 2.5, 3.0, 900, 2.5, 60, 20, 700),
    f("Pork adobo", "1 cup", 220, 420, 28.0, 7.0, 31.0, 0.5, 2.0, 950, 1.8, 30, 3, 420),
    f("Beef nilaga", "1 bowl", 350, 330, 28.0, 15.0, 17.0, 3.0, 4.0, 700, 3.4, 60, 22, 800),
    f("Kare-kare", "1 cup", 250, 420, 28.0, 12.0, 30.0, 3.0, 3.0, 800, 3.0, 70, 6, 560),
    f("Bistek tagalog", "1 serving", 180, 300, 28.0, 8.0, 17.0, 1.0, 4.0, 900, 3.0, 30, 12, 480),
    f("Lechon kawali", "100 g", 100, 480, 25.0, 0.0, 41.0, 0.0, 0.0, 500, 1.0, 15, 0, 300),
    f("Longganisa", "1 piece", 50, 170, 8.0, 6.0, 13.0, 0.0, 5.0, 450, 0.8, 10, 0, 150),
    f("Tocino", "2 pieces", 80, 220, 13.0, 14.0, 13.0, 0.0, 12.0, 700, 1.0, 10, 0, 220),
    f("Beef tapa", "80 g", 80, 210, 22.0, 6.0, 11.0, 0.0, 5.0, 650, 2.0, 10, 0, 300),
    f("Hotdog", "1 piece", 50, 140, 6.0, 3.0, 11.0, 0.0, 2.0, 450, 0.5, 15, 0, 100),
    f("Corned beef, canned", "100 g", 100, 250, 17.0, 2.0, 19.0, 0.0, 1.0, 900, 2.0, 15, 0, 120),
    f("Lumpiang shanghai", "3 pieces", 60, 190, 7.0, 15.0, 11.0, 1.0, 1.0, 330, 1.0, 15, 2, 120),
    f("Fried egg", "1 large", 46, 90, 6.0, 0.4, 7.0, 0.0, 0.2, 95, 0.9, 27, 0, 70),
    f("Boiled egg", "1 large", 50, 78, 6.3, 0.6, 5.3, 0.0, 0.6, 62, 0.9, 25, 0, 63),
    f("Grilled chicken breast", "100 g", 100, 165, 31.0, 0.0, 3.6, 0.0, 0.0, 74, 0.4, 15, 0, 256),
    f("Roasted chicken thigh, skinless", "100 g", 100, 209, 26.0, 0.0, 11.0, 0.0, 0.0, 95, 1.1, 12, 0, 240),
    f("Fried chicken", "1 piece", 100, 280, 20.0, 9.0, 18.0, 0.3, 0.1, 500, 1.1, 15, 0, 230),
    f("Grilled bangus (milkfish)", "100 g", 100, 190, 22.0, 0.0, 11.0, 0.0, 0.0, 80, 0.5, 40, 0, 300),
    f("Fried tilapia", "100 g", 100, 180, 26.0, 0.0, 8.0, 0.0, 0.0, 70, 0.7, 14, 0, 380),
    f("Fried galunggong", "100 g", 100, 200, 23.0, 0.0, 11.0, 0.0, 0.0, 100, 1.5, 70, 0, 300),
    f("Tuyo (dried fish)", "30 g", 30, 80, 14.0, 0.0, 2.0, 0.0, 0.0, 900, 1.0, 100, 0, 200),
    f("Tinapa (smoked fish)", "100 g", 100, 190, 22.0, 0.0, 11.0, 0.0, 0.0, 700, 1.0, 80, 0, 340),
    f("Tuna in oil, canned", "1 can, drained", 85, 190, 22.0, 0.0, 11.0, 0.0, 0.0, 280, 1.0, 10, 0, 200),
    f("Sardines in tomato sauce", "1 can", 100, 150, 14.0, 4.0, 8.0, 0.0, 2.0, 600, 2.5, 300, 1, 280),
    f("Fried tokwa (tofu)", "100 g", 100, 220, 16.0, 4.0, 16.0, 1.0, 0.5, 15, 3.0, 350, 0, 200),
    // Vegetables and stews
    f("Kangkong, sauteed", "1 cup", 100, 60, 3.0, 5.0, 3.5, 2.5, 1.0, 220, 2.0, 90, 30, 300),
    f("Pechay, cooked", "1 cup", 170, 20, 2.7, 3.0, 0.3, 2.0, 2.0, 60, 1.8, 160, 45, 630),
    f("Malunggay leaves", "1 cup", 21, 14, 2.0, 2.0, 0.3, 0.8, 0.0, 9, 0.8, 40, 14, 130),
    f("Ampalaya with egg", "1 cup", 150, 130, 8.0, 9.0, 7.0, 3.0, 4.0, 300, 1.5, 40, 80, 450),
    f("Sitaw (long beans), cooked", "1 cup", 100, 40, 3.0, 7.0, 0.3, 3.0, 2.0, 5, 1.0, 70, 18, 280),
    f("Ginisang monggo", "1 cup", 250, 270, 14.0, 38.0, 7.0, 12.0, 2.0, 650, 3.0, 70, 8, 900),
    f("Pinakbet", "1 cup", 200, 130, 6.0, 14.0, 6.0, 5.0, 6.0, 500, 1.5, 90, 20, 600),
    f("Chopsuey", "1 cup", 200, 150, 9.0, 14.0, 6.0, 4.0, 5.0, 600, 1.5, 60, 40, 450),
    f("Kamote (sweet potato), boiled", "1 medium", 150, 135, 2.4, 31.0, 0.2, 4.5, 6.3, 60, 0.9, 60, 24, 440),
    f("Potato, boiled", "1 medium", 170, 150, 3.5, 34.0, 0.2, 3.0, 1.6, 10, 1.1, 20, 18, 620),
    // Fruit
    f("Banana (lakatan)", "1 medium", 118, 105, 1.3, 27.0, 0.4, 3.1, 14.0, 1, 0.3, 6, 10, 420),
    f("Banana (saba), boiled", "1 piece", 100, 110, 1.0, 28.0, 0.2, 2.0, 8.0, 2, 0.6, 5, 12, 400),
    f("Mango, ripe", "1 cup", 165, 100, 1.4, 25.0, 0.6, 2.6, 23.0, 2, 0.3, 18, 60, 277),
    f("Papaya", "1 cup", 140, 60, 0.7, 15.0, 0.4, 2.5, 11.0, 12, 0.3, 30, 90, 260),
    f("Pineapple", "1 cup", 165, 82, 0.9, 22.0, 0.2, 2.3, 16.0, 2, 0.5, 21, 79, 180),
    f("Apple", "1 medium", 182, 95, 0.5, 25.0, 0.3, 4.4, 19.0, 2, 0.2, 11, 8, 195),
    f("Watermelon", "1 cup", 150, 45, 0.9, 11.0, 0.2, 0.6, 9.0, 2, 0.4, 11, 12, 170),
    f("Avocado", "half", 100, 160, 2.0, 9.0, 15.0, 7.0, 0.7, 7, 0.6, 12, 10, 485),
    // Dairy, snacks and basics
    f("Whole milk", "1 cup", 244, 150, 8.0, 12.0, 8.0, 0.0, 12.0, 105, 0.1, 300, 0, 320),
    f("Plain yogurt", "1 cup", 245, 150, 8.5, 11.4, 8.0, 0.0, 11.4, 113, 0.1, 296, 1, 380),
    f("Processed cheese slice", "1 slice", 20, 70, 4.0, 1.0, 5.5, 0.0, 0.5, 280, 0.0, 100, 0, 30),
    f("Peanut butter", "1 tbsp", 16, 95, 4.0, 3.5, 8.0, 1.0, 1.5, 70, 0.3, 7, 0, 100),
    f("Roasted peanuts", "30 g", 30, 170, 7.5, 5.0, 14.0, 2.5, 1.0, 5, 0.6, 20, 0, 190),
    f("Whey protein", "1 scoop", 30, 120, 24.0, 3.0, 1.5, 0.0, 2.0, 150, 0.5, 130, 0, 150),
    f("Cooking oil", "1 tbsp", 14, 120, 0.0, 0.0, 14.0, 0.0, 0.0, 0, 0.0, 0, 0, 0),
    f("Sugar", "1 tsp", 4, 16, 0.0, 4.0, 0.0, 0.0, 4.0, 0, 0.0, 0, 0, 0),
    f("Coffee, 3-in-1 sachet", "1 sachet", 20, 80, 0.5, 15.0, 2.0, 0.0, 12.0, 40, 0.1, 10, 0, 70),
    f("Soft drink", "1 can", 330, 140, 0.0, 35.0, 0.0, 0.0, 35.0, 15, 0.0, 5, 0, 0),
    f("Taho", "1 cup", 250, 180, 6.0, 31.0, 3.0, 1.0, 25.0, 20, 1.5, 100, 0, 150),
    f("Halo-halo", "1 glass", 350, 340, 6.0, 65.0, 8.0, 3.0, 45.0, 90, 1.0, 150, 2, 350),
    f("Turon", "1 piece", 80, 220, 2.0, 36.0, 8.0, 2.0, 20.0, 10, 0.6, 12, 4, 200),
)

/** Bump this when foods are added to the lists, so phones that already have the old list get the new ones too. */
const val BUILTIN_VERSION = 3

val BuiltInFoods: List<Food> = FilipinoFoods + WorldFoods + FastFoodPH
