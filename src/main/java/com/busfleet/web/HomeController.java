package com.busfleet.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Главная страница приложения (корень URL).
 * Редирект на H2 Console, если она включена; иначе — простая страница.
 */
@Controller
public class HomeController {

    @Value("${spring.h2.console.enabled:true}")
    private boolean h2ConsoleEnabled;

    @GetMapping("/")
    public RedirectView home() {
        if (h2ConsoleEnabled) {
            return new RedirectView("/h2-console");
        }
        return new RedirectView("/h2-console"); // всё равно ведём на консоль при запросе корня
    }
}
