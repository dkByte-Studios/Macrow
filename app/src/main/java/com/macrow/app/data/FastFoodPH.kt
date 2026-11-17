package com.macrow.app.data

/**
 * Popular fast food and street food in the Philippines. The chains do not all publish full nutrition tables, so these
 * are careful estimates for a typical serving, rounded: check the box or the chain's website when you need precision,
 * and edit any food to match. Brand names belong to their owners and are used here only to help you find the food.
 */
val FastFoodPH: List<Food> = listOf(
    // Jollibee
    f("Jollibee Chickenjoy, 1 piece", "1 piece", 120, 290, 20.0, 11.0, 18.0, 0.5, 0.5, 650, 1.0, 20, 0, 230),
    f("Jollibee Chickenjoy with rice", "1 piece + rice", 280, 490, 24.0, 56.0, 18.0, 1.0, 0.5, 700, 1.4, 35, 0, 280),
    f("Jollibee Jolly Spaghetti", "1 regular", 250, 420, 14.0, 60.0, 14.0, 3.0, 15.0, 800, 2.5, 60, 6, 400),
    f("Jollibee Yumburger", "1 burger", 100, 250, 11.0, 28.0, 10.0, 1.5, 6.0, 480, 2.0, 60, 1, 180),
    f("Jollibee Cheesy Yumburger", "1 burger", 115, 300, 13.0, 28.0, 15.0, 1.5, 6.0, 650, 2.0, 120, 1, 190),
    f("Jollibee Burger Steak with rice", "1 piece + rice", 290, 470, 16.0, 58.0, 20.0, 2.0, 4.0, 900, 2.5, 40, 2, 330),
    f("Jollibee Jolly Hotdog", "1 hotdog", 110, 300, 12.0, 26.0, 16.0, 1.0, 5.0, 790, 1.5, 40, 0, 180),
    f("Jollibee Palabok Fiesta", "1 regular", 250, 380, 15.0, 52.0, 12.0, 2.5, 3.0, 900, 2.5, 80, 3, 300),
    f("Jollibee Peach Mango Pie", "1 pie", 80, 230, 2.0, 30.0, 11.0, 1.0, 14.0, 130, 0.8, 10, 2, 90),
    f("Jollibee Crispy Fries, regular", "1 regular", 100, 300, 4.0, 38.0, 15.0, 3.5, 0.5, 280, 1.0, 15, 8, 600),
    f("Jollibee Iced Tea, medium", "1 medium", 360, 150, 0.0, 38.0, 0.0, 0.0, 36.0, 15, 0.0, 5, 0, 30),
    // McDonald's
    f("McDonald's Big Mac", "1 burger", 215, 550, 25.0, 46.0, 30.0, 3.0, 9.0, 1010, 4.5, 250, 1, 400),
    f("McDonald's McChicken Sandwich", "1 sandwich", 150, 400, 14.0, 40.0, 21.0, 2.0, 5.0, 560, 2.5, 100, 1, 250),
    f("McDonald's Cheeseburger", "1 burger", 120, 300, 15.0, 33.0, 12.0, 2.0, 7.0, 720, 3.0, 180, 1, 230),
    f("McDonald's Chicken McDo, 1 piece", "1 piece", 120, 300, 18.0, 12.0, 19.0, 0.5, 0.5, 650, 1.0, 20, 0, 230),
    f("McDonald's Chicken McDo with rice", "1 piece + rice", 280, 490, 22.0, 56.0, 19.0, 1.0, 0.5, 700, 1.4, 35, 0, 290),
    f("McDonald's McSpaghetti", "1 regular", 240, 440, 14.0, 62.0, 15.0, 3.0, 12.0, 780, 2.5, 60, 5, 380),
    f("McDonald's Fries, medium", "1 medium", 115, 340, 4.0, 44.0, 16.0, 4.0, 0.5, 260, 1.0, 15, 10, 700),
    f("McDonald's McFlurry Oreo", "1 regular", 285, 340, 8.0, 52.0, 12.0, 1.0, 44.0, 180, 1.0, 250, 1, 380),
    f("McDonald's Baked Apple Pie", "1 pie", 80, 240, 2.0, 33.0, 11.0, 2.0, 13.0, 170, 0.8, 10, 1, 90),
    // KFC
    f("KFC Fried Chicken, 1 piece", "1 piece", 110, 290, 19.0, 9.0, 20.0, 0.3, 0.1, 750, 1.0, 20, 0, 220),
    f("KFC Zinger Sandwich", "1 sandwich", 210, 500, 24.0, 45.0, 25.0, 2.5, 6.0, 1100, 2.5, 100, 2, 350),
    f("KFC Mashed Potato with gravy", "1 regular", 130, 130, 2.0, 18.0, 6.0, 1.5, 0.5, 480, 0.4, 20, 4, 280),
    f("KFC Coleslaw", "1 regular", 100, 150, 1.0, 14.0, 10.0, 2.0, 10.0, 200, 0.4, 30, 18, 180),
    // Chowking
    f("Chowking Chicken, 1 piece", "1 piece", 110, 300, 22.0, 8.0, 20.0, 0.3, 0.5, 600, 1.0, 20, 0, 230),
    f("Chowking Chao Fan", "1 regular", 300, 480, 11.0, 70.0, 17.0, 2.0, 3.0, 900, 2.0, 40, 3, 250),
    f("Chowking Beef Wanton Mami", "1 regular", 450, 380, 18.0, 52.0, 11.0, 2.0, 3.0, 1600, 3.0, 50, 4, 350),
    f("Chowking Pork Siopao", "1 piece", 120, 270, 9.0, 40.0, 8.0, 1.5, 9.0, 450, 1.5, 40, 0, 130),
    f("Chowking Lumpiang Shanghai, 4 pieces", "4 pieces", 80, 260, 8.0, 22.0, 15.0, 1.0, 1.0, 380, 1.0, 15, 2, 140),
    f("Chowking Siomai, 4 pieces", "4 pieces", 100, 200, 12.0, 15.0, 10.0, 1.0, 1.0, 480, 1.0, 20, 0, 160),
    f("Chowking Halo-Halo", "1 regular", 400, 430, 6.0, 75.0, 12.0, 3.0, 55.0, 90, 1.2, 150, 2, 350),
    // Mang Inasal and Max's
    f("Mang Inasal Chicken Inasal, paa (leg)", "1 piece", 160, 250, 26.0, 3.0, 15.0, 0.0, 1.0, 650, 1.5, 15, 1, 280),
    f("Mang Inasal Chicken Inasal, pecho (breast)", "1 piece", 190, 280, 30.0, 3.0, 16.0, 0.0, 1.0, 700, 1.2, 15, 1, 330),
    f("Max's Fried Chicken, 1/4 chicken", "1 serving", 220, 600, 52.0, 4.0, 41.0, 0.3, 0.1, 1000, 2.0, 25, 0, 480),
    // Pizza and pasta chains
    f("Greenwich Hawaiian Pizza, 1 slice", "1 slice", 110, 260, 11.0, 32.0, 9.0, 1.5, 6.0, 560, 2.0, 140, 1, 160),
    f("Greenwich Lasagna Supreme", "1 regular", 280, 450, 22.0, 42.0, 22.0, 2.5, 8.0, 950, 2.5, 300, 4, 520),
    f("Shakey's Manager's Choice Pizza, 1 slice", "1 slice", 90, 220, 10.0, 24.0, 9.0, 1.2, 3.0, 480, 1.8, 120, 1, 140),
    f("Pizza Hut Pepperoni Pan Pizza, 1 slice", "1 slice", 120, 300, 13.0, 30.0, 14.0, 1.5, 3.0, 750, 2.2, 150, 1, 170),
    // Burger King, coffee and bakeries
    f("Burger King Whopper", "1 burger", 290, 650, 28.0, 49.0, 37.0, 3.0, 11.0, 980, 4.5, 90, 6, 560),
    f("Burger King Whopper Jr", "1 burger", 150, 340, 15.0, 31.0, 17.0, 2.0, 6.0, 500, 2.5, 60, 3, 280),
    f("Starbucks Caffe Latte, tall", "1 tall", 355, 190, 13.0, 19.0, 7.0, 0.0, 17.0, 160, 0.1, 450, 0, 600),
    f("Starbucks Caramel Frappuccino, tall", "1 tall", 355, 300, 4.0, 56.0, 7.0, 0.0, 53.0, 190, 0.2, 130, 0, 200),
    f("Mister Donut Honey Glazed Donut", "1 donut", 60, 240, 3.0, 32.0, 11.0, 1.0, 14.0, 250, 1.4, 20, 0, 70),
    f("Mister Donut Pon de Ring", "1 donut", 55, 230, 3.0, 34.0, 9.0, 1.0, 13.0, 220, 1.0, 15, 0, 60),
    f("Goldilocks Ensaymada", "1 piece", 70, 250, 5.0, 31.0, 12.0, 1.0, 10.0, 220, 1.4, 25, 0, 80),
    f("Goldilocks Polvoron", "1 piece", 20, 100, 1.5, 11.0, 5.5, 0.3, 6.0, 20, 0.3, 15, 0, 40),
    f("Potato Corner Fries, medium", "1 medium", 110, 380, 4.0, 46.0, 20.0, 4.0, 1.0, 600, 1.0, 15, 8, 650),
    // Silog meals and carinderia favorites
    f("Tapsilog (tapa, garlic rice, egg)", "1 plate", 380, 690, 35.0, 70.0, 30.0, 1.5, 6.0, 1200, 4.0, 60, 0, 520),
    f("Longsilog (longganisa, garlic rice, egg)", "1 plate", 340, 680, 24.0, 72.0, 33.0, 1.5, 8.0, 1100, 3.0, 50, 0, 400),
    f("Tocilog (tocino, garlic rice, egg)", "1 plate", 340, 680, 22.0, 85.0, 26.0, 1.5, 22.0, 1050, 3.0, 50, 0, 380),
    f("Hotsilog (hotdog, garlic rice, egg)", "1 plate", 330, 650, 22.0, 68.0, 32.0, 1.5, 6.0, 1300, 3.0, 60, 0, 330),
    // Street food
    f("Kwek-kwek, 5 pieces", "5 pieces", 120, 320, 14.0, 24.0, 18.0, 1.0, 2.0, 420, 1.8, 40, 0, 150),
    f("Fish balls, 5 pieces", "5 pieces", 100, 120, 6.0, 12.0, 5.0, 0.5, 1.0, 380, 0.5, 20, 0, 80),
    f("Kikiam, 3 pieces", "3 pieces", 90, 180, 8.0, 14.0, 10.0, 0.5, 1.0, 420, 0.8, 20, 0, 90),
    f("Isaw, 2 sticks", "2 sticks", 80, 200, 11.0, 6.0, 14.0, 0.0, 2.0, 380, 1.5, 15, 0, 100),
    f("Banana cue, 1 stick", "1 stick", 80, 250, 1.0, 42.0, 9.0, 2.0, 28.0, 10, 0.5, 5, 6, 300),
    f("Balut", "1 egg", 100, 190, 14.0, 1.0, 14.0, 0.0, 0.5, 190, 3.0, 80, 0, 150),
)
