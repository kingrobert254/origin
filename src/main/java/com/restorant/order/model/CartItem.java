package com.restorant.order.model;

public class CartItem {
    private Long foodId;
    private String foodName;
    private double price;
    private int quantity;
    private double subTotal;

    public CartItem(Long foodId, String foodName, double price, int quantity) {
        this.foodId = foodId;
        this.foodName = foodName;
        this.price = price;
        this.quantity = quantity;
        this.subTotal = price * quantity;
    }

    // Getter dan Setter
    public Long getFoodId() { return foodId; }
    public void setFoodId(Long foodId) { this.foodId = foodId; }
    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; return; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; this.subTotal = this.price * quantity; }
    public double getSubTotal() { return subTotal; }
}