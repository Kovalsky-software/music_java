package org.example.vp_final;

// Используем Record для краткости
public record AfishaEvent(
        int afishaId,
        String title,
        String date,      // Для сортировки по дате (формат YYYY-MM-DD HH:MM:SS)
        String location
        // Если в вашей таблице Afisha есть другие поля, добавьте их сюда
) {}