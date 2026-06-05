package com.restorant.order.repository;
import com.restorant.order.model.Food;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodRepository extends JpaRepository<Food, Long> { }