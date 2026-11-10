package pe.edu.upeu.fia.infra;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class PortalController {
 @GetMapping({"/", "/index.html"})
 String home(){return "redirect:/portal/";}
 @GetMapping({"/portal", "/portal/"})
 String angular(){return "forward:/portal/index.html";}
}

