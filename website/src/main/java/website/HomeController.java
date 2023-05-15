package website;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("message", "Hello, Thymeleaf!");
        return "home";
    }

    @PostMapping("/submit")
    public String submit(@RequestParam String myInput, Model model) {
        model.addAttribute("message", "You entered: " + myInput);
        return "home";
    }
}
